package other.tw.business.opportunity;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;

import java.util.*;

public class OpportunityInsertBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
//        LOGGER.error("opportunity before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        TriggerContext triggerContext = new TriggerContext();
        LOGGER.error("request log => " + dataList);

        Map<Opportunity,DataResult> resultMap = new HashMap<>();
        List<Opportunity> newList= new ArrayList<>();

        for (XObject xObject : dataList) {
            Opportunity newOpp = (Opportunity) xObject;
            newList.add(newOpp);
            resultMap.put(newOpp,new DataResult(true, "", newOpp));
        }

        OpportunityService opportunityService = new OpportunityService();

        try{
            opportunityService.setOpportunityDefaultValue(newList);
        } catch (Exception e) {
            LOGGER.error("opportunity before insert Exception message =>" + e.getMessage());
            LOGGER.error("opportunity before insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()), triggerContext);
    }


}
