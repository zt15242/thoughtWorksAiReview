package other.tw.business.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.User;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.*;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class UserUpdateManagerBatchJob implements BatchJobPro<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final UserSelector userSelector = new UserSelector();

    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        LOGGER.error("UserUpdateLeaderBatchJob prepare is start!");

        List<String> data = new ArrayList<>();
        Map<String, JSONObject> openIdMap = new HashMap<>();

        try {
            openIdMap = userSelector.getOpenIdMap();

            data = new ArrayList<>(openIdMap.keySet());

            LOGGER.info("UserUpdateLeaderBatchJob prepare data ==>" + data);
        } catch (Exception e) {
            LOGGER.error("UserUpdateLeaderBatchJob Exception message =>" + e.getMessage());
            LOGGER.error("UserUpdateLeaderBatchJob Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();

        return batchJobDataBuilder
                .setBatchJobParam("openIdMap", JSONObject.toJSONString(openIdMap))
                .setDataList(data)
                .buildListData();
    }

    @Override
    public void execute(List<String> list, BatchJobParam batchJobParam) {
        LOGGER.info("UserUpdateLeaderBatchJob execute list ==> " + list.toString());
        LOGGER.info("UserUpdateLeaderBatchJob execute batchJobParam ==> " + batchJobParam);
        try {
            Map<String, JSONObject> openIdMap = (Map<String, JSONObject>) JSON.parse(batchJobParam.get("openIdMap"));

            todo(list, openIdMap);
        } catch (ApiEntityServiceException | BatchJobException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {
        LOGGER.error("UserUpdateLeaderBatchJob finish");
    }

    private void todo(List<String> list, Map<String, JSONObject> openIdMap) throws ApiEntityServiceException, BatchJobException {
        // 对数据做处理
        List<User> updateList = new ArrayList<>();

        for (String openId : list) {
            JSONObject userJson = openIdMap.get(openId);

            String old_leader_OpenId__c = userJson.getString("old_leader_OpenId__c");
            String leader_OpenId__c = userJson.getString("leader_OpenId__c");

            if (Objects.equals(old_leader_OpenId__c, leader_OpenId__c)) {
                LOGGER.error(openId+"'s leader did not change");
                continue;
            }

            User user = new User();
            user.setId(userJson.getLong("id"));

            if (leader_OpenId__c == null || leader_OpenId__c.isEmpty()) {
                LOGGER.error(openId+"has no leader");
                user.setManagerId(null);
                updateList.add(user);
            } else {
                if(openIdMap.containsKey(leader_OpenId__c)){
                    JSONObject managerJson = openIdMap.get(leader_OpenId__c);
                    LOGGER.error(openId+"'s leader info => " + managerJson);
                    if(managerJson!=null) {
                        user.setManagerId(managerJson.getLong("id"));
                        updateList.add(user);
                    }
                }else {
                    LOGGER.error(openId+"'s leader "+leader_OpenId__c+" does not exist");
                }

            }
        }

        LOGGER.error("更新用户直属上级更新数量 =>" + updateList.size());

        NeoCrmUtils.update(updateList,"更新用户直属上级");
    }

}