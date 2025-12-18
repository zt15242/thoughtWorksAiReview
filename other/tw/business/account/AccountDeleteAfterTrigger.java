package other.tw.business.account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.tw.business.api.Interfaces;

import java.util.*;

public class AccountDeleteAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("account after delete triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long, DataResult> resultMap = new HashMap<>();
        Map<Long, Account> deleteMap = new HashMap<>();

        for (XObject temp : dataList) {
            Account deletedAcc = (Account) temp;
            deleteMap.put(deletedAcc.getId(), deletedAcc);
            resultMap.put(deletedAcc.getId(), new DataResult(true, "", deletedAcc));
        }

        List<Account> deletedList= new ArrayList<>(deleteMap.values());

        AccountService accountService = new AccountService();

        try {
            accountService.resetParentIsParent(deletedList);
        } catch (Exception e) {
            LOGGER.error("account after delete Exception message =>" + e.getMessage());
            LOGGER.error("account after delete Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
