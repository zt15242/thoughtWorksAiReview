package other.tw.business.account;

import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.trigger.DataResult;
import other.tw.business.Constants;
import other.tw.business.contract.ContractSelector;
import other.tw.business.service.ErpService;
import other.tw.business.util.CollectionUtils;
import other.tw.business.util.NeoCrmUtils;
import other.tw.business.opportunity.OpportunitySelector;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class AccountService {
    public static final Logger LOGGER = LoggerFactory.getLogger();

    private final AccountSelector accountSelector = new AccountSelector();
    private final OpportunitySelector opportunitySelector = new OpportunitySelector();
    private final ContractSelector contractSelector = new ContractSelector();

//    public void setRiskLevel(List<Account> newList) {
//        for (Account newAcc : newList) {
//            Integer legalRisk = newAcc.getLegal_RiskLevel__c();
//            Integer financeRisk = newAcc.getFinance_RiskLevel__c();
//            if (legalRisk == null) {
//                newAcc.setRiskLevel__c(financeRisk);
//            } else if (financeRisk == null) {
//                newAcc.setRiskLevel__c(legalRisk);
//            } else {
//                newAcc.setRiskLevel__c(legalRisk > financeRisk ? legalRisk : financeRisk);
//            }
//        }
//    }

    //业务客户添加parent时，parent如果有商机，则提示错误。
    public void checkClientAccountParent(List<Account> newList, Map<Account, DataResult> resultMap) throws ApiEntityServiceException {
        Map<Account, Long> accountToParentIdMap = new HashMap<>();

        for (Account newAcc : newList) {
            if (newAcc.getEntityType() == -11010000100001L && newAcc.getParentAccountId() != null) {
                accountToParentIdMap.put(newAcc, newAcc.getParentAccountId());
            }
        }

        Map<Long, List<Opportunity>> accOppsMap = opportunitySelector.getOppByAccId(new ArrayList<>(accountToParentIdMap.values()));

        for (Account newAcc : newList) {
            if (accOppsMap.containsKey(newAcc.getParentAccountId()) && !accOppsMap.get(newAcc.getParentAccountId()).isEmpty()) {
                DataResult dataResult = resultMap.get(newAcc);
                dataResult.setSuccess(false);
                dataResult.setMsg(dataResult.getMsg() + " error: the parent account has opportunities.");
            }

        }
    }


    public void sendDataToFocusAccountProcessQueue(Map<Long, Account> oldAccount, Map<Long, Account> newAccount) throws ApiEntityServiceException {
        List<Account> accountList = generateNeedUpdateAccountListWhenFocusOrPeriodChangeOfParentAccount(oldAccount, newAccount);
        if (accountList.isEmpty()) {
            return;
        }
        NeoCrmUtils.update(accountList, "客户");
    }

    public List<Account> generateNeedUpdateAccountListWhenFocusOrPeriodChangeOfParentAccount(Map<Long, Account> oldMap, Map<Long, Account> newMap) throws ApiEntityServiceException {
        Map<Long, List<Account>> childMap = accountSelector.getChildAccountMap(new ArrayList<>(newMap.keySet()));

        List<Account> updateList = new ArrayList<>();

        for (Long id : newMap.keySet()) {
            Account newAcc = newMap.get(id);
            if (oldMap.containsKey(id)) {
                Account oldAcc = oldMap.get(id);

                if (oldAcc.getIs_Parent__c() && childMap.containsKey(id)) {
                    List<Account> childList = childMap.get(id);
                    if (oldAcc.getCurrently_is_a_focus_account__c() != newAcc.getCurrently_is_a_focus_account__c()) {
                        for (Account childAcc : childList) {
                            childAcc.setCurrently_is_a_focus_account__c(newAcc.getCurrently_is_a_focus_account__c());
                            updateList.add(childAcc);
                        }
                    }
                }
            }

        }

        return updateList;
    }


    public void syncAccountFocusAndPeriodWhenUpdate(Map<Long, Account> oldMap, Map<Long, Account> newMap) throws ApiEntityServiceException {

        List<Long> parentAccountIdList = new ArrayList<>();
        for (Account account : oldMap.values()) {
            if (account.getParentAccountId() != null) {
                parentAccountIdList.add(account.getParentAccountId());
            }
        }
        for (Account account : newMap.values()) {
            if (account.getParentAccountId() != null) {
                parentAccountIdList.add(account.getParentAccountId());
            }
        }

        Map<Long, Account> parentMap = new AccountSelector().getAccountMapById(parentAccountIdList);

        for (Long id : newMap.keySet()) {
            Account newAcc = newMap.get(id);
            if (oldMap.containsKey(id)) {
                Account oldAcc = oldMap.get(id);

                if (!oldAcc.getIs_Parent__c()) {
                    if (oldAcc.getParentAccountId() == null && newAcc.getParentAccountId() != null) {
                        // when add account to parent account under
                        Account parentAcc = parentMap.get(newAcc.getParentAccountId());
                        if (oldAcc.getCurrently_is_a_focus_account__c() || parentAcc.getCurrently_is_a_focus_account__c()) {
                            newAcc.setCurrently_is_a_focus_account__c(parentAcc.getCurrently_is_a_focus_account__c());
                        }
                    } else if (oldAcc.getParentAccountId() != null && newAcc.getParentAccountId() == null) {
                        // when remove child account from parent account
                        Account parentAcc = parentMap.get(oldAcc.getParentAccountId());
                        if (parentAcc.getCurrently_is_a_focus_account__c()) {
                            newAcc.setCurrently_is_a_focus_account__c(false);
                        }
                    }
                }
            }
        }
    }

    //after update contract
    public void updateAccountLevelTCV(Map<Long, Contract> newMap, Map<Long, Contract> oldMap) throws ApiEntityServiceException {
        Set<Long> accountIdSetToUpdate = getAccountNeedToUpdateTCV(newMap, oldMap);
        if (accountIdSetToUpdate.isEmpty()) {
            return;
        }
        List<Account> accountsToUpdate = getAccountsWithTCVUpdated(accountIdSetToUpdate);

        NeoCrmUtils.update(accountsToUpdate, "after update contract, updateAccountLevelTCV");
    }

    //after insert contract
    public void updateAccountLevelTCV(List<Contract> newList) throws ApiEntityServiceException {
        Set<Long> accountIdSetToUpdate = getAccountNeedToUpdateTCV(newList);
        if (accountIdSetToUpdate.isEmpty()) {
            return;
        }
        List<Account> accountsToUpdate = getAccountsWithTCVUpdated(accountIdSetToUpdate);

        NeoCrmUtils.update(accountsToUpdate, "after insert contract, updateAccountLevelTCV");
    }

    public List<Account> getAccountsWithTCVUpdated(Set<Long> accountIdSetToUpdate) throws ApiEntityServiceException {
        List<Contract> allContracts = contractSelector.getAccountTCVAmountByAccountIds(accountIdSetToUpdate);
        Map<Long, List<Contract>> contractListMapByAccountId = CollectionUtils.groupByIdField(allContracts, "accountId");
        Set<Long> keySet = contractListMapByAccountId.keySet();

        Set<Long> accountIdsNotExistContractObj = new HashSet<>(accountIdSetToUpdate);
        accountIdsNotExistContractObj.removeAll(keySet);

        Set<Long> accountIdsExistContractObj = new HashSet<>(accountIdSetToUpdate);
        accountIdsExistContractObj.retainAll(keySet);

        List<Account> accountsToUpdate = new ArrayList<>();
        for (Long accountId : accountIdsExistContractObj) {
            List<Contract> contracts = contractListMapByAccountId.get(accountId);
            double grossTCV = calculateAccountGrossTCV(contracts);
            double netTCV = calculateAccountNetTCV(contracts);
            Account acc = new Account();
            acc.setId(accountId);
            acc.setTCV_Amount_in_USD__c(grossTCV);
            acc.setNet_TCV_in_USD__c(netTCV);
            accountsToUpdate.add(acc);
        }

        for (Long accountId : accountIdsNotExistContractObj) {
            Account acc = new Account();
            acc.setId(accountId);
            acc.setTCV_Amount_in_USD__c((double) 0);
            acc.setNet_TCV_in_USD__c((double) 0);

            accountsToUpdate.add(acc);
        }
        return accountsToUpdate;
    }

    private Set<Long> getAccountNeedToUpdateTCV(Map<Long, Contract> newMap, Map<Long, Contract> oldMap) {
        Set<Long> accountIdSetToUpdate = new HashSet<>();
        for (Long contractId : newMap.keySet()) {
            Contract newContract = newMap.get(contractId);
            Contract oldContract = oldMap.get(contractId);

            if (relevantFieldsChanged(newContract, oldContract)) {
                if (newContract.getAccountId() == null) {
                    continue;
                }
                accountIdSetToUpdate.add(newContract.getAccountId());
            }
        }
        return accountIdSetToUpdate;
    }

    private Set<Long> getAccountNeedToUpdateTCV(List<Contract> newList) {
        Set<Long> accountIdSetToUpdate = new HashSet<>();
        for (Contract newContract : newList) {
            if (newContract.getAccountId() == null) {
                continue;
            }
            accountIdSetToUpdate.add(newContract.getAccountId());
        }
        return accountIdSetToUpdate;
    }

    private Boolean relevantFieldsChanged(Contract newContract, Contract oldContract) {
        List<String> relevantFields = new ArrayList<>(Arrays.asList("should_include_in_tcv_reporting__c", "net_TCV_New__c"));
        for (String field : relevantFields) {
            if (newContract.getAttribute(field) != oldContract.getAttribute(field)) {
                return true;
            }
        }
        return false;
    }

    private double calculateAccountGrossTCV(List<Contract> contracts) {
        double accountTCVAmount = 0;
        for (Contract contract : contracts) {
            accountTCVAmount += contract.getTCV_Amount_in_USD__c();
        }
        BigDecimal b = new BigDecimal(accountTCVAmount);
        return b.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double calculateAccountNetTCV(List<Contract> contracts) {
        double accountTCVAmount = 0;
        for (Contract contract : contracts) {
            accountTCVAmount += contract.getNet_TCV_in_USD__c();
        }
        BigDecimal b = new BigDecimal(accountTCVAmount);
        return b.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }


    public void resetParentIsParent(Map<Long, Account> oldMap, Map<Long, Account> newMap) throws ApiEntityServiceException {
        List<Account> resetParentList = new ArrayList<>();

        for (Long accId : newMap.keySet()) {
            Account oldAcc = oldMap.get(accId);
            Account newAcc = newMap.get(accId);

            if (!Objects.equals(oldAcc.getParentAccountId(), newAcc.getParentAccountId())) {
                if (oldAcc.getParentAccountId() != null) {
                    resetParentList.add(oldAcc);
                }
                if (newAcc.getParentAccountId() != null) {
                    resetParentList.add(newAcc);
                }
            }
        }

        resetParentIsParent(resetParentList);
    }

    public void resetParentIsParent(List<Account> accountList) throws ApiEntityServiceException {

        Set<Long> parentAccountIdSet = new HashSet<>();

        for (Account account : accountList) {
            if (account.getParentAccountId() != null) {
                parentAccountIdSet.add(account.getParentAccountId());
            }
        }

        Map<Long, List<Account>> childMap = accountSelector.getChildAccountMap(new ArrayList<>(parentAccountIdSet));

        List<Account> updateList = new ArrayList<>();

        for (Long parentId : parentAccountIdSet) {
            boolean isParent = childMap.containsKey(parentId) && !childMap.get(parentId).isEmpty();
            Account acc = new Account();
            acc.setId(parentId);
            acc.setIs_Parent__c(isParent);
            updateList.add(acc);
        }

        NeoCrmUtils.update(updateList, "resetParentIsParent");
    }

    public void sendToErp(Map<Long, Account> oldMap, Map<Long, Account> newMap) throws ApiEntityServiceException {
        List<String> ids = new ArrayList<>();

        for (Long accId : newMap.keySet()) {
            Account oldAcc = oldMap.get(accId);
            Account newAcc = newMap.get(accId);

            if (Constants.ACCOUNT_APPROVAL_STATUS.equals(newAcc.getApproval_Status__c())
                    && (!Objects.equals(oldAcc.getSettlementType__c(), newAcc.getSettlementType__c())
                    || !Objects.equals(oldAcc.getReceivingCondition__c(), newAcc.getReceivingCondition__c())
                    || !Objects.equals(oldAcc.getInvoiceType__c(), newAcc.getInvoiceType__c()))) {
                ids.add(accId.toString());
            }
        }

        if (!ids.isEmpty()) {
            new ErpService().sendAccountToErpById2(ids);
        }
    }
}