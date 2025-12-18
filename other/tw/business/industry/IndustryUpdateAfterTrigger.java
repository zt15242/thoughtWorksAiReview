package other.tw.business.industry;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Industry__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;

import java.util.*;

public class IndustryUpdateAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("industry after update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Industry__c,DataResult> resultMap = new HashMap<>();
        Map<Long, Industry__c> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Industry__c newIndustry = (Industry__c) xObject;
            newMap.put(newIndustry.getId(),newIndustry);
            resultMap.put(newIndustry,new DataResult(true, "", newIndustry));
        }

        List<Industry__c> newList= new ArrayList<>(newMap.values());

        IndustryService industryService = new IndustryService();

        try{
            industryService.sendToErp(newList);
        } catch (Exception e) {
            LOGGER.error("industry after update Exception message =>" + e.getMessage());
            LOGGER.error("industry after update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
