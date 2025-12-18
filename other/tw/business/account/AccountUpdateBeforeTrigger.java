package other.tw.business.account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class AccountUpdateBeforeTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("account before update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Account,DataResult> resultMap = new HashMap<>();
        Map<Long, Account> newMap = new HashMap<>();
        Map<Long, Account> oldMap = new HashMap<>();
        List<JSONObject> oldAccJsonList = new ArrayList<>();

        for (XObject temp : dataList) {
            Account newAcc = (Account) temp;
            newMap.put(newAcc.getId(), newAcc);
            resultMap.put(newAcc,new DataResult(true,"",newAcc));
        }
        List<Account> newList= new ArrayList<>(newMap.values());

        AccountService accountService = new AccountService();

        TriggerContext triggerContext = new TriggerContext();

        try {
            oldAccJsonList = NeoCrmUtils.getObjectJsonList(new ArrayList<>(newMap.keySet()),"currently_is_a_focus_account__c,is_Parent__c,parentAccountId,settlementType__c,receivingCondition__c,invoiceType__c","account");
            triggerContext.set("oldAccJsonList",JSON.toJSONString(oldAccJsonList));
            oldMap = NeoCrmUtils.objectJsonListToIdMap(oldAccJsonList,Account.class);

            accountService.syncAccountFocusAndPeriodWhenUpdate(oldMap, newMap);
            accountService.checkClientAccountParent(newList,resultMap);
//            accountService.setRiskLevel(newList);
        } catch (Exception e) {
            LOGGER.error("account before update Exception message =>" + e.getMessage());
            LOGGER.error("account before update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()), triggerContext);
    }
}
