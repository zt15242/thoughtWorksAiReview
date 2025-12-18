package other.tw.business.opplinksolution;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.OppLinkSolution__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;

import java.util.*;

public class OppLinkSolutionInsertBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("oppLinkSolution before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<OppLinkSolution__c, DataResult> resultMap = new HashMap<>();

        List<OppLinkSolution__c> newList= new ArrayList<>();

        for (XObject xObject : dataList) {
            OppLinkSolution__c newOppSolution = (OppLinkSolution__c) xObject;
            newList.add(newOppSolution);
            resultMap.put(newOppSolution,new DataResult(true, "", newOppSolution));
        }

       OppLinkSolutionService  oppLinkSolutionService = new OppLinkSolutionService();

        try{
            oppLinkSolutionService.checkSolutionPercentageBeforeInsert(newList,resultMap);
        } catch (Exception e) {
            LOGGER.error("oppLinkSolution before insert Exception message =>" + e.getMessage());
            LOGGER.error("oppLinkSolution before insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}