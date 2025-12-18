package other.tw.business.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Department;
import com.rkhd.platform.sdk.data.model.User;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.*;
import other.tw.business.department.DepartmentSelector;
import other.tw.business.service.FeishuService;
import other.tw.business.util.NeoCrmUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UserSyncBatchJob implements BatchJobPro<SimpleMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private final DepartmentSelector departmentSelector = new DepartmentSelector();

    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        LOGGER.info("UserSyncBatchJob prepare is start!");

        int pageSize = Integer.parseInt(prepareParam.get("pageSize"));

        List<SimpleMap> data = new ArrayList<>();

        FeishuService feishuService = new FeishuService();

        try {
            List<Department> departmentList = departmentSelector.getAllDepartment();

            for (Department department : departmentList) {
                String pageToken = "";
                boolean hasMore = true;
//                int pageNo = 0;

                while (hasMore) {
//                    pageNo++;
                    LOGGER.error("data getUserByDepartment departCode => "+department.getDepartCode());
                    JSONObject resData = feishuService.getUserByDepartment(department.getDepartCode(), pageSize, pageToken);

                    if (resData == null) {
                        break;
                    }

                    hasMore = resData.getBoolean("has_more");
                    if (hasMore) {
                        pageToken = resData.getString("page_token");
                    }

                    SimpleMap simpleMap = new SimpleMap();
                    simpleMap.set("departCode", department.getDepartCode());
                    simpleMap.set("departmentId", department.getId().toString());
                    simpleMap.set("items", JSON.toJSONString(resData.getJSONArray("items")));

                    data.add(simpleMap);

//                    JSONArray items = resData.getJSONArray("items");
//
//                    LOGGER.error(department.getDepartCode()+" data pageNo =>"+pageNo);
//                    if(items==null){
//                        LOGGER.error(department.getDepartCode()+" data items is null");
//                    }else{
//                        LOGGER.error(department.getDepartCode()+" data items size => "+items.size());
//                        LOGGER.error(department.getDepartCode()+" data items => "+items);
//                    }
                }
            }

        } catch (ApiEntityServiceException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        try {
            LOGGER.error("UserSyncBatchJob prepare data size==>" + data.size());
            LOGGER.error("UserSyncBatchJob prepare data ==>" + data);
        } catch (Exception e) {
            LOGGER.error("UserSyncBatchJob Exception message =>" + e.getMessage());
            LOGGER.error("UserSyncBatchJob Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();

        return batchJobDataBuilder
                .setDataList(data)
                .buildListData();
    }

    @Override
    public void execute(List<SimpleMap> list, BatchJobParam batchJobParam) {
        LOGGER.info("UserSyncBatchJob execute list size ==> " + list.size());
        LOGGER.info("UserSyncBatchJob execute list ==> " + list.toString());
        LOGGER.info("UserSyncBatchJob execute batchJobParam ==> " + batchJobParam);
        try {
            todo(list);
        } catch (ApiEntityServiceException | BatchJobException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {
        LOGGER.info("UserSyncBatchJob finish");
    }

    private void todo(List<SimpleMap> list) throws ApiEntityServiceException, BatchJobException {
        // 对数据做处理
        List<User> userList = new ArrayList<>();

        for (SimpleMap simpleMap : list) {
//            JSONObject jsonObject = JSONObject.parseObject(jsonStr);
            String departCode = simpleMap.get("departCode");
            Long departmentId = Long.valueOf(simpleMap.get("departmentId"));
            JSONArray itemArray = JSONArray.parseArray(simpleMap.get("items"));

            if(itemArray==null){
                LOGGER.error(departCode+"部门下无人员");
                continue;
            }

//            LOGGER.error("simpleMap departCode => "+departCode);
//            LOGGER.error(departCode+" simpleMap itemArray size => "+itemArray.size());
//            LOGGER.error(departCode+" simpleMap itemArray => "+itemArray);

            for (Object o : itemArray) {
                JSONObject item = (JSONObject) o;

//                if(item.getJSONObject("orders").getBoolean("is_primary_dept"))

                JSONArray orderArray = item.getJSONArray("orders");

//                LOGGER.error("departCode => "+departCode);
                LOGGER.error(departCode+" item => "+item);

                boolean is_primary_dept = false;
                for (Object o2 : orderArray) {
                    JSONObject order = (JSONObject) o2;
                    if (order.getString("department_id").equals(departCode)) {
                        is_primary_dept = order.getBoolean("is_primary_dept");
                    }
                }

                LOGGER.error(departCode+" is_primary_dept => "+is_primary_dept);

                if (is_primary_dept) {
                    User user = new User();

                    String email = item.getString("email");
                    if(email!=null && !email.isEmpty()) {
                        user.setPersonalEmail(email);
                    }

                    String regex = "^1\\d{10}$";
                    String phone = item.getString("mobile").replace("+86", "");

                    if(Pattern.matches(regex, phone)){
                        user.setPhone(phone);
                    }

                    String en_name = item.getString("en_name");
                    user.setName(en_name == null || en_name.isEmpty() ? item.getString("name") : en_name);

                    user.setDimDepart(departmentId);

                    int gender = item.getInteger("gender");
                    if (gender == 1) {
                        user.setGender(1);
                    }else if (gender == 2) {
                        user.setGender(2);
                    }

                    user.setJoinAt(item.getLong("join_time"));
                    user.setStatus(item.getJSONObject("status").getBoolean("is_activated") ? 1 : 0);
                    user.setEmployeeCode(item.getString("employee_no"));
                    user.setOpenId__c(item.getString("open_id"));

                    String leader_user_id = item.getString("leader_user_id");
                    user.setLeader_OpenId__c(leader_user_id == null ? "" : leader_user_id);

//                    LOGGER.error("UserSyncBatchJob user ==> " + user);

                    if(!Pattern.matches(regex, phone)){
                        LOGGER.error("phone format error item ==>" + item);
                    }

                    if((user.getPersonalEmail()==null || user.getPersonalEmail().isEmpty() )&& (user.getPhone()==null || user.getPhone().isEmpty())){
                        LOGGER.error("no email and no phone item =>" + item);
                        LOGGER.error("no email and no phone user =>" + JSON.toJSONString(user));
                        continue;
                    }

                    userList.add(user);
                }
            }
        }

        List<String> changedFieldList = Arrays.asList("dimDepart", "status","openId__c", "leader_OpenId__c");

        NeoCrmUtils.upsertWithKeyFieldWhenFieldChanged(userList, User.class, true,"employeeCode", changedFieldList, 990641599905821L, "同步用户");
    }

}
