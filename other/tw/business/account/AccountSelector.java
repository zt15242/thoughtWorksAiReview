package other.tw.business.account;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class AccountSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    public Map<Long, List<Account>> getChildAccountMap(ArrayList<Long> accIds) throws ApiEntityServiceException {
        Map<Long, List<Account>> childMap = new HashMap<>();

        if (accIds.isEmpty()) {
            LOGGER.error("accIds is empty");
            return  childMap;
        }
        
        String accSql = "SELECT id,parentAccountId FROM account WHERE parentAccountId IN (" + NeoCrmUtils.convertListToString(accIds) + ")";
        LOGGER.error("child accSql:" + accSql);
        QueryResult<JSONObject> accQuery = XoqlService.instance().query(accSql, true, true);
        if (accQuery.getSuccess()) {
            for (JSONObject record : accQuery.getRecords()) {
                Account acc =record.toJavaObject(Account.class);
                if (!childMap.containsKey(acc.getParentAccountId())) {
                    childMap.put(acc.getParentAccountId(), new ArrayList<>());
                }
                childMap.get(acc.getParentAccountId()).add(acc);
            }
        } else {
            LOGGER.error("child account query error:" + accQuery.getErrorMessage());
        }
        
        return childMap;
    }

    public Map<Long, Account> getAccountMapById(List<Long> accIds) throws ApiEntityServiceException {
        Map<Long, Account> accMap = new HashMap<>();

        if (accIds.isEmpty()) {
            LOGGER.error("accIds is empty");
            return  accMap;
        }

        String accSql = "SELECT id,currently_is_a_focus_account__c FROM account WHERE id IN (" + NeoCrmUtils.convertListToString(accIds) + ")";
        LOGGER.error("getAccountMapById accSql:" + accSql);
        QueryResult<JSONObject> accQuery = XoqlService.instance().query(accSql, true, true);
        if (accQuery.getSuccess()) {
            for (JSONObject record : accQuery.getRecords()) {
                Account acc = new Account();
                acc.setId(record.getLong("id"));
                acc.setCurrently_is_a_focus_account__c(record.getBoolean("currently_is_a_focus_account__c"));

                accMap.put(acc.getId(), acc);
            }
        } else {
            LOGGER.error("getAccountMapById query error:" + accQuery.getErrorMessage());
        }

        return accMap;
    }

    public List<Account> getAccountApprovalStatus(Set<Long> accIds) throws ApiEntityServiceException {
        if (accIds.isEmpty()) {
            LOGGER.error("accIds is empty");
            return new ArrayList<>();
        }

        String sql = "SELECT id, approval_status__c FROM account WHERE id IN ("+NeoCrmUtils.convertSetToString(accIds)+")";

        return NeoCrmUtils.query(sql,Account.class,"getAccountApprovalStatus");
    }
}
