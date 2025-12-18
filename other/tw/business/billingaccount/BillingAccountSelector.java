package other.tw.business.billingaccount;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Billing_Account__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.ArrayList;
import java.util.List;

public class BillingAccountSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public List<Billing_Account__c> getBillingAccountByAccountId(Long accountId) throws ApiEntityServiceException {
        String sql = "SELECT id FROM billing_Account__c WHERE account__c = "+accountId;
        return NeoCrmUtils.query(sql,Billing_Account__c.class);
    }
}
