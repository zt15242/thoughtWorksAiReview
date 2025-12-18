package other.tw.business.contract;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class ContractSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public Map<Long,Long> getAccountContractDateByOppId(List<Long> oppIds) throws ApiEntityServiceException {
        Map<Long,Long> oppIdToContractDateMap = new HashMap<>();

        if (oppIds.isEmpty()){
            LOGGER.error("oppIds is empty");
            return oppIdToContractDateMap;
        }

        String contractSql = "SELECT opportunityId, max(signDate) FROM contract WHERE opportunityId IN (" + NeoCrmUtils.convertListToString(oppIds) + ") GROUP BY opportunityId";
        LOGGER.info("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                oppIdToContractDateMap.put(record.getLong("opportunityId"),record.getLong("max(signDate)"));
            }
        } else {
            LOGGER.error("getOpportunityContractDate query error:" + contractQuery.getErrorMessage());
        }

        return oppIdToContractDateMap;
    }

    public Map<Long, Contract> getContractsById(Set<Long> contractIds) throws ApiEntityServiceException {
//        return [
//        SELECT
//                Id,
//                Name,
//                Contract_Status__c,
//                AccountId,
//        RecordType.Name,
//                Belong_To_MSA__c,
//                Belong_To_SOW__c,
//                OwnerId,
//                CurrencyIsoCode,
//                Contract_Signing_ER__c,
//                Amount__c,
//                Expense__c,
//                Estimated_Discount__c,
//                Subtype__c,
//                Is_Linked_One_Opportunity__c
//        FROM Contract
//        WHERE Id IN :ids
//        ];

        Map<Long,Contract> contractMap = new HashMap<>();

        if (contractIds.isEmpty()){
            LOGGER.error("contractIds is empty");
            return contractMap;
        }

        String contractSql = "SELECT" +
                " id" +
                ", name__c" +
                ", contract_Status__c" +
                ", accountId" +
                ", entityType" +
                ", belong_To_MSA__c" +
                ", belong_To_SOW__c" +
                ", ownerId" +
                ", currencyIsoCode__c" +
                ", contract_Signing_ER__c" +
                ", amount" +
                ", expense__c" +
                ", estimated_Discount__c" +
                ", subtype__c" +
                ", is_Linked_One_Opportunity__c" +
                " FROM contract WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")";
        LOGGER.info("getContractsById contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                contractMap.put(record.getLong("id"),record.toJavaObject(Contract.class));
            }
        } else {
            LOGGER.error("getContractsById query error:" + contractQuery.getErrorMessage());
        }

        return contractMap;
    }

    public List<Contract> getContractsByContractId(Set<Long> contractIds) throws ApiEntityServiceException {
//        return [SELECT Id, AccountId, Account.Name FROM Contract WHERE Id IN :contractId];

        List<Contract> contractList = new ArrayList<>();

        if (contractIds.isEmpty()) {
            LOGGER.error("contractIds is empty");
            return contractList;
        }

        String contractSql = "SELECT id,accountId FROM contract WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")";
        LOGGER.info("getContractsByContractId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                Contract contract = record.toJavaObject(Contract.class);
                contractList.add(contract);
            }
        } else {
            LOGGER.error("getContractsByContractId query error:" + contractQuery.getErrorMessage());
        }

        return contractList;
    }


//    public Map<Id, Contract> getExtendContractByIds(Set<Id> contractIds) {
//                [
//                SELECT
//        Id,
//                RecordTypeId,
//                Belong_To_MSA__c,
//                Belong_To_SOW__c,
//                Subtype__c,
//                Contract_Status__c,
//                Should_Include_in_TCV_Reporting__c,
//                Amount__c,
//                Expense__c,
//                Net_TCV_New__c,
//                X24mCV_New__c,
//                Estimated_Discount__c,
//                StartDate,
//                EndDate,
//                Is_Amendatory__c,
//                Amendatory_Type__c,
//                TCV_Amount__c,
//                Extend_From__c
//        FROM Contract
//        WHERE Extend_From__c IN :contractIds
//            ]
    public Map<Long, Contract> getExtendContractByIds(Set<Long> contractIds) throws ApiEntityServiceException {
        Map<Long,Contract> contractMap = new HashMap<>();

        if (contractIds.isEmpty()){
            LOGGER.error("contractIds is empty");
            return contractMap;
        }

        String contractSql ="SELECT" +
                " id" +
                ", entityType" +
                ", belong_To_MSA__c" +
                ", belong_To_SOW__c" +
                ", subtype__c" +
                ", contract_Status__c" +
                ", should_Include_in_TCV_Reporting__c" +
                ", amount" +
                ", expense__c" +
                ", net_TCV_New__c" +
                ", x24mCV_New__c" +
                ", estimated_Discount__c" +
                ", startDate" +
                ", endDate" +
                ", is_Amendatory__c" +
                ", amendatory_Type__c" +
                ", tCV_Amount__c" +
                ", extend_From__c" +
                " FROM contract WHERE extend_From__c IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")";

        LOGGER.info("getExtendContractByIds contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                contractMap.put(record.getLong("id"),record.toJavaObject(Contract.class));
            }
        } else {
            LOGGER.error("getExtendContractByIds query error:" + contractQuery.getErrorMessage());
        }

        return contractMap;
    }

//    public Map<Id, Contract> getBelongsToContractByIds(Set<Id> contractIds) {
//        return new Map<Id, Contract>(
//                [
//                SELECT
//        Id,
//                Name,
//                RecordTypeId,
//                Belong_To_MSA__c,
//                Belong_To_SOW__c,
//                Subtype__c,
//                Contract_Status__c,
//                Should_Include_in_TCV_Reporting__c,
//                Amount__c,
//                Is_Amendatory__c,
//                Amendatory_Type__c,
//                TCV_Amount__c,
//                Expense__c,
//                Net_TCV_New__c,
//                X24mCV_New__c,
//                StartDate,
//                EndDate,
//                Estimated_Discount__c,
//                Extend_From__c
//        FROM Contract
//        WHERE Belong_To_MSA__c IN :contractIds OR Belong_To_SOW__c IN :contractIds
//            ]
    public Map<Long, Contract> getBelongsToContractByIds(Set<Long> contractIds) throws ApiEntityServiceException {
        Map<Long,Contract> contractMap = new HashMap<>();

        if (contractIds.isEmpty()){
            LOGGER.error("contractIds is empty");
            return contractMap;
        }

        String contractSql ="SELECT" +
                " id" +
                ", Name" +
                ", entityType" +
                ", belong_To_MSA__c" +
                ", belong_To_SOW__c" +
                ", subtype__c" +
                ", contract_Status__c" +
                ", should_Include_in_TCV_Reporting__c" +
                ", amount" +
                ", is_Amendatory__c" +
                ", amendatory_Type__c" +
                ", tCV_Amount__c" +
                ", expense__c" +
                ", net_TCV_New__c" +
                ", x24mCV_New__c" +
                ", startDate" +
                ", endDate" +
                ", estimated_Discount__c" +
                ", extend_From__c" +
                " FROM contract" +
                " WHERE belong_To_MSA__c IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")" +
                " OR belong_To_SOW__c IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")";
                
        LOGGER.info("getBelongsToContractByIds contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                contractMap.put(record.getLong("id"),record.toJavaObject(Contract.class));
            }
        } else {
            LOGGER.error("getBelongsToContractByIds query error:" + contractQuery.getErrorMessage());
        }

        return contractMap;
    }

    public Map<Long, Contract> getContractByIds(Set<Long> contractIds) throws ApiEntityServiceException {
//        return new Map<Id, Contract>(
//                [
//                SELECT
//        Id,
//                Subtype__c,
//                Contract_Status__c,
//                Amount__c,
//                RecordTypeId,
//                Belong_To_MSA__c,
//                Belong_To_SOW__c,
//                Is_Amendatory__c,
//                Amendatory_Type__c,
//                Extend_From__c,
//                Expense__c,
//                Net_TCV_New__c,
//                X24mCV_New__c,
//                Estimated_Discount__c,
//                StartDate,
//                EndDate,
//                TCV_Amount__c,
//                Should_Include_in_TCV_Reporting__c
//        FROM Contract
//        WHERE Id IN :relatedContractIds
//            ]
//        );

        Map<Long,Contract> contractMap = new HashMap<>();

        if (contractIds.isEmpty()){
            LOGGER.error("contractIds is empty");
            return contractMap;
        }

        String contractSql = "SELECT id, subtype__c, contract_Status__c, amount, entityType, belong_To_MSA__c, belong_To_SOW__c, is_Amendatory__c, amendatory_Type__c, extend_From__c, expense__c, net_TCV_New__c, x24mCV_New__c, estimated_Discount__c, startDate, endDate, tCV_Amount__c, should_Include_in_TCV_Reporting__c  FROM contract WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(contractIds)) + ")";
        LOGGER.info("getContractByIds contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {
                contractMap.put(record.getLong("id"),record.toJavaObject(Contract.class));
            }
        } else {
            LOGGER.error("getContractByIds query error:" + contractQuery.getErrorMessage());
        }
        return contractMap;
    }


    public List<Contract> getAccountTCVAmountByAccountIds(Set<Long> accountIdSet) throws ApiEntityServiceException {
        String contractSql = "SELECT tCV_Amount_in_USD__c, net_TCV_in_USD__c, accountId, contract_Signing_ER__c" +
                " FROM contract" +
                " WHERE accountId IN ("+NeoCrmUtils.convertListToString(new ArrayList<>(accountIdSet))+") AND should_Include_in_TCV_Reporting__c = 1";
        LOGGER.info("getAccountTCVAmountByAccountIds contractSql:" + contractSql);
        return NeoCrmUtils.query(contractSql,Contract.class);
    }
}
