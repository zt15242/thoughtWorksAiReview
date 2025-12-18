package other.tw.business.billingaccount;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Billing_Account__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;

import java.util.*;

public class BillingAccountInsertAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("billingAccount after insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Billing_Account__c, DataResult> resultMap = new HashMap<>();
        Map<Long, Billing_Account__c> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Billing_Account__c newBillingAcc = (Billing_Account__c) xObject;
            newMap.put(newBillingAcc.getId(),newBillingAcc);
            resultMap.put(newBillingAcc,new DataResult(true, "", newBillingAcc));
        }

        List<Billing_Account__c> newList= new ArrayList<>(newMap.values());

        BillingAccountService billingAccountService = new BillingAccountService();

        try{
            billingAccountService.sendAccountToErp(newList);
        } catch (Exception e) {
            LOGGER.error("billingAccount after insert Exception message =>" + e.getMessage());
            LOGGER.error("billingAccount after insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}