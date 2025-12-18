package other.tw.business.accountteammember;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account_Team_Member__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.TriggerContextException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class AccountTeamMemberUpdateBeforeTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("AccountTeamMember before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));
        List<DataResult> result = new ArrayList<>();
        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);
        for (XObject xObject : dataList) {
            result.add(new DataResult(true, "", xObject));
        }
        TriggerContext triggerContext = new TriggerContext();
        String consql = "Select id,Account__c,teamMemberRole__c,read_And_Write_Permissions__c from Account_Team_Member__c  where id ="+dataList.get(0).getId();
        List<Account_Team_Member__c> conIds = null;
        try {
            conIds = NeoCrmUtils.executeQuery(consql, Account_Team_Member__c.class);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }

        List<XObject> contextList = new ArrayList<>(conIds);
        LOGGER.info("contextList==" + JSON.toJSONString(contextList));
        try {
            triggerContext.set("oldList", JSON.toJSONString(contextList));
        } catch (TriggerContextException e) {
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "",result, triggerContext);
    }
}
