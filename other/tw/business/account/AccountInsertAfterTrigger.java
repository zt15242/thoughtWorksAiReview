package other.tw.business.account;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;

import java.util.*;

public class AccountInsertAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("account after insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Long, DataResult> resultMap = new HashMap<>();
        Map<Long, Account> newMap = new HashMap<>();

        for (XObject temp : dataList) {
            Account newAcc = (Account) temp;
            newMap.put(newAcc.getId(), newAcc);
            resultMap.put(newAcc.getId(), new DataResult(true, "", newAcc));
        }

        List<Account> newList= new ArrayList<>(newMap.values());


        AccountService accountService = new AccountService();

        try {
//           Account account = (Account) dataList.get(0);
//           LOGGER.error("getEntityType => " + account.getEntityType().toString());
//           if (account.getEntityType().toString().equals("-11010000100001")){
//                ReturnResult result = Interfaces.syncAccountJigSaw(account.getId().toString());
//                LOGGER.info("result =>" + JSONObject.toJSONString(result));
//           }

//            accountService.setRiskLevel(newList);
            accountService.resetParentIsParent(newList);
        } catch (Exception e) {
            LOGGER.error("account after insert Exception message =>" + e.getMessage());
            LOGGER.error("account after insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
