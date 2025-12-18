package other.tw.business.contractopportunityallocation;

import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import other.tw.business.Constants;
import other.tw.business.contract.ContractSelector;
import other.tw.business.contract.ContractService;
import other.tw.business.opportunity.OpportunitySelector;
import other.tw.business.opportunity.OpportunityService;
import other.tw.business.util.NeoCrmUtils;
import other.tw.business.util.CollectionUtils;


import java.util.*;

public class ContractOppService {
    private static final ContractService contractService = new ContractService();
    private static ContractSelector contractSelector = new ContractSelector();
    private static ContractOppSelector contractOppAllocationSelector = new ContractOppSelector();
    private static final OpportunitySelector opportunitySelector = new OpportunitySelector();
    private final OpportunityService opportunityService = new OpportunityService();


    public static final String GROSS_TCV = "grossTCV";
    public static final String NET_TCV = "netTCV";

    private ContractOppService(ContractOppSelector mockContractOppAllocationSelector, ContractSelector mockContractSelector) {
        contractSelector = mockContractSelector;
        contractOppAllocationSelector = mockContractOppAllocationSelector;
    }

    public ContractOppService() {
    }

    public void reCalOpportunityOCV(List<Contract_Opportunity_Allocation__c> allocationList) throws ApiEntityServiceException {
        Set<Long> oppIdSet = new HashSet<>();

        for (Contract_Opportunity_Allocation__c allocation : allocationList) {
                oppIdSet.add(allocation.getOpportunity__c());
        }

        List<Opportunity> oppList = opportunitySelector.getOpportunityById(oppIdSet);

        opportunityService.reCalOpportunityContractValue(oppList);

        NeoCrmUtils.update(oppList,"Allocation changed, recalculate OCV");
    }

    public void updateAllocationsTCVAmount(Map<Long, Contract> newContractMap) throws ApiEntityServiceException {
        List<Contract_Opportunity_Allocation__c> allocations = calculateTCVAmount(newContractMap);
        if (!allocations.isEmpty()) {
            Long entityType = 4037899940271593L;
            NeoCrmUtils.upsert(allocations, "contract_Opportunity_Allocation__c", entityType, "allocations");
        }
    }


    private List<Contract_Opportunity_Allocation__c> calculateTCVAmount(Map<Long, Contract> contractMap) throws ApiEntityServiceException {
        Map<Long, Contract> parentContractsByExtend = contractService.getParentContractsByExtend(contractMap.values());
        List<Contract_Opportunity_Allocation__c> parentAllocationsByExtend = contractOppAllocationSelector.getContractAllocationsByContractIds(parentContractsByExtend.keySet());
        Map<Long, List<Contract_Opportunity_Allocation__c>> parentContractIdToAllocations = mapAllocationsByContractId(parentAllocationsByExtend);
        List<Contract_Opportunity_Allocation__c> allocations = contractOppAllocationSelector.getContractAllocationsByContractIds(contractMap.keySet());
        Map<Long, List<Contract_Opportunity_Allocation__c>> contractIdToAllocations = mapAllocationsByContractId(allocations);
        List<Contract_Opportunity_Allocation__c> allocationsToUpsert = new ArrayList<>();
        for (Contract contract : contractMap.values()) {
            if (!contract.getIs_Amendatory__c() || Objects.equals(contract.getAmendatory_Type__c(), Constants.AMENDATORY_TYPE_ADDITIONAL_TCV)) {
                allocationsToUpsert.addAll(calculateAllocationTCVAmount(contractIdToAllocations.get(contract.getId()), false));
            } else if (Objects.equals(contract.getAmendatory_Type__c(), Constants.AMENDATORY_TYPE_OVERWRITE_TVC)) {
                allocationsToUpsert.addAll(calculateAllocationOverwriteTCVAmount(contract, parentContractIdToAllocations.get(contract.getExtend_From__c()), contractIdToAllocations.get(contract.getId())));
            } else {
                allocationsToUpsert.addAll(calculateAllocationTCVAmount(contractIdToAllocations.get(contract.getId()), true)); //NO_TCV
            }
        }

        return allocationsToUpsert;
    }

    private Map<Long, List<Contract_Opportunity_Allocation__c>> mapAllocationsByContractId(List<Contract_Opportunity_Allocation__c> allocations) {
        Map<Long, List<Contract_Opportunity_Allocation__c>> allocationsMap = new HashMap<>();
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            if (!allocationsMap.containsKey(allocation.getContract__c())) {
                allocationsMap.put(allocation.getContract__c(), new ArrayList<>());
            }
            allocationsMap.get(allocation.getContract__c()).add(allocation);
        }
        return allocationsMap;
    }

    private Map<Long, Contract_Opportunity_Allocation__c> getOppIdToAllocationsByIncludeInTCV(List<Contract_Opportunity_Allocation__c> allocations) {
        Map<Long, Contract_Opportunity_Allocation__c> allocationsMap = new HashMap<>();
        if (allocations == null) {
            return allocationsMap;
        }
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            if (!shouldCalculateAllocationTCVAmount(allocation)) {
                allocation.setAllocated_TCV_Amount__c((double) 0);
                allocation.setNet_TCV_New__c((double) 0);
            } else {
                allocationsMap.put(allocation.getOpportunity__c(), allocation);
            }
        }
        return allocationsMap;
    }

    private List<Contract_Opportunity_Allocation__c> calculateAllocationOverwriteTCVAmount(
            Contract contract,
            List<Contract_Opportunity_Allocation__c> parentAllocationsByExtend,
            List<Contract_Opportunity_Allocation__c> allocations
    ) {
        List<Contract_Opportunity_Allocation__c> allocationsToUpsert = new ArrayList<>();
        if (allocations == null || allocations.isEmpty()) {
            return allocationsToUpsert;
        }

        allocationsToUpsert.addAll(allocations);

        Map<Long, Contract_Opportunity_Allocation__c> oppIdToAllocationMap = getOppIdToAllocationsByIncludeInTCV(allocations);
        Map<Long, Contract_Opportunity_Allocation__c> parentOppIdToAllocationMap = getOppIdToAllocationsByIncludeInTCV(parentAllocationsByExtend);

        if (oppIdToAllocationMap.isEmpty()) {
            return allocationsToUpsert;
        }

        Set<Long> allOppIds = new HashSet<>();
        allOppIds.addAll(oppIdToAllocationMap.keySet());
        allOppIds.addAll(parentOppIdToAllocationMap.keySet());
        for (Long oppId : allOppIds) {
            Contract_Opportunity_Allocation__c allocation = oppIdToAllocationMap.get(oppId);
            Contract_Opportunity_Allocation__c parentAllocation = parentOppIdToAllocationMap.get(oppId);
            if (oppIdToAllocationMap.containsKey(oppId) && parentOppIdToAllocationMap.containsKey(oppId)) {
                allocation.setAllocated_TCV_Amount__c(calculateTCVAmountByFieldType(allocation, GROSS_TCV) - calculateTCVAmountByFieldType(parentAllocation, GROSS_TCV));
                allocation.setNet_TCV_New__c(calculateTCVAmountByFieldType(allocation, NET_TCV) - calculateTCVAmountByFieldType(parentAllocation, NET_TCV));
            } else if (parentOppIdToAllocationMap.containsKey(oppId)) {
                Contract_Opportunity_Allocation__c newAllocation = new Contract_Opportunity_Allocation__c();
                newAllocation.setContract__c(contract.getId());
                newAllocation.setOpportunity__c(oppId);
                newAllocation.setAllocated_Amount__c((double) 0);
                newAllocation.setAllocated_TCV_Amount__c(0 - calculateTCVAmountByFieldType(parentAllocation, GROSS_TCV));
                newAllocation.setNet_TCV_New__c(0 - calculateTCVAmountByFieldType(parentAllocation, NET_TCV));
                newAllocation.setShould_Include_in_Opportunity_Level_TCV__c(true);
                allocationsToUpsert.add(newAllocation);
            } else if (oppIdToAllocationMap.containsKey(oppId)) {
                allocation.setAllocated_TCV_Amount__c(calculateTCVAmountByFieldType(allocation, GROSS_TCV));
                allocation.setNet_TCV_New__c(calculateTCVAmountByFieldType(allocation, NET_TCV));
            }
        }

        return allocationsToUpsert;
    }

    private Double calculateTCVAmountByFieldType(Contract_Opportunity_Allocation__c allocation, String fieldType) {
        if (GROSS_TCV.equals(fieldType)) {
            return allocation.getAllocated_Amount__c();
        }

        if (NET_TCV.equals(fieldType)) {
            Double expense = allocation.getExpense__c() == null ? 0 : allocation.getExpense__c();
            Double discount = allocation.getEstimated_Discount__c() == null ? 0 : allocation.getEstimated_Discount__c();
            return allocation.getAllocated_Amount__c() - expense - discount;
        }

        return (double) 0;
    }

    private List<Contract_Opportunity_Allocation__c> calculateAllocationTCVAmount(List<Contract_Opportunity_Allocation__c> currentAllocations, Boolean isNoTCV) {
        if (currentAllocations == null) {
            return new ArrayList<>();
        }
        for (Contract_Opportunity_Allocation__c allocation : currentAllocations) {
            if (!shouldCalculateAllocationTCVAmount(allocation) || isNoTCV) {
                allocation.setNet_TCV_New__c((double) 0);
                allocation.setAllocated_TCV_Amount__c((double) 0);
            } else {
                allocation.setAllocated_TCV_Amount__c(calculateTCVAmountByFieldType(allocation, GROSS_TCV));
                allocation.setNet_TCV_New__c(calculateTCVAmountByFieldType(allocation, NET_TCV));
            }
        }
        return currentAllocations;
    }

    private Boolean shouldCalculateAllocationTCVAmount(Contract_Opportunity_Allocation__c allocation) {
        return allocation != null && allocation.getShould_Include_in_Opportunity_Level_TCV__c();
    }

    public void updateAllocationsIncludeInTCV(Map<Long, Contract> contractMap) throws ApiEntityServiceException {
        List<Contract_Opportunity_Allocation__c> allocations = calculateAllocationsIncludeInTCV(contractMap);

        if (!allocations.isEmpty()) {
            NeoCrmUtils.update(allocations, "updateAllocationsIncludeInTCV");
        }
    }


    private List<Contract_Opportunity_Allocation__c> calculateAllocationsIncludeInTCV(Map<Long, Contract> contractMap) throws ApiEntityServiceException {
        Map<Long, Contract> parentContractsByBelongTo = contractService.getParentContractsByBelongTo(contractMap.values());
        Map<Long, Boolean> parentContractIdToAllocationIncludeInTCVMap = getParentContractIdToAllocationIncludeInTCVMap(parentContractsByBelongTo);
        List<Contract_Opportunity_Allocation__c> allocations = contractOppAllocationSelector.getContractAllocationsByContractIds(contractMap.keySet());

        for (Contract_Opportunity_Allocation__c allocation : allocations) {
//            Long contractEntityTypeId = allocation.Contract__r.getEntityType;
            Contract contract = contractMap.get(allocation.getContract__c());
            Long contractEntityTypeId = contract.getEntityType();
            if (Constants.CONTRACT_MSA_ENTITY_TYPE_ID.equals(contractEntityTypeId)) {
                updateAllocationIncludeInTCVFlag(allocation, contract, true);
            } else if (Constants.CONTRACT_SOW_ENTITY_TYPE_ID.equals(contractEntityTypeId) || Constants.CONTRACT_OTHER_ENTITY_TYPE_ID.equals(contractEntityTypeId)) {
                Contract parentMSAContract = parentContractsByBelongTo.get(contract.getBelong_To_MSA__c());
                Boolean includeByMSA = isAllocationIncludeInTCVByParentContract(parentMSAContract, parentContractIdToAllocationIncludeInTCVMap);
                updateAllocationIncludeInTCVFlag(allocation, contract, includeByMSA);
            } else if (Constants.CONTRACT_PO_ENTITY_TYPE_ID.equals(contractEntityTypeId)) {
                Contract parentMSAContract = parentContractsByBelongTo.get(contract.getBelong_To_MSA__c());
                Contract parentSOWContract = parentContractsByBelongTo.get(contract.getBelong_To_SOW__c());
                Boolean includeByTopLevelMSA = true;
                if (parentSOWContract != null) {
                    Contract parentTopLevelMSAContract = parentContractsByBelongTo.get(parentSOWContract.getBelong_To_MSA__c());
                    includeByTopLevelMSA = isAllocationIncludeInTCVByParentContract(parentTopLevelMSAContract, parentContractIdToAllocationIncludeInTCVMap);
                }
                Boolean includeByMSA = isAllocationIncludeInTCVByParentContract(parentMSAContract, parentContractIdToAllocationIncludeInTCVMap);
                Boolean includeBySOW = isAllocationIncludeInTCVByParentContract(parentSOWContract, parentContractIdToAllocationIncludeInTCVMap);
                updateAllocationIncludeInTCVFlag(allocation, contract, includeByMSA && includeBySOW && includeByTopLevelMSA);
            } else {
                allocation.setShould_Include_in_Opportunity_Level_TCV__c(false);
            }
        }

        return allocations;
    }

    private Map<Long, Boolean> getParentContractIdToAllocationIncludeInTCVMap(Map<Long, Contract> parentContractsByBelongTo) throws ApiEntityServiceException {
        List<Contract_Opportunity_Allocation__c> parentAllocationsByBelongTo = contractOppAllocationSelector.getContractAllocationsByContractIds(parentContractsByBelongTo.keySet());
        Map<Long, Boolean> parentContractIdToAllocationIncludeInTCVMap = new HashMap<>();
        for (Contract_Opportunity_Allocation__c parentAllocation : parentAllocationsByBelongTo) {
            parentContractIdToAllocationIncludeInTCVMap.put(parentAllocation.getContract__c(), parentAllocation.getShould_Include_in_Opportunity_Level_TCV__c());
        }
        return parentContractIdToAllocationIncludeInTCVMap;
    }

    private Boolean isAllocationIncludeInTCVByParentContract(Contract parentContractByBelongTo, Map<Long, Boolean> parentContractIdToAllocationIncludeInTCVMap) {
        return !(parentContractByBelongTo != null &&
                parentContractIdToAllocationIncludeInTCVMap.get(parentContractByBelongTo.getId()) != null &&
                parentContractIdToAllocationIncludeInTCVMap.get(parentContractByBelongTo.getId()));
    }

    private void updateAllocationIncludeInTCVFlag(Contract_Opportunity_Allocation__c allocation, Contract contract, Boolean includeByParent) {
        if (!contractService.isIncludeInTCVByContract(contract)) {
            allocation.setShould_Include_in_Opportunity_Level_TCV__c(false);
        } else {
            allocation.setShould_Include_in_Opportunity_Level_TCV__c(includeByParent);
        }
    }

    private void updateChildAllocationsForIncludeInTCVAndTCVAmount(Map<Long, Contract> newContractMap) throws ApiEntityServiceException {
        Set<Long> contractIds = newContractMap.keySet();
        Map<Long, Contract> childContractsByExtend = contractSelector.getExtendContractByIds(contractIds);
        Map<Long, Contract> childContractsByBelongTo = contractSelector.getBelongsToContractByIds(contractIds);
        if (!childContractsByExtend.isEmpty()) {
            updateContractsByEvents(childContractsByExtend, new HashSet<>(Collections.singletonList("tCV_Amount__c")));
        }

        if (!childContractsByBelongTo.isEmpty()) {
            updateContractsByEvents(childContractsByBelongTo, new HashSet<>(Arrays.asList("should_Include_in_TCV_Reporting__c", "tCV_Amount__c")));
        }
    }

    private void updateContractsByEvents(Map<Long, Contract> mapContracts, Set<String> fields) {
//        for (Contract contract : mapContracts.values()) {
//            FieldUpdateEventService.buildAndSendFieldUpdateEvents(new ArrayList<Contract>(Collections.singletonList(contract)),fields);
//        }
        List<Contract> updateList = new ArrayList<>();

        for (Contract contract : mapContracts.values()){
            Contract temp = new Contract();
            temp.setId(contract.getId());
            for (String field : fields) {
                temp.setAttribute(field, contract.getAttribute(field));
            }
            updateList.add(temp);
        }

        NeoCrmUtils.update(updateList,"updateContractsByEvents");
    }

    private Boolean isNeedUpdateAllocationTCVByManualInsert(Contract_Opportunity_Allocation__c allocation) {
        return allocation.getShould_Include_in_Opportunity_Level_TCV__c() == null || !allocation.getShould_Include_in_Opportunity_Level_TCV__c();
    }

    public void updateAllocationsIncludeInTCVInAfterInsert(List<Contract_Opportunity_Allocation__c> allocations) throws ApiEntityServiceException {
        Set<Long> contractIds = new HashSet<>();
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            if (isNeedUpdateAllocationTCVByManualInsert(allocation)) {
                contractIds.add(allocation.getContract__c());
            }
        }
        if (contractIds.isEmpty()) {
            return;
        }
        Map<Long, Contract> contractMap = contractSelector.getContractByIds(contractIds);
        updateAllocationsIncludeInTCV(contractMap);
        updateAllocationsTCVAmount(contractMap);
        updateChildAllocationsForIncludeInTCVAndTCVAmount(contractMap);
    }

    public void setDefaultCurrencyFromContract(List<Contract_Opportunity_Allocation__c> allocations) throws NoSuchFieldException, IllegalAccessException, ApiEntityServiceException {
        Set<Long> contractIds = CollectionUtils.getIdSet(allocations, "contract__c");
        Map<Long, Contract> contractMap = contractSelector.getContractsById(contractIds);
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            allocation.setCurrencyIsoCode__c(contractMap.get(allocation.getContract__c()).getCurrencyIsoCode__c());
        }
    }

    public void validCurrencyToSameAsContract(List<Contract_Opportunity_Allocation__c> allocations, Map<Long, Contract_Opportunity_Allocation__c> allocationMap) throws ApiEntityServiceException {
        Set<Long> contractIds = new HashSet<>();
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            if (!Objects.equals(allocationMap.get(allocation.getId()).getCurrencyIsoCode__c(), allocation.getCurrencyIsoCode__c())) {
                contractIds.add(allocation.getContract__c());
            }
        }
        if (contractIds.isEmpty()) {
            return;
        }
        Map<Long, Contract> contractMap = contractSelector.getContractsById(contractIds);
        for (Contract_Opportunity_Allocation__c allocation : allocations) {
            Contract contract = contractMap.get(allocation.getContract__c());
            if (!Objects.equals(allocation.getCurrencyIsoCode__c(), contract.getCurrencyIsoCode__c())) {
                allocation.setCurrencyIsoCode__c(contract.getCurrencyIsoCode__c());
                // todo: need to fix the transaction for ContractController.handleContractValueSave which update allocation first
                //                allocation.CurrencyIsoCode.addError("The contract allocation currency must match the contract currency.");
            }
        }
    }

}