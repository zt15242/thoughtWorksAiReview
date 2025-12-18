//package other.tw.business;
//
//import com.alibaba.fastjson.JSON;
//import com.rkhd.platform.sdk.exception.ScriptBusinessException;
//import com.rkhd.platform.sdk.log.Logger;
//import com.rkhd.platform.sdk.log.LoggerFactory;
//import com.rkhd.platform.sdk.model.XObject;
//import com.rkhd.platform.sdk.business.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class SalesContractOppAllocSubscriberAfterInsertTrigger implements Trigger {
//    private static final Logger LOGGER = LoggerFactory.getLogger();
//
//    @Override
//    public TriggerResponse execute(TriggerRequest triggerRequest) throws ScriptBusinessException {
//        LOGGER.error("SalesContractOppAllocSubscriberAfterInsertTrigger triggerRequest =>" + JSON.toJSONString(triggerRequest));
//
//        List<XObject> dataList = triggerRequest.getDataList();
//        TriggerContext triggerContext = new TriggerContext();
//        LOGGER.error("request log => " + dataList);
//
//        Map<Long, DataResult> resultMap = new HashMap<>();
//        Map<Long, Contract_Opp_Alloc_Operation__e> newMap = new HashMap<>();
//
//        for (XObject xObject : dataList) {
//            Contract_Opp_Alloc_Operation__e newOpp = (Contract_Opp_Alloc_Operation__e) xObject;
//            newMap.put(newOpp.getId(),newOpp);
//            resultMap.put(newOpp.getId(),new DataResult(true, "", newOpp));
//        }
//
//        List<Contract_Opp_Alloc_Operation__e> newList= new ArrayList<>(newMap.values());
//
//        OpportunityService opportunityService = new OpportunityService();
//
//        opportunityService.updateOpportunityLevelTCV((newList);
//
//        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()), triggerContext);
//    }
//}
