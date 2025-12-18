package other.tw.business.paymentApplicationPlan;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.tw.business.account.AccountService;

import java.util.*;

public class PaymentApplicationPlanInsAfterTrigger implements Trigger {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("PaymentApplicationPlanInsAfterTrigger triggerRequest =>" + JSON.toJSONString(triggerRequest));
        List<DataResult> result = new ArrayList<>();
        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        for(XObject xObject : dataList){

            result.add(new DataResult(true, "成功", xObject));
        }
        try {
//            accountService.setRiskLevel(newList);
        } catch (Exception e) {
            LOGGER.error("account after insert Exception message =>" + e.getMessage());
            LOGGER.error("account after insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>());
    }
}
