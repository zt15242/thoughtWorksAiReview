package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.contract.ContractService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class ContractOppUpdateBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    private static final ContractService contractService = new ContractService();
    private static final ContractOppService contractOpportunityAllocationService = new ContractOppService();


    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("ContractOpp Before Update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Contract_Opportunity_Allocation__c,DataResult> resultMap = new HashMap<>();
        Map<Long, Contract_Opportunity_Allocation__c> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Contract_Opportunity_Allocation__c newContractOpp = (Contract_Opportunity_Allocation__c) xObject;
            newMap.put(newContractOpp.getId(),newContractOpp);
            resultMap.put(newContractOpp,new DataResult(true, "", newContractOpp));
        }

        List<Contract_Opportunity_Allocation__c> newList = new ArrayList<>(newMap.values());

        try{
            List<JSONObject> oldJsonList = NeoCrmUtils.getObjectJsonList(new ArrayList<>(newMap.keySet()),"currencyIsoCode__c","contract_Opportunity_Allocation__c");
            Map<Long, Contract_Opportunity_Allocation__c> oldMap = NeoCrmUtils.objectJsonListToIdMap(oldJsonList,Contract_Opportunity_Allocation__c.class);
            contractOpportunityAllocationService.validCurrencyToSameAsContract(newList, oldMap);
            contractService.checkIfLinkedOpportunitiesHaveSameAccounts(newList,resultMap);
        } catch (Exception e) {
            LOGGER.error("ContractOpp Before Update Exception message =>" + e.getMessage());
            LOGGER.error("ContractOpp Before Update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }


}
