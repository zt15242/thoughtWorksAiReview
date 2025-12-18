package other.tw.business.accountteammember;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account_Team_Member__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.tw.business.api.Interfaces;

import java.util.*;

public class AccountTeamMemberInsertAddTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("AccountTeamMember after insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Account_Team_Member__c, DataResult> resultMap = new HashMap<>();
        List<Account_Team_Member__c> newList = new ArrayList<>();
        Set<String> accountIds = new HashSet<>();

        for (XObject temp : dataList) {
            Account_Team_Member__c newTeamMember = (Account_Team_Member__c) temp;
            newList.add(newTeamMember);
            resultMap.put(newTeamMember, new DataResult(true, "", newTeamMember));
            if (newTeamMember.getAccount__c() != null) {
                accountIds.add(newTeamMember.getAccount__c().toString());
            }
        }

        if (!accountIds.isEmpty()) {
            String contractSql = "SELECT id,entityType,client_Code__c FROM account WHERE id in (" + String.join(",", accountIds) + ")";
            LOGGER.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
            QueryResult<JSONObject> contractQuery;
            try {
                contractQuery = XoqlService.instance().query(contractSql, true, true);
            } catch (ApiEntityServiceException e) {
                throw new RuntimeException(e);
            }

            Map<Long, JSONObject> accountMap = new HashMap<>();
            for (JSONObject account : contractQuery.getRecords()) {
                accountMap.put(account.getLong("id"), account);
            }

            for (Account_Team_Member__c atm : newList) {
                if (atm.getAccount__c() == null) continue;
                JSONObject account = accountMap.get(Long.parseLong(atm.getAccount__c().toString()));
                if (account == null) continue;
                if (account.getString("entityType").equals("-11010000100001") && (account.getString("client_Code__c") != null || !Objects.equals(account.getString("client_Code__c"), ""))) {
                    Interfaces.ReturnResult syncresult = Interfaces.syncAccountJigSaw(account.getString("id"));
                }
            }
        }
        

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
