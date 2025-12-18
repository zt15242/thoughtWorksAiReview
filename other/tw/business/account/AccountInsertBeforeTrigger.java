package other.tw.business.account;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;

import java.util.*;

public class AccountInsertBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest)  {
        LOGGER.error("account before insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Account, DataResult> resultMap = new HashMap<>();
        List<Account> newList= new ArrayList<>();

        for (XObject temp : dataList) {
            Account newAcc = (Account) temp;
            newList.add(newAcc);
            resultMap.put(newAcc,new DataResult(true,"",newAcc));
        }

        AccountService accountService = new AccountService();

        try {
            accountService.checkClientAccountParent(newList,resultMap);
//            accountService.setRiskLevel(newList);
        } catch (Exception e) {
            LOGGER.error("account before insert Exception message =>" + e.getMessage());
            LOGGER.error("account before insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
