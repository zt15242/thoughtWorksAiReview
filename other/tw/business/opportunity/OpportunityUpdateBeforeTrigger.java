package other.tw.business.opportunity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class OpportunityUpdateBeforeTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
//        LOGGER.error("opportunity before update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        TriggerContext triggerContext = new TriggerContext();
        LOGGER.error("request log => " + dataList);

        Map<Opportunity,DataResult> resultMap = new HashMap<>();
        Map<Long, Opportunity> newMap = new HashMap<>();
        Map<Long, Opportunity> oldMap = new HashMap<>();
        List<JSONObject> oldOppJsonList = new ArrayList<>();

        for (XObject xObject : dataList) {
            Opportunity newOpp = (Opportunity) xObject;
            newMap.put(newOpp.getId(),newOpp);
            resultMap.put(newOpp,new DataResult(true, "", newOpp));
        }

        List<Opportunity> newList= new ArrayList<>(newMap.values());


        Set<Long> oppIds = newMap.keySet();

        OpportunityService opportunityService = new OpportunityService();

        try {
            oldOppJsonList = NeoCrmUtils.getObjectJsonList(new ArrayList<>(oppIds), "opportunity_Contract_Value__c, saleStageId, stageName_F__c, currencyUnit, latest_Monthly_Exchange_Rate__c", "opportunity");
            LOGGER.error("oldOppJsonList: "+oldOppJsonList);
            triggerContext.set("oldOppJsonList",JSON.toJSONString(oldOppJsonList));
//            oldOppJsonList = opportunityService.handleCurrencyUnit(oldOppJsonList);
//            LOGGER.error("oldOppJsonList after handleCurrencyUnit: "+oldOppJsonList);
            oldMap = NeoCrmUtils.objectJsonListToIdMap(oldOppJsonList, Opportunity.class);

//            opportunityService.reCalContractValueWhenCurrencyStageContractValueChanged(oldMap, newList);
//            opportunityService.updateFieldBeforeUpdate(oldMap, newList);
            opportunityService.checkStageChangedBeforeUpdate(oldMap,newMap,resultMap);

        } catch (ApiEntityServiceException | TriggerContextException e) {
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()), triggerContext);
    }

}
