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

public class OppLinkSolutionDeleteBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("oppLinkSolution before delete triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<OppLinkSolution__c, DataResult> resultMap = new HashMap<>();
        Map<Long, OppLinkSolution__c> deleteMap = new HashMap<>();

        for (XObject xObject : dataList) {
            OppLinkSolution__c deleteOppSolution = (OppLinkSolution__c) xObject;
            deleteMap.put(deleteOppSolution.getId(),deleteOppSolution);
            resultMap.put(deleteOppSolution,new DataResult(true, "", deleteOppSolution));
        }

        List<OppLinkSolution__c> deleteList= new ArrayList<>(deleteMap.values());

       OppLinkSolutionService  oppLinkSolutionService = new OppLinkSolutionService();

        try{
            oppLinkSolutionService.checkSolutionPercentageBeforeDelete(deleteList,resultMap);
        } catch (Exception e) {
            LOGGER.error("oppLinkSolution before delete Exception message =>" + e.getMessage());
            LOGGER.error("oppLinkSolution before delete Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}