package other.tw.business.accountapprovalnew;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;

public class AccountApprovalNewSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger();
    
    public Long getAccountIdByAccountApprovalId(Long id) throws ApiEntityServiceException {
        Long accId = null;

//        if (ids.isEmpty()) {
//            LOGGER.error("ids is empty");
//            return  accList;
//        }

        String accApprovalSql = "SELECT Account__c FROM accountApprovalNew__c WHERE id = " + id;
        LOGGER.error("getAccountByAccountApprovalIds accApprovalSql:" + accApprovalSql);
        QueryResult<JSONObject> accApprovalQuery = XoqlService.instance().query(accApprovalSql, true, true);
        if (accApprovalQuery.getSuccess()) {
            for (JSONObject record : accApprovalQuery.getRecords()) {
                accId = record.getLong("Account__c");
            }
        } else {
            LOGGER.error("getAccountByAccountApprovalIds query error:" + accApprovalQuery.getErrorMessage());
        }

        return accId;
    }
}
