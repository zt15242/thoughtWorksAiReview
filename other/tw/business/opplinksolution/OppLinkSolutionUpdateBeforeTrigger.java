package other.tw.business.opplinksolution;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.OppLinkSolution__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.tw.business.util.NeoCrmUtils;

import java.awt.image.LookupOp;
import java.util.*;

public class OppLinkSolutionUpdateBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("oppLinkSolution before update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<OppLinkSolution__c, DataResult> resultMap = new HashMap<>();
        Map<Long, OppLinkSolution__c> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            OppLinkSolution__c newOppSolution = (OppLinkSolution__c) xObject;
            newMap.put(newOppSolution.getId(),newOppSolution);
            resultMap.put(newOppSolution,new DataResult(true, "", newOppSolution));
        }

        List<OppLinkSolution__c> newList= new ArrayList<>(newMap.values());

       OppLinkSolutionService  oppLinkSolutionService = new OppLinkSolutionService();

        try{
            List<JSONObject> oldSolutionJsonList = NeoCrmUtils.getObjectJsonList(new ArrayList<>(newMap.keySet()),"opportunity__c,Percentage__c","oppLinkSolution__c");
            Map<Long, OppLinkSolution__c> oldMap = NeoCrmUtils.objectJsonListToIdMap(oldSolutionJsonList, OppLinkSolution__c.class);
            LOGGER.error("oldSolutionJsonList => " + JSON.toJSONString(oldSolutionJsonList));

            oppLinkSolutionService.checkSolutionPercentageBeforeUpdate(oldMap,newMap,resultMap);
        } catch (Exception e) {
            LOGGER.error("oppLinkSolution before update Exception message =>" + e.getMessage());
            LOGGER.error("oppLinkSolution before update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}