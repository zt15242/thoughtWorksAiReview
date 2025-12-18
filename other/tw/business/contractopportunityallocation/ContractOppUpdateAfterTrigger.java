package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.contract.ContractService;

import java.util.*;

public class ContractOppUpdateAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static ContractService contractService = new ContractService();
    private final ContractOppService contractOppService = new ContractOppService();


    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("ContractOpp After Update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long,DataResult> resultMap = new HashMap<>();
        Map<Long, Contract_Opportunity_Allocation__c> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Contract_Opportunity_Allocation__c newContractOpp = (Contract_Opportunity_Allocation__c) xObject;
            newMap.put(newContractOpp.getId(),newContractOpp);
            resultMap.put(newContractOpp.getId(),new DataResult(true, "", newContractOpp));
        }

        List<Contract_Opportunity_Allocation__c> newList = new ArrayList<>(newMap.values());

        try{

//            contractService.publishContractOppAllocEvent(triggerRequest);
            contractOppService.reCalOpportunityOCV(newList);
        } catch (Exception e) {
            LOGGER.error("ContractOpp After Update Exception message =>" + e.getMessage());
            LOGGER.error("ContractOpp After Update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
