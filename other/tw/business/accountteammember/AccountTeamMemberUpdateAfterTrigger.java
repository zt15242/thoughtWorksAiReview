package other.tw.business.accountteammember;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account_Team_Member__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.api.Interfaces;

import java.util.*;

public class AccountTeamMemberUpdateAfterTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("AccountTeamMember before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));
        List<DataResult> result = new ArrayList<>();
        List<XObject> dataList = triggerRequest.getDataList();
        TriggerContext triggerContext = triggerRequest.getTriggerContext();
        LOGGER.error("request log => " + dataList);
        JSONArray jsonArray = null;
        try {
            jsonArray = JSONArray.parseArray(triggerContext.get("oldList"));
        } catch (TriggerContextException e) {
            throw new RuntimeException(e);
        }
        List<Account_Team_Member__c> oldList = jsonArray.toJavaList(Account_Team_Member__c.class);
        Map<Long, Account_Team_Member__c> oldMap = new HashMap<>();
        for(Account_Team_Member__c old :oldList){
            LOGGER.info("old=" + old);
            oldMap.put(old.getId(),old);
        }

        // Create a set to store unique account IDs
        Set<String> accountIds = new HashSet<>();
        for(XObject xObject : dataList) {
            accountIds.add(oldMap.get(xObject.getId()).getAccount__c().toString());
        }

        // Query all accounts at once
        String contractSql = "SELECT id,entityType,client_Code__c FROM account WHERE id in (" +
                String.join(",", accountIds) + ")";
        LOGGER.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = null;
        try {
            contractQuery = XoqlService.instance().query(contractSql, true, true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }

        // Create a map of accounts for quick lookup
        Map<Long, JSONObject> accountMap = new HashMap<>();
        for(JSONObject account : contractQuery.getRecords()) {
            accountMap.put(account.getLong("id"), account);
        }

        // Process each account team member
        for(XObject xObject: dataList) {
            Account_Team_Member__c oldATM = oldMap.get(xObject.getId());
            JSONObject account = accountMap.get(Long.parseLong(oldATM.getAccount__c().toString()));

            if ((oldATM.getTeamMemberRole__c() != null && !oldATM.getTeamMemberRole__c().equals(((Account_Team_Member__c)xObject).getTeamMemberRole__c())) ||
                    (oldATM.getRead_And_Write_Permissions__c() != null && !oldATM.getRead_And_Write_Permissions__c().equals(((Account_Team_Member__c)xObject).getRead_And_Write_Permissions__c()))) {
                if (account.getString("entityType").equals("-11010000100001")&&(account.getString("client_Code__c")!=null|| !Objects.equals(account.getString("client_Code__c"), ""))) {
                    Interfaces.ReturnResult syncresult = Interfaces.syncAccountJigSaw(account.getString("id"));
                }
            }
            result.add(new DataResult(true, "成功", xObject));
        }

        return new TriggerResponse(true, "",result);
    }
}
