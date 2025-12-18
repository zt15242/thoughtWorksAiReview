package other.tw.business.contract;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.DataResult;
import com.rkhd.platform.sdk.trigger.Trigger;
import com.rkhd.platform.sdk.trigger.TriggerRequest;
import com.rkhd.platform.sdk.trigger.TriggerResponse;
import other.tw.business.account.AccountService;
import other.tw.business.opportunity.OpportunityService;

import java.util.*;

public class ContractInsertAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
//        LOGGER.error("contract after insert triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

        Map<Contract, DataResult> resultMap = new HashMap<>();
        Map<Long, Contract> newMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Contract newContract = (Contract) xObject;
            newMap.put(newContract.getId(),newContract);
            resultMap.put(newContract,new DataResult(true, "", newContract));
        }

        List<Contract> newList= new ArrayList<>(newMap.values());

        AccountService accountService = new AccountService();

        try{
            accountService.updateAccountLevelTCV(newList);
        } catch (Exception e) {
            LOGGER.error("contract after insert Exception message =>" + e.getMessage());
            LOGGER.error("contract after insert Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
