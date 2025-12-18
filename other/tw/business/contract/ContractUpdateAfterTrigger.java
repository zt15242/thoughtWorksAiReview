package other.tw.business.contract;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.trigger.*;
import other.tw.business.account.AccountService;
import other.tw.business.opportunity.OpportunityService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class ContractUpdateAfterTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.info("contract after update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.info("request log => " + dataList);

        Map<Contract, DataResult> resultMap = new HashMap<>();
        Map<Long, Contract> newMap = new HashMap<>();
        Map<Long, Contract> oldMap = new HashMap<>();

        for (XObject xObject : dataList) {
            Contract newContract = (Contract) xObject;
            newMap.put(newContract.getId(),newContract);
            resultMap.put(newContract,new DataResult(true, "", newContract));
        }

        List<Contract> newList= new ArrayList<>(newMap.values());

        AccountService accountService = new AccountService();

        TriggerContext triggerContext = new TriggerContext();

        try {
            triggerContext = triggerRequest.getTriggerContext();
            LOGGER.info("contract after update triggerContext =>" + triggerContext);
            List<JSONObject> oldContractJsonList = JSONObject.parseArray(triggerContext.get("oldContractJsonList"), JSONObject.class);
            oldMap = NeoCrmUtils.objectJsonListToIdMap(oldContractJsonList, Contract.class);

            accountService.updateAccountLevelTCV(newMap,oldMap);
        } catch (Exception e) {
            LOGGER.error("contract after update Exception message =>" + e.getMessage());
            LOGGER.error("contract after update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()));
    }
}
