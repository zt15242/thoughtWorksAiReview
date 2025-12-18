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
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class ContractUpdateBeforeTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public TriggerResponse execute(TriggerRequest triggerRequest) {
        LOGGER.error("contract before update triggerRequest =>" + JSON.toJSONString(triggerRequest));

        List<XObject> dataList = triggerRequest.getDataList();
        LOGGER.error("request log => " + dataList);

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

        List<JSONObject> oldContractJsonList = new ArrayList<>();

        TriggerContext triggerContext = new TriggerContext();

        try{
            oldContractJsonList = NeoCrmUtils.getObjectJsonList(new ArrayList<>(newMap.keySet()),"should_include_in_tcv_reporting__c, net_TCV_New__c","contract");
            triggerContext.set("oldContractJsonList",JSON.toJSONString(oldContractJsonList));

        } catch (Exception e) {
            LOGGER.error("contract before update Exception message =>" + e.getMessage());
            LOGGER.error("contract before update Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        return new TriggerResponse(true, "", new ArrayList<>(resultMap.values()),triggerContext);
    }
}
