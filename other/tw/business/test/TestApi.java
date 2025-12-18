package other.tw.business.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.AccountApproval__c;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Industry__c;
import com.rkhd.platform.sdk.data.model.User;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.http.HttpResult;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.bank.BankSyncScheduleJob;
import other.tw.business.service.ErpService;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import other.tw.business.user.UserSyncScheduleJob;
import other.tw.business.util.NeoCrmUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

@RestApi(baseUrl = "/test")
public class TestApi {
    public static final Logger LOGGER = LoggerFactory.getLogger();

    @RestMapping(value = "/deleteAllAccountApproval",method = RequestMethod.POST)
    public static void deleteAllAccountApproval() throws ApiEntityServiceException {
        String sql = "select id,name from accountApproval__c";

        List<AccountApproval__c> deleteList = new ArrayList<>();

        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            for(JSONObject jsonObject:query.getRecords()){
                LOGGER.error(jsonObject.toString());
                deleteList.add(jsonObject.toJavaObject(AccountApproval__c.class));
            }
        } else {
            LOGGER.error("accountApproval__c query error: " + query.getErrorMessage());
        }

        BatchOperateResult delResult = XObjectService.instance().delete(deleteList, true);

        if (delResult.getSuccess()) {
            LOGGER.error("accountApproval__c delete success");

        } else {
            for (OperateResult operateResult : delResult.getOperateResults()) {
                LOGGER.error("*** 删除accountApproval__c失败: "+operateResult.getErrorMessage());
            }
            LOGGER.error("accountApproval__c delete error:" + delResult.getErrorMessage());
        }
    }

    @RestMapping(value = "/testSyncBank",method = RequestMethod.POST)
    public static void testBankSync(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException, BatchJobException, ParseException, InterruptedException {
        LOGGER.info("testBankSync RestBeanParam ==="+jsonObject);
        JSONObject filter = jsonObject.getJSONObject("filter");

        int pageNo = jsonObject.getInteger("pageNo");
        int pageSize = jsonObject.getInteger("pageSize");

        BankSyncScheduleJob job = new BankSyncScheduleJob();
        job.syncBank(filter,pageNo,pageSize);
    }

    @RestMapping(value = "/testSendToErp",method = RequestMethod.POST)
    public static void testSendToErp(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException, InterruptedException, IOException {
        LOGGER.info("testSendToErp RestBeanParam ==="+jsonObject);

        List<Long> idList = jsonObject.getJSONArray("ids").toJavaList(Long.class);
        String obj = jsonObject.getString("object");

        Set<Long> idSet = new HashSet<>(idList);

        ErpService erpService = new ErpService();

        switch (obj) {
            case "account":
                erpService.sendAccountToErpById(idSet);
                break;
            case "opportunity":
                erpService.sendOpportunityToErpById(idSet);
                break;
            case "contract":
                erpService.sendContractToErpById(idSet);
                break;
        }
    }

    @RestMapping(value = "/testSendToErp2",method = RequestMethod.POST)
    public static void testSendToErp2(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException, InterruptedException, IOException {
        LOGGER.info("testSendToErp2 RestBeanParam ==="+jsonObject);

        List<String> idList = jsonObject.getJSONArray("ids").toJavaList(String.class);
        String obj = jsonObject.getString("object");

        ErpService erpService = new ErpService();

        switch (obj) {
            case "account":
                erpService.sendAccountToErpById2(idList);
                break;
            case "opportunity":
                erpService.sendOpportunityToErpById2(idList);
                break;
            case "contract":
                erpService.sendContractToErpById2(idList);
                break;
            case "industry":
                List<Industry__c> industryList = new ArrayList<>();
                for (String id : idList) {
                    Industry__c industry= new Industry__c();
                    industry.setId(Long.valueOf(id));
                    industryList.add(industry);
                }
                erpService.sendIndustryToErp2(industryList);
                break;
        }
    }

    @RestMapping(value = "/testFeishuGet",method = RequestMethod.POST)
    public static String testFeishuGet(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException {
//        String param = jsonObject.getString("param");
        String url = jsonObject.getString("url");

        String tokenUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

        JSONObject dataBody = new JSONObject();

        dataBody.put("app_id","cli_a9a4e84ad7ba9bb4");
        dataBody.put("app_secret","CFXlihnMiUFetbTJQfQCKhYVBbjm7WB1");

        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString(tokenUrl);
        commonData.setCall_type("POST");
        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        JSONObject resJson = JSONObject.parseObject(result.getResult());

        LOGGER.error("resJson: "+resJson.toJSONString());


        String tenantAccessToken= "Bearer "+resJson.getString("tenant_access_token");
        LOGGER.error("tenantAccessToken: "+tenantAccessToken);

//        String url2 = "https://open.feishu.cn/open-apis/contact/v3/users/find_by_department"+param;

//        dataBody = new JSONObject();
//
//        dataBody.put("department_id_type","department_id");
//        dataBody.put("department_id",department_id);

        commonData.setCallString(url);
        commonData.setCall_type("GET");
//        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        commonData.addHeader("Authorization", tenantAccessToken);
        LOGGER.error(JSON.toJSONString(commonData.getHeaders()));
        LOGGER.error(commonData.getBody());

        result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        return result.getResult();
    }


    @RestMapping(value = "/testFeishuPost",method = RequestMethod.POST)
    public static String testFeishuPost(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException {
        JSONObject dataBody = jsonObject.getJSONObject("body");
        String url = jsonObject.getString("url");

        String tokenUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

        JSONObject tokenBody = new JSONObject();

        tokenBody.put("app_id","cli_a9a4e84ad7ba9bb4");
        tokenBody.put("app_secret","CFXlihnMiUFetbTJQfQCKhYVBbjm7WB1");

        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString(tokenUrl);
        commonData.setCall_type("POST");
        commonData.setBody(tokenBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        JSONObject resJson = JSONObject.parseObject(result.getResult());

        LOGGER.error("resJson: "+resJson.toJSONString());


        String tenantAccessToken= "Bearer "+resJson.getString("tenant_access_token");
        LOGGER.error("tenantAccessToken: "+tenantAccessToken);

//        dataBody = new JSONObject();
//
//        dataBody.put("department_id_type","department_id");
//        dataBody.put("department_id",department_id);

        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        commonData.addHeader("Authorization", tenantAccessToken);
        LOGGER.error(JSON.toJSONString(commonData.getHeaders()));
        LOGGER.error(commonData.getBody());

        result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        return result.getResult();
    }


    @RestMapping(value = "/testSyncUser",method = RequestMethod.POST)
    public static void testSyncUser(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException, BatchJobException, ParseException, InterruptedException {
        LOGGER.info("testSyncUser RestBeanParam ==="+jsonObject);

        int pageSize = jsonObject.getInteger("pageSize");

        UserSyncScheduleJob job = new UserSyncScheduleJob();

        job.syncUser(pageSize);
    }

    @RestMapping(value = "/deleteUser",method = RequestMethod.POST)
    public static String deleteUser(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException {
        String sql = jsonObject.getString("sql");

        List<XObject> deleteList = new ArrayList<>();

        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            for(JSONObject record:query.getRecords()){
                LOGGER.error(record.toString());
                User user= record.toJavaObject(User.class);
                deleteList.add(user);
            }
        } else {
            LOGGER.error("TestApi delete query error: " + query.getErrorMessage());
            return "TestApi delete query error: " + query.getErrorMessage();
        }

        BatchOperateResult delResult = XObjectService.instance().delete(deleteList, true);

        String msg ="";
        if (delResult.getSuccess()) {
            LOGGER.error("TestApi delete success");
            for (OperateResult operateResult : delResult.getOperateResults()) {
                LOGGER.error("*** TestApi删除失败: "+operateResult.getErrorMessage());
                msg+=operateResult.getErrorMessage();
            }
        } else {
            LOGGER.error("TestApi delete error:" + delResult.getErrorMessage());
            msg+=delResult.getErrorMessage();
        }

        return msg;
    }

    @RestMapping(value = "/upsertUser",method = RequestMethod.POST)
    public static String upsertUser(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException {
        String updateListJson = jsonObject.getString("updateListJson");
        String insertListJson = jsonObject.getString("insertListJson");

        String msg="";

        if(!(updateListJson==null|| updateListJson.isEmpty())){
            List<User> updateList = JSONArray.parseArray(updateListJson, User.class);

            msg+=NeoCrmUtils.update(updateList,"TestApi upsertUser");
        }

        if(!(insertListJson==null|| insertListJson.isEmpty())){
            List<User> insertList = JSONArray.parseArray(insertListJson, User.class);

            msg+=NeoCrmUtils.update(insertList,"TestApi upsertUser");
        }

        return msg;
    }

    @RestMapping(value = "/updateUsersManager",method = RequestMethod.POST)
    public static String updateUsersManager(@RestBeanParam(name = "json") JSONObject jsonObject) throws ApiEntityServiceException, BatchJobException {
        new UserSyncScheduleJob().updateUsersManager();

        return "updateUsersManager done";
    }

}
