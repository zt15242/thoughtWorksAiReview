package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.contract.ContractService;

import java.util.*;

public class ContractOppInsertAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final ContractService contractService = new ContractService();
    private static final ContractOppService contractOppService = new ContractOppService();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("ContractOpp After Insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

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
            contractOppService.updateAllocationsIncludeInTCVInAfterInsert(newList);
            contractOppService.reCalOpportunityOCV(newList);
//            return contractService.publishContractOppAllocEvent(triggerRequest);
        } catch (Exception e) {
            LOGGER.error("ContractOpp After Insert Exception message =>" + e.getMessage());
            LOGGER.error("ContractOpp After Insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
