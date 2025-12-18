package other.tw.business.opportunity;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class OpportunitySelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public Map<Long, List<Opportunity>> getOppByAccId(List<Long> accId) throws ApiEntityServiceException {
        Map<Long, List<Opportunity>> accOppsMap = new HashMap<>();

        if (accId.isEmpty()) {
            LOGGER.error("accIds is empty");
            return accOppsMap;
        }

        String oppSql = "SELECT id,accountId FROM opportunity WHERE accountId IN (" + NeoCrmUtils.convertListToString(accId) + ")";
        LOGGER.error("getOppByAccId oppSql:" + oppSql);
        QueryResult<JSONObject> oppQuery = XoqlService.instance().query(oppSql, true, true);
        if (oppQuery.getSuccess()) {
            for (JSONObject record : oppQuery.getRecords()) {
                Opportunity opp = record.toJavaObject(Opportunity.class);

                if (!accOppsMap.containsKey(opp.getAccountId())) {
                    accOppsMap.put(opp.getAccountId(), new ArrayList<>());
                }

                accOppsMap.get(opp.getAccountId()).add(opp);
            }
        } else {
            LOGGER.error("getOppByAccId query error:" + oppQuery.getErrorMessage());
        }

        return accOppsMap;
    }

    public List<Opportunity> getAccountsById(Set<Long> oppIds) throws ApiEntityServiceException {
        List<Opportunity> oppList = new ArrayList<>();

        if (oppIds.isEmpty()) {
            LOGGER.error("oppIds is empty");
            return oppList;
        }

        String oppSql = "SELECT id,accountId FROM opportunity WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(oppIds)) + ")";
        LOGGER.error("getAccountsById oppSql:" + oppSql);
        QueryResult<JSONObject> oppQuery = XoqlService.instance().query(oppSql, true, true);
        if (oppQuery.getSuccess()) {
            for (JSONObject record : oppQuery.getRecords()) {
                Opportunity opp = record.toJavaObject(Opportunity.class);
                oppList.add(opp);
            }
        } else {
            LOGGER.error("getAccountsById query error:" + oppQuery.getErrorMessage());
        }

        return oppList;
    }

    public List<Opportunity> getOpportunityById(Set<Long> idSet) throws ApiEntityServiceException {
        if(idSet.isEmpty()) {
            LOGGER.error("idSet is empty");
            return new ArrayList<>();
        }

        String sql = "SELECT id, saleStageId FROM opportunity WHERE id IN ("+NeoCrmUtils.convertSetToString(idSet)+")";

        return NeoCrmUtils.query(sql,Opportunity.class);
    }
}
