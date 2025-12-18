package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.contract.ContractService;

import java.util.*;

public class ContractOppInsertBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final ContractService contractService = new ContractService();
    private static final ContractOppService contractOpportunityAllocationService = new ContractOppService();


    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("ContractOpp Before Insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Contract_Opportunity_Allocation__c,DataResult> resultMap = new HashMap<>();
        List<Contract_Opportunity_Allocation__c> newList = new ArrayList<>();

        for (XObject xObject : dataList) {
            Contract_Opportunity_Allocation__c newContractOpp = (Contract_Opportunity_Allocation__c) xObject;
            newList.add(newContractOpp);
            resultMap.put(newContractOpp,new DataResult(true, "", newContractOpp));
        }


        try{
            contractOpportunityAllocationService.setDefaultCurrencyFromContract(newList);
            contractService.checkIfLinkedOpportunitiesHaveSameAccounts(newList,resultMap);
        } catch (Exception e) {
            LOGGER.error("ContractOpp Before Insert Exception message =>" + e.getMessage());
            LOGGER.error("ContractOpp Before Insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
