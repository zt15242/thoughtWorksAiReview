package other.tw.business.billingaccount;

import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Billing_Account__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import other.tw.business.Constants;
import other.tw.business.account.AccountSelector;
import other.tw.business.service.ErpService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BillingAccountService {

    private final AccountSelector accountSelector = new AccountSelector();

    public void sendAccountToErp(List<Billing_Account__c> newList) throws ApiEntityServiceException {

        Set<Long> accIdSet = new HashSet<>();

        for(Billing_Account__c billingAcc : newList){
            accIdSet.add(billingAcc.getAccount__c());
        }

        List<Account> accounts = accountSelector.getAccountApprovalStatus(accIdSet);
        List<String> approvedIds = new ArrayList<>();

        for(Account acc : accounts){
            if(Constants.ACCOUNT_APPROVAL_STATUS.equals(acc.getApproval_Status__c())){
                approvedIds.add(acc.getId().toString());
            }
        }

        new ErpService().sendAccountToErpById2(approvedIds);
    }
}
