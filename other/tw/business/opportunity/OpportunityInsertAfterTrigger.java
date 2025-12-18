package other.tw.business.opportunity;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.service.ErpService;

import java.util.*;

public class OpportunityInsertAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
//        LOGGER.error("opportunity after insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long,DataResult> resultMap = new HashMap<>();
        Map<Long, Opportunity> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Opportunity newOpp = (Opportunity) xObject;
            newMap.put(newOpp.getId(),newOpp);
            resultMap.put(newOpp.getId(),new DataResult(true, "", newOpp));
        }

        List<Opportunity> newList= new ArrayList<>(newMap.values());

        OpportunityService opportunityService = new OpportunityService();

        try{
            opportunityService.checkStatus(newList);
            opportunityService.sendOpportunityToErp(newList);
        } catch (Exception e) {
            LOGGER.error("opportunity after insert Exception message =>" + e.getMessage());
            LOGGER.error("opportunity after insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
