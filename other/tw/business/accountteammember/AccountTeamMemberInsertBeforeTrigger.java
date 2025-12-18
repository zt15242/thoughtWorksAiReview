package other.tw.business.accountteammember;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account_Team_Member__c;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;

import java.util.*;

public class AccountTeamMemberInsertBeforeTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("AccountTeamMember before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Account_Team_Member__c, DataResult> resultMap = new HashMap<>();
        List<Account_Team_Member__c> newList = new ArrayList<>();

        for (XObject temp : dataList) {
            Account_Team_Member__c newTeamMember = (Account_Team_Member__c) temp;
            newList.add(newTeamMember);
            resultMap.put(newTeamMember, new DataResult(true, "", newTeamMember));
        }

        AccountTeamMemberService teamMemberService = new AccountTeamMemberService();

        try {
            teamMemberService.createTeamMember(newList, resultMap);
        } catch (Exception e) {
            LOGGER.error("AccountTeamMember before insert Exception message =>" + e.getMessage());
            LOGGER.error("AccountTeamMember before insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}