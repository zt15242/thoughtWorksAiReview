package other.tw.business.opportunity;

import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.trigger.DataResult;
import other.tw.business.Constants;
import other.tw.business.opplinksolution.OppLinkSolutionSelector;
import other.tw.business.service.ErpService;
import other.tw.business.util.CollectionUtils;
import other.tw.business.util.NeoCrmUtils;
import other.tw.business.account.AccountService;
import other.tw.business.contract.ContractSelector;
import other.tw.business.contractopportunityallocation.ContractOppSelector;

import java.util.*;

public class OpportunityService {
    public static final Logger LOGGER = LoggerFactory.getLogger();
    public static final Integer FIXED_BID_CONTRACT = 1;
    public static final Integer PRO_BONO_CONTRACT = 2;
    public static final Integer TIME_MATERIALS_CONTRACT = 3;
    private final AccountService accountService = new AccountService();
    private final ContractSelector contractSelector = new ContractSelector();
    private final ContractOppSelector contractOpportunityAllocationSelector = new ContractOppSelector();
    private final OppLinkSolutionSelector oppLinkSolutionSelector = new OppLinkSolutionSelector();

    public void setOpportunityDefaultValue(List<Opportunity> oppList) {
        for (Opportunity opp : oppList) {

            if (PRO_BONO_CONTRACT.equals(opp.getContract_Type__c()) ||
                    TIME_MATERIALS_CONTRACT.equals(opp.getContract_Type__c() ) ||
                    (FIXED_BID_CONTRACT.equals(opp.getContract_Type__c()) && !opp.getIs_MISC_Opportunity__c()) ||
                    opp.getContract_Type__c() == null
            ) {
                opp.setAmount__c(opp.getOpportunity_Contract_Value__c());
            }
            if (opp.getAmount__c() == null) {
                opp.setAmount__c((double) 0);
            }
        }
    }

    //商机won时，更新客户的对应字段
    public void checkStatus(List<Opportunity> newList) throws ApiEntityServiceException {
        Map<Long,Long> wonOppToAcc = new HashMap<>();

        for(Opportunity opp : newList){
            if(opp.getStatus()==2){
                wonOppToAcc.put(opp.getId(),opp.getAccountId());
            }
        }

        if(wonOppToAcc.isEmpty()){
            return;
        }

        Map<Long,Long> oppIdToContractDateMap = contractSelector.getAccountContractDateByOppId(new ArrayList<>(wonOppToAcc.keySet()));

        List<Account> accUpdateList = new ArrayList<>();

        for(Long oppId :oppIdToContractDateMap.keySet()){
            Account account = new Account();
            account.setId(wonOppToAcc.get(oppId));
            account.setLastContactDate__c(oppIdToContractDateMap.get(oppId));

            accUpdateList.add(account);
        }

        NeoCrmUtils.update(accUpdateList,"客户最新合同日");
    }

    public void checkStageChangedBeforeUpdate(Map<Long, Opportunity> oldMap, Map<Long, Opportunity> newMap,Map<Opportunity,DataResult> dataResultMap) throws ApiEntityServiceException {
        Map<Long,Opportunity> checkPercentageMap = new HashMap<>();
        List<Opportunity> needToReCalList = new ArrayList<>();

        for (Long oppId : newMap.keySet()) {
            Opportunity oldOpp = oldMap.get(oppId);
            Opportunity newOpp = newMap.get(oppId);
            if(!Objects.equals(oldOpp.getSaleStageId(), newOpp.getSaleStageId())) {

                if (Constants.OPPORTUNITY_SALESTAGE_CONTRACT_NEGOTIATION_IDSET.contains(newOpp.getSaleStageId())) {
                    checkPercentageMap.put(newOpp.getId(), newOpp);
                }

                if(Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(newOpp.getSaleStageId())){
                    needToReCalList.add(newOpp);
                }
            }
        }

        if(!checkPercentageMap.isEmpty()) {
            checkLinkSolutionPercentage(checkPercentageMap, dataResultMap);
        }

        if(!needToReCalList.isEmpty()) {
            reCalOpportunityContractValue(needToReCalList);
        }
    }

    public void checkLinkSolutionPercentage(Map<Long,Opportunity> checkMap, Map<Opportunity, DataResult> dataResultMap) throws ApiEntityServiceException {
         Map<Long,Double> oppToPercentageMap = oppLinkSolutionSelector.getOpportunityPercentageMap(checkMap.keySet());

         for (Long oppId : checkMap.keySet()) {
             Opportunity opp = checkMap.get(oppId);

             if(oppToPercentageMap.containsKey(oppId) && !oppToPercentageMap.get(oppId).equals(1.0)){
                 DataResult dataResult = dataResultMap.get(opp);
                 dataResult.setSuccess(false);
                 dataResult.setMsg(dataResult.getMsg()+" The total percentage of oppLinkSolution does not equal 100%.");
             }
         }
    }



    public void updateFieldBeforeUpdate(Map<Long, Opportunity> oldMap, List<Opportunity> newList) {
        for (Opportunity newOpp : newList) {
//            Opportunity oldOpp = oldMap != null ? oldMap.get(newOpp.getId()) : null;
            updateAmountWithOpportunityContractValue(newOpp);
        }
    }

    public void updateAmountWithOpportunityContractValue(Opportunity newOpp) {
        if (PRO_BONO_CONTRACT.equals(newOpp.getContract_Type__c()) ||
                (FIXED_BID_CONTRACT.equals(newOpp.getContract_Type__c() ) && !newOpp.getIs_MISC_Opportunity__c()) ||
                (TIME_MATERIALS_CONTRACT.equals(newOpp.getContract_Type__c()) && !newOpp.getHas_Proxies__c()) ||
                newOpp.getContract_Type__c() == null
        ) {
            newOpp.setAmount__c(newOpp.getOpportunity_Contract_Value__c() == null ? 0 : newOpp.getOpportunity_Contract_Value__c());
        }
    }

    public void reCalOpportunityContractValue(List<Opportunity> reCalList) throws ApiEntityServiceException {
        Set<Long> oppIdSet = new HashSet<>();

        for (Opportunity opp : reCalList) {
            if(Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(opp.getSaleStageId())) {
                oppIdSet.add(opp.getId());
            }
        }

        List<Contract_Opportunity_Allocation__c> allocationList = contractOpportunityAllocationSelector.getOpportunityContractValueFieldsByOpportunityId(oppIdSet);

        Map<Long,List<Contract_Opportunity_Allocation__c>> oppIdToContractMap = CollectionUtils.groupByIdField(allocationList,"opportunity__c");

        for(Opportunity opp:reCalList){
            Long oppId = opp.getId();
            double ocv = 0;

            if(oppIdToContractMap.containsKey(oppId)){
                List<Contract_Opportunity_Allocation__c> calList = oppIdToContractMap.get(oppId);
                for(Contract_Opportunity_Allocation__c allocation:calList){
                    double amount = allocation.getAllocated_Amount__c() == null ? 0 : allocation.getAllocated_Amount__c();
                    double discount = allocation.getEstimated_Discount__c() == null ? 0 : allocation.getEstimated_Discount__c();
                    double expense = allocation.getExpense__c() == null ? 0 : allocation.getExpense__c();

                    ocv += amount - discount - expense;
                }
            }

            opp.setMoney(ocv);
        }
    }


    public void reCalContractValueWhenCurrencyStageContractValueChanged(Map<Long, Opportunity> oldMap, List<Opportunity> newList) throws ApiEntityServiceException, NoSuchFieldException, IllegalAccessException {
        Map<Long, Opportunity> oppsNeedToCheck = getOppsNeedToReCalContractValueAndContractValueInUSD(newList, oldMap);
        if (oppsNeedToCheck.isEmpty()) {
            return;
        }

        List<Contract_Opportunity_Allocation__c> allocations = contractOpportunityAllocationSelector.getOpportunityAllocatedTCVAmountByOppIds(oppsNeedToCheck.keySet());

        List<Contract_Opportunity_Allocation__c> closedWonSameCurrencyAllocations = getClosedWonSameCurrencyAllocations(allocations, oppsNeedToCheck);

        Map<Long, Double> opportunityToContractValueMap = CollectionUtils.aggregateFieldByGroup(closedWonSameCurrencyAllocations, "opportunity__c", "net_TCV_New__c");

        updateOpportunitiesOCVAndGMD(new ArrayList<>(oppsNeedToCheck.values()), opportunityToContractValueMap);
    }



    public List<Contract_Opportunity_Allocation__c> getClosedWonSameCurrencyAllocations(List<Contract_Opportunity_Allocation__c> allocations, Map<Long, Opportunity> oppsNeedToCheck) {
        List<Contract_Opportunity_Allocation__c> matchingAllocations = new ArrayList<>();

        for (Contract_Opportunity_Allocation__c currentAllocation : allocations) {

            Opportunity relatedOpportunity = oppsNeedToCheck.get(currentAllocation.getOpportunity__c());
            boolean isCurrencyMatched = Objects.equals(currentAllocation.getCurrencyIsoCode__c(), relatedOpportunity.getCurrencyUnit());
            boolean isStageClosedWon = Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(relatedOpportunity.getSaleStageId());

            if (isCurrencyMatched && isStageClosedWon) {
                matchingAllocations.add(currentAllocation);
            }
        }

        return matchingAllocations;
    }


    public void updateOpportunitiesOCVAndGMD(List<Opportunity> newList, Map<Long, Double> oppoIdToContractValue) {
        String logContent = "";
        for (Opportunity opportunity : newList) {
            Double contractValue = oppoIdToContractValue.get(opportunity.getId());
            if (contractValue != null) {
                opportunity.setOpportunity_Contract_Value__c(contractValue);
                logContent += ">>> opportunityId:  " + opportunity.getId() + ",opportunityToContractValue updated with new contractValue;";
            }

        }

        if (!logContent.isEmpty()) {
            LOGGER.info("OCV final calculation detail through opp before update business: " + logContent);
        }
    }


    public Map<Long, Opportunity> getOppsNeedToReCalContractValueAndContractValueInUSD(List<Opportunity> newList, Map<Long, Opportunity> oldMap) {
        Map<Long, Opportunity> oppsNeedToCheck = new HashMap<>();
        String logContent = "";
        for (Opportunity newOpp : newList) {
            Opportunity oldOpp = oldMap.get(newOpp.getId());

            boolean contractValueChanged = !Objects.equals(oldOpp.getOpportunity_Contract_Value__c(), newOpp.getOpportunity_Contract_Value__c());
            boolean closedWonStageChanged =
                    !Objects.equals(oldOpp.getSaleStageId(), newOpp.getSaleStageId()) && (Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(oldOpp.getSaleStageId()) || Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(newOpp.getSaleStageId()));
            boolean currencyChanged = !Objects.equals(oldOpp.getCurrencyUnit(), newOpp.getCurrencyUnit());
            boolean monthlyExchangeRateChanged = !Objects.equals(oldOpp.getLatest_Monthly_Exchange_Rate__c(), newOpp.getLatest_Monthly_Exchange_Rate__c());

            if (contractValueChanged || closedWonStageChanged || currencyChanged || monthlyExchangeRateChanged) {
                String exchangeRateChangedLog = monthlyExchangeRateChanged
                        ? "exchange rate from old: " + oldOpp.getLatest_Monthly_Exchange_Rate__c() + " to new: " + newOpp.getLatest_Monthly_Exchange_Rate__c() + ";"
                        : "";
                logContent +=
                        ">>> opportunityId:  " +
                                newOpp.getId() +
                                ", contractValueChanged:  " +
                                contractValueChanged +
                                ", closedWonStageChanged:  " +
                                closedWonStageChanged +
                                ", currencyChanged:  " +
                                currencyChanged +
                                ", monthlyExchangeRateChanged:  " +
                                monthlyExchangeRateChanged +
                                ";" +
                                exchangeRateChangedLog;
                oppsNeedToCheck.put(newOpp.getId(), newOpp);
            }
        }
        if (!logContent.isEmpty()) {
            LOGGER.error("Opps need to change OCV in before update business: " + logContent);
        }
        return oppsNeedToCheck;
    }

    public void sendOpportunityToErp(List<Opportunity> oppList) throws ApiEntityServiceException {
        List<String> oppIds = new ArrayList<>();

        for (Opportunity opportunity : oppList) {
            oppIds.add(opportunity.getId().toString());
        }

        new ErpService().sendOpportunityToErpById2(oppIds);
    }

    public void checkStageChanged(Map<Long, Opportunity> oldMap, Map<Long, Opportunity> newMap) throws ApiEntityServiceException {
        List<String> oppIds = new ArrayList<>();

        for (Long oppId : newMap.keySet()) {
            Opportunity oldOpp = oldMap.get(oppId);
            Opportunity newOpp = newMap.get(oppId);
            if(!Objects.equals(oldOpp.getSaleStageId(), newOpp.getSaleStageId())) {
                oppIds.add(oppId.toString());

                if(Constants.OPPORTUNITY_SALESTAGE_CLOSED_WON_IDSET.contains(newOpp.getSaleStageId())){
                    newOpp.setComfirm_Revenue__c(1);
                }
            }
        }

        new ErpService().sendOpportunityToErpById2(oppIds);
    }

//    public List<JSONObject> handleCurrencyUnit(List<JSONObject> jsonObjectList) {
//        for(JSONObject jsonObject : jsonObjectList){
//            if(jsonObject.containsKey("currencyUnit") && !jsonObject.getJSONArray("currencyUnit").isEmpty()) {
//                String currencyUnit = jsonObject.getJSONArray("currencyUnit").getString(0);
//                Integer currencyUnit1 = null;
//                switch (currencyUnit) {
//                    case "人民币":
//                        currencyUnit1 = 1;
//                        break;
//                    case "港币":
//                        currencyUnit1 = 2;
//                        break;
//                    case "美元":
//                        currencyUnit1 = 3;
//                        break;
//                }
//                jsonObject.put("currencyUnit",currencyUnit1);
//            }
//        }
//
//        return jsonObjectList;
//    }

//    public void updateOpportunityLevelTCV(List<Contract_Opp_Alloc_Operation__e> allocationEventList) {
//        List<Opportunity> opportunitiesToUpdate = getUpdateOpportunitiesWithLatestExchangeRate(allocationEventList);
//        if (!opportunitiesToUpdate.isEmpty()) {
//            NeoCrmUtils.update(opportunitiesToUpdate,"商业机会");
//        }
//    }
//
//    public List<Opportunity> getUpdateOpportunitiesWithLatestExchangeRate(List<Contract_Opp_Alloc_Operation__e> allocationEventList) {
//        Set<Long> oppIdSetToUpdate = getOppNeedToUpdateTCV(allocationEventList);
//        if (oppIdSetToUpdate.isEmpty()) {
//            return new ArrayList<>();
//        }
//        List<Opportunity> opportunitiesToUpdate = getOpportunitiesWithTCVUpdated(oppIdSetToUpdate);
////        updateOpportunitiesWithLatestExchangeRate(opportunitiesToUpdate);
//        return opportunitiesToUpdate;
//    }
//
//    public List<Opportunity> getOpportunitiesWithTCVUpdated(Set<Long> oppIdSetToUpdate) throws NoSuchFieldException, IllegalAccessException {
//        List<Contract_Opportunity_Allocation__c> allAllocations = contractOpportunityAllocationSelector.getOpportunityAllocatedTCVAmountByOppIds(oppIdSetToUpdate);
//
//        Map<Long, List<Contract_Opportunity_Allocation__c>> allocationListMapByOppId = CollectionUtils.groupByIdField(allAllocations,"opportunity__c");
//        Set<Long> keySet = allocationListMapByOppId.keySet();
//
//        Set<Long> oppIdsNotExistAllocationObj = new HashSet<>();
//        oppIdsNotExistAllocationObj.removeAll(keySet);
//
//        Set<Long> oppIdsExistAllocationObj = new HashSet<>();
//        oppIdsExistAllocationObj.addAll(keySet);
//
//        List<Opportunity> opportunitiesToUpdate = new ArrayList<>();
//        String logContent = "";
//        for (Long opportunityId : oppIdsExistAllocationObj) {
//            List<Contract_Opportunity_Allocation__c> allocations = allocationListMapByOppId.get(opportunityId);
//            Double opportunityTCV = calculateOpportunityGrossTCV(allocations);
//            Double netTCV = calculateOpportunityNetTCV(allocations);
////            Opportunity opportunity = new Opportunity(Long = opportunityId, TCV_Amount_in_USD__c = opportunityTCV, Net_TCV_in_USD__c = netTCV);
//            Opportunity opportunity = new Opportunity();
//            opportunity.setId(opportunityId);
//            opportunity.setTCV_Amount_in_USD__c(opportunityTCV);
//            opportunity.setNet_TCV_in_USD__c(netTCV);
//
//            opportunitiesToUpdate.add(opportunity);
//        }
//        if (logContent != "") {
//            LOGGER.info("OCV calculation triggered by allocation event:" + logContent);
//        }
//
//        for (Long oppId : oppIdsNotExistAllocationObj) {
//            Opportunity opportunity = new Opportunity();
//            opportunity.setId(oppId);
//            opportunity.setTCV_Amount_in_USD__c((double) 0);
//            opportunity.setNet_TCV_in_USD__c((double) 0);
////            opportunitiesToUpdate.add(new Opportunity(Long = oppId, TCV_Amount_in_USD__c = 0, Net_TCV_in_USD__c = 0));
//        }
//        return opportunitiesToUpdate;
//    }
//
//    public Set<Long> getOppNeedToUpdateTCV(List<Contract_Opp_Alloc_Operation__e> allocationEventList) {
//        Set<Long> oppIdSetToUpdate = new Set<Long>();
//        for (Contract_Opp_Alloc_Operation__e allocOperation : allocationEventList) {
//            if (allocOperation.Operation__c != "Update" || containsRelevantFields(allocOperation.Change_Fields__c)) {
//                if (allocOperation.Extra__c == null) {
//                    continue;
//                }
//                Map<String, Object> opportunityIdMap = (Map<String, Object>) JSON.deserializeUntyped(allocOperation.Extra__c);
//                Object opportunityId = opportunityIdMap.get("opportunity_id");
//                if (opportunityId != null) {
//                    oppIdSetToUpdate.add((Long) opportunityId);
//                }
//            }
//        }
//        return oppIdSetToUpdate;
//    }
//
//    public Double calculateOpportunityGrossTCV(List<Contract_Opportunity_Allocation__c> allocations) {
//        Double opportunityTCVAmount = (double) 0;
//        for (Contract_Opportunity_Allocation__c allocation : allocations) {
//            opportunityTCVAmount += allocation.Allocated_TCV_Amount_in_USD__c;
//        }
//        return opportunityTCVAmount.setScale(2, RoundingMode.HALF_UP);
//    }
//
//    public Double calculateOpportunityNetTCV(List<Contract_Opportunity_Allocation__c> allocations) {
//        Double opportunityTCVAmount = (double) 0;
//        for (Contract_Opportunity_Allocation__c allocation : allocations) {
//            opportunityTCVAmount += allocation.Net_TCV_in_USD__c;
//        }
//        return opportunityTCVAmount.setScale(2, RoundingMode.HALF_UP);
//    }

}
