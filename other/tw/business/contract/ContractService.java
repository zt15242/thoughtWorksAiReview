package other.tw.business.contract;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.trigger.DataResult;
import other.tw.business.Constants;
import other.tw.business.opportunity.OpportunitySelector;
import other.tw.business.util.CollectionUtils;
import other.tw.business.util.NeoCrmUtils;

import java.io.IOException;
import java.util.*;

public class ContractService {

    public static String getEntityTypeName(Long typeId){
        if (Constants.CONTRACT_MSA_ENTITY_TYPE_ID.equals(typeId)) {
            return "Master Service Agreement";
        } else if (Constants.CONTRACT_SOW_ENTITY_TYPE_ID.equals(typeId)) {
            return "Purchase Order";
        } else if (Constants.CONTRACT_PO_ENTITY_TYPE_ID.equals(typeId)) {
            return "Po";
        }else if (Constants.CONTRACT_OTHER_ENTITY_TYPE_ID.equals(typeId)) {
            return "Other";
        }else {
            return null;
        }
    }

    public Map<Long, Contract> getParentContractsByExtend(Collection<Contract> values) {
        Map<Long, Contract> map = new HashMap<>();
        return map;
    }

    public Map<Long, Contract> getParentContractsByBelongTo(Collection<Contract> values) {
        Map<Long, Contract> map = new HashMap<>();
        return map;
    }

    public boolean isIncludeInTCVByContract(Contract contract) {
        return false;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final ContractSelector contractSelector = new ContractSelector();

    private static final String ACCOUNT_LINKED_OPPORTUNITY_ERROR_MESSAGE = "Select the opportunities under the account this contract belongs to.";


    private static final OpportunitySelector opportunitySelector = new OpportunitySelector();
//    public TriggerResponse publishContractOppAllocEvent(TriggerRequest triggerRequest) {
//        List<Contract_Opp_Alloc_Operation__e> events = generateContractOppAllocEvent(tp);
//        if (events.size() > 0) {
//            for (Contract_Opp_Alloc_Operation__e event : events) {
//                LOGGER.error("Starting to publish contractOppAllocEvent to update opportunity level TCV, id: " + event.Extra__c + ", record id: " + event.Record_Id__c);
//            }
//            List<Long> eventTrackingIds = EventTrackingService.createContractOppAllocEventTracking(events);
//            addTrackingIdToEvents(eventTrackingIds, events);
//            EventPublisher.publishContractOppAllocOperationEvent(events);
//        }
//    }

    public void checkIfLinkedOpportunitiesHaveSameAccounts(List<Contract_Opportunity_Allocation__c> contractOpportunityAllocations, Map<Contract_Opportunity_Allocation__c, DataResult> resultMap) throws NoSuchFieldException, IllegalAccessException, ApiEntityServiceException {
        Set<Long> contractIds = CollectionUtils.getIdSet(contractOpportunityAllocations, "contract__c");
        Set<Long> oppIds = CollectionUtils.getIdSet(contractOpportunityAllocations, "opportunity__c");
        List<Opportunity> opportunities = opportunitySelector.getAccountsById(oppIds);

        Map<Long, Long> opportunityIdAccountId = new HashMap<>();
        for (Opportunity opportunity : opportunities) {
            opportunityIdAccountId.put(opportunity.getId(), opportunity.getAccountId());
        }

        List<Contract> contractDetails = contractSelector.getContractsByContractId(contractIds);
        Map<Long, Long> contractIdAccountId = new HashMap<>();
        for (Contract contract : contractDetails) {
            contractIdAccountId.put(contract.getId(), contract.getAccountId());
        }

        for (Contract_Opportunity_Allocation__c contractOpportunityAllocation : contractOpportunityAllocations) {
            Long contractId = contractOpportunityAllocation.getContract__c();
            Long oppId = contractOpportunityAllocation.getOpportunity__c();
            Long contractAccountId = contractIdAccountId.get(contractId);
            Long opportunityAccountId = opportunityIdAccountId.get(oppId);
            if (!Objects.equals(contractAccountId, opportunityAccountId)) {
                DataResult dataResult = resultMap.get(contractOpportunityAllocation);
                dataResult.setSuccess(false);
                dataResult.setMsg(ACCOUNT_LINKED_OPPORTUNITY_ERROR_MESSAGE);
//                contractOpportunityAllocation.addError(ACCOUNT_LINKED_OPPORTUNITY_ERROR_MESSAGE);
            }
        }
    }

}
