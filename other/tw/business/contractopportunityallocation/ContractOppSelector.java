package other.tw.business.contractopportunityallocation;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Contract_Opportunity_Allocation__c;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.Constants;
import other.tw.business.util.NeoCrmUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContractOppSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public List<Contract_Opportunity_Allocation__c> getOpportunityAllocatedTCVAmountByOppIds(Set<Long> oppIds) throws ApiEntityServiceException {
        List<Contract_Opportunity_Allocation__c> list = new ArrayList<>();

        if (oppIds.isEmpty()) {
            LOGGER.error("oppIds is empty");
            return list;
        }

        String sql = "SELECT" +
                " allocated_TCV_Amount_in_USD__c" +
                ", opportunity__c" +
                ", net_TCV_New__c" +
                ", net_TCV_in_USD__c" +
                ", currencyIsoCode__c" +
//                ", opportunity__c.currencyUnit" +
//                ", opportunity__c.stageName_F__c" +
//                ", contract__c.contract_Signing_ER__c" +
                " FROM contract_Opportunity_Allocation__c" +
                " WHERE opportunity__c IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(oppIds)) + ") AND should_Include_in_Opportunity_Level_TCV__c = 1";
        LOGGER.error("getOpportunityAllocatedTCVAmountByOppIds sql:" + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            for (JSONObject record : query.getRecords()) {
                list.add(record.toJavaObject(Contract_Opportunity_Allocation__c.class));
            }
        } else {
            LOGGER.error("getOpportunityAllocatedTCVAmountByOppIds query error:" + query.getErrorMessage());
        }

        return list;
    }

    public List<Contract_Opportunity_Allocation__c> getContractAllocationsByContractIds(Set<Long> contractIds) throws ApiEntityServiceException {
//        SELECT
//                Id,
//                Contract__c,
//        Contract__r.RecordTypeId,
//                Contract__r.Belong_To_MSA__c,
//                Should_Include_in_Opportunity_Level_TCV__c,
//                Contract__r.Belong_To_SOW__c,
//                Opportunity__c,
//                Expense__c,
//                Estimated_Discount__c,
//                Net_TCV_New__c,
//                Opportunity__r.CurrencyIsoCode,
//                Opportunity__r.Amount,
//                Allocated_Amount__c,
//                Allocated_TCV_Amount__c,
//                CurrencyIsoCode
//        FROM Contract_Opportunity_Allocation__c
//        WHERE Contract__c IN :contractIds
//        ORDER BY Opportunity__r.Name ASC


        List<Contract_Opportunity_Allocation__c> list = new ArrayList<>();

        if (contractIds.isEmpty()) {
            LOGGER.error("contractIds is empty");
            return list;
        }

        String sql = "SELECT"+
                " id"+
                ", contract__c"+
//                ", Contract__r.RecordTypeId"+
//                ", Contract__r.Belong_To_MSA__c"+
                ", should_Include_in_Opportunity_Level_TCV__c"+
//                ", Contract__r.Belong_To_SOW__c"+
                ", opportunity__c"+
                ", expense__c"+
                ", estimated_Discount__c"+
                ", net_TCV_New__c"+
//                ", Opportunity__r.CurrencyIsoCode"+
//                ", Opportunity__r.Amount"+
                ", allocated_Amount__c"+
                ", allocated_TCV_Amount__c"+
                ", currencyIsoCode__c"+
                " FROM contract_Opportunity_Allocation__c"+
                " WHERE contract__c IN ("+NeoCrmUtils.convertListToString(new ArrayList<>(contractIds))+")"+
                " ORDER BY opportunity__c.opportunityName ASC";

        LOGGER.error("getContractAllocationsByContractIds sql:" + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            for (JSONObject record : query.getRecords()) {
                list.add(record.toJavaObject(Contract_Opportunity_Allocation__c.class));
            }
        } else {
            LOGGER.error("getContractAllocationsByContractIds query error:" + query.getErrorMessage());
        }

        return list;
    }

    public List<Contract_Opportunity_Allocation__c> getOpportunityContractValueFieldsByOpportunityId(Set<Long> oppIdSet) throws ApiEntityServiceException {

        if (oppIdSet.isEmpty()){
            LOGGER.error("oppIdSet is empty");
            return new ArrayList<>();
        }

        String sql = "SELECT opportunity__c, allocated_Amount__c, estimated_Discount__c , expense__c" +
                " FROM contract_Opportunity_Allocation__c" +
                " WHERE should_Include_in_Opportunity_Level_TCV__c = 1" +
                " AND contract__c.approvalStatus = 3" +
                " AND contract__c.subtype__c = 1" +
                " AND ( contract__c.entityType != "+ Constants.CONTRACT_PO_ENTITY_TYPE_ID+" OR (contract__c.belong_To_SOW__c.subtype__c != 1 AND contract__c.belong_To_MSA__c.subtype__c != 1))" +
                " AND opportunity__c IN ("+NeoCrmUtils.convertSetToString(oppIdSet)+")";

        return NeoCrmUtils.query(sql,Contract_Opportunity_Allocation__c.class);
    }

    public List<Opportunity> getOpportunityByContractId(Set<Long> contractIdSet) throws ApiEntityServiceException {
        if(contractIdSet.isEmpty()) {
            LOGGER.error("contractIdSet is empty");
            return new ArrayList<>();
        }

        String sql = "SELECT opportunity__c id, opportunity__c.stageName_F__c stageName_F__c FROM contract_Opportunity_Allocation__c WHERE contract__c IN ("+NeoCrmUtils.convertSetToString(contractIdSet)+")";

        return NeoCrmUtils.query(sql,Opportunity.class);
    }
}
