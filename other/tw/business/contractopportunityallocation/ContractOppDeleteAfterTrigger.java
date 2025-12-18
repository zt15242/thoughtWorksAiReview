package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.contract.ContractService;

import java.util.*;

public class ContractOppDeleteAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final ContractService contractService = new ContractService();

    private final ContractOppService contractOppService = new ContractOppService();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("ContractOpp After Delete triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long,DataResult> resultMap = new HashMap<>();
        Map<Long, Contract_Opportunity_Allocation__c> deleteMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Contract_Opportunity_Allocation__c deleteContractOpp = (Contract_Opportunity_Allocation__c) xObject;
            deleteMap.put(deleteContractOpp.getId(),deleteContractOpp);
            resultMap.put(deleteContractOpp.getId(),new DataResult(true, "", deleteContractOpp));
        }

        List<Contract_Opportunity_Allocation__c> deleteList = new ArrayList<>(deleteMap.values());

        try{
//            return contractService.publishContractOppAllocEvent(triggerRequest);
            contractOppService.reCalOpportunityOCV(deleteList);
        } catch (Exception e) {
            LOGGER.error("ContractOpp After Delete Exception message =>" + e.getMessage());
            LOGGER.error("ContractOpp After Delete Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
