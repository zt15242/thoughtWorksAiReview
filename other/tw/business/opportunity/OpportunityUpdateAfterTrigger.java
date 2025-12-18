package other.tw.business.opportunity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class OpportunityUpdateAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
//        LOGGER.error("opportunity after update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Opportunity,DataResult> resultMap = new HashMap<>();
        Map<Long, Opportunity> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Opportunity newOpp = (Opportunity) xObject;
            newMap.put(newOpp.getId(),newOpp);
            resultMap.put(newOpp,new DataResult(true, "", newOpp));
        }

        List<Opportunity> newList= new ArrayList<>(newMap.values());

        OpportunityService opportunityService = new OpportunityService();

        try{
            TriggerContext triggerContext = triggerRequest.getTriggerContext();
            LOGGER.error("opportunity after update triggerContext =>" + triggerContext);
            List<JSONObject> oldOppJsonList = JSONObject.parseArray(triggerContext.get("oldOppJsonList"), JSONObject.class);
            Map<Long,Opportunity> oldMap = NeoCrmUtils.objectJsonListToIdMap(oldOppJsonList, Opportunity.class);

            opportunityService.checkStatus(newList);
            opportunityService.checkStageChanged(oldMap,newMap);
        } catch (Exception e) {
            LOGGER.error("opportunity after update Exception message =>" + e.getMessage());
            LOGGER.error("opportunity after update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
