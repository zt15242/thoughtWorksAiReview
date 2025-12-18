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

public class AccountUpdateAfterTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest)  {
        LOGGER.error("account after update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long,DataResult> resultMap = new HashMap<>();
        Map<Long, Account> newMap = new HashMap<>();
        Map<Long, Account> oldMap = new HashMap<>();
        List<JSONObject> oldAccJsonList = new ArrayList<>();


        for (XObject temp : dataList) {
            Account newAcc = (Account) temp;
            newMap.put(newAcc.getId(), newAcc);
            resultMap.put(newAcc.getId(),new DataResult(true,"",newAcc));
        }

        List<Account> newList= new ArrayList<>(newMap.values());

        AccountService accountService = new AccountService();

        TriggerContext triggerContext = new TriggerContext();

        try {
            triggerContext = triggerRequest.getTriggerContext();
            LOGGER.error("account after update triggerContext =>" + triggerContext);
            oldAccJsonList = JSONObject.parseArray(triggerContext.get("oldAccJsonList"), JSONObject.class);
            oldMap = NeoCrmUtils.objectJsonListToIdMap(oldAccJsonList, Account.class);

            accountService.resetParentIsParent(oldMap,newMap);
            accountService.sendDataToFocusAccountProcessQueue(oldMap, newMap);
            accountService.sendToErp(oldMap,newMap);

//            Account account = (Account) dataList.get(0);
//            LOGGER.info("getEntityType => " + account.getEntityType().toString());
//            if (account.getEntityType().toString().equals("-11010000100001")){
////                    ReturnResult result = Interfaces.syncAccountJigSaw(account.getId().toString());
////                    LOGGER.info("result =>" + JSONObject.toJSONString(result));
//            }
        } catch (Exception e) {
            LOGGER.error("account after update Exception message =>" + e.getMessage());
            LOGGER.error("account after update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }


        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
