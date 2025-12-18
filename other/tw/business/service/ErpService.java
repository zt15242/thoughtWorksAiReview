package other.tw.business.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Bank__c;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Industry__c;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.HttpResult;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.task.param.SimpleMap;
import other.tw.business.ResponseEntity;
import other.tw.business.util.CommonInterfaceUtil;
import other.tw.business.util.NeoCrmUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ErpService {
    public static final Logger LOGGER = LoggerFactory.getLogger();
    private static String ACCESS_TOKEN = "";
    private static Date EXPIRE_TIME = new Date();

    public String getAppToken() {
        // 基础配置
        Map<String, Object> token = new HashMap<>();
        token.put("appId", "crm");
        token.put("appSecret", "ywxtAdmin-n79001");
        token.put("tenantid", "inspire.test");
        token.put("accountId", "2337138593526564864");
        JSONObject body = new JSONObject(token);
        // 调用接口获取token
        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString("https://inspire.test.kdcloud.com/api/getAppToken.do");
        commonData.setCall_type("POST");
        commonData.setBody(body.toString());
        LOGGER.error(commonData.getBody());
        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.info("返回值1===" + JSON.toJSONString(result));

        JSONObject resJson = JSONObject.parseObject(result.getResult());
        String appToken = JSON.toJSONString(resJson.getJSONObject("data").getString("app_token")).replace("\"", "");
        LOGGER.info("返回值2===" + result.getResult());
//        LOGGER.info("返回值3===" + resJson);
        LOGGER.error("app_token===" + appToken);

        EXPIRE_TIME = new Date(resJson.getJSONObject("data").getLong("expire_time"));

        return appToken;
    }

    public String updateAccessToken() {
        // 基础配置
        Map<String, Object> token = new HashMap<>();
        token.put("user", "yw");
        token.put("usertype", "UserName");
        token.put("apptoken", getAppToken());
        token.put("tenantid", "inspire.test");
        token.put("accountId", "2337138593526564864");
        token.put("language", "zh_CN");
        JSONObject body = new JSONObject(token);
        // 调用接口获取token
        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString("https://inspire.test.kdcloud.com/api/login.do");
        commonData.setCall_type("POST");
        commonData.setBody(body.toString());
        LOGGER.error(commonData.getBody());
        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.info("返回值1===" + JSON.toJSONString(result));

        JSONObject resJson = JSONObject.parseObject(result.getResult());
        ACCESS_TOKEN = JSON.toJSONString(resJson.getJSONObject("data").getString("access_token")).replace("\"", "");


        LOGGER.info("返回值2===" + result.getResult());
//        LOGGER.info("返回值3===" + resJson);
        LOGGER.error("access_token===" + ACCESS_TOKEN);

        return ACCESS_TOKEN;
    }

    private String getAccessToken() {
        Date now = new Date();
        if (ACCESS_TOKEN.isEmpty() || !now.before(EXPIRE_TIME)) {
            updateAccessToken();
        }

        return ACCESS_TOKEN;
    }

    public String sendToErp(String url, List<JSONObject> jsonObjectList) throws InterruptedException {
        String msg ="";
        
        Date now = new Date();
        if (ACCESS_TOKEN.isEmpty() || !now.before(EXPIRE_TIME)) {
            updateAccessToken();
        }
        // 基础配置
        JSONObject dataBody = new JSONObject();
        dataBody.put("data", jsonObjectList);

        // 调用接口获取token
        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json");
        commonData.addHeader("accesstoken", ACCESS_TOKEN);
        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.info("返回值1===" + JSON.toJSONString(result));

        JSONObject resJson = JSONObject.parseObject(result.getResult());
        LOGGER.info("返回值2===" + result.getResult());
//        LOGGER.info("返回值3===" + resJson);

        //access_token无效，重试三次
        int count = 0;
        while (resJson.getInteger("errorCode") == 401 && count++ < 3) {
            Thread.sleep(300);

            LOGGER.error("access_token无效，重试第" + count + "次===");

            updateAccessToken();

            commonData.addHeader("accesstoken", ACCESS_TOKEN);
            LOGGER.error(commonData.getBody());

            result = client.execute(commonData);
            LOGGER.info("返回值1===" + JSON.toJSONString(result));

            resJson = JSONObject.parseObject(result.getResult());
            LOGGER.info("返回值2===" + result.getResult());
//            LOGGER.info("返回值3===" + resJson);
        }
        
        msg+=resJson.getString("message")==null?"":resJson.getString("message");

        return msg;
    }

    public JSONObject getFromErp(String url, JSONObject dataBody) throws InterruptedException {
        Date now = new Date();
        if (ACCESS_TOKEN.isEmpty() || !now.before(EXPIRE_TIME)) {
            updateAccessToken();
        }

        // 调用接口获取token
        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString(url);
        commonData.setCall_type("POST");
        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json");
        commonData.addHeader("accesstoken", ACCESS_TOKEN);
        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.info("返回值1===" + JSON.toJSONString(result));

        JSONObject resJson = JSONObject.parseObject(result.getResult());
        LOGGER.info("返回值2===" + result.getResult());
//        LOGGER.info("返回值3===" + resJson);

        //access_token无效，重试三次
        int count = 0;
        while (resJson.getInteger("errorCode") == 401 && count++ < 3) {
            Thread.sleep(300);

            LOGGER.error("access_token无效，重试第" + count + "次===");

            updateAccessToken();

            commonData.addHeader("accesstoken", ACCESS_TOKEN);
            LOGGER.error(commonData.getBody());

            result = client.execute(commonData);
            LOGGER.info("返回值1===" + JSON.toJSONString(result));

            resJson = JSONObject.parseObject(result.getResult());
            LOGGER.info("返回值2===" + result.getResult());
//            LOGGER.info("返回值3===" + resJson);
        }

        return resJson;
    }

    public String sendIndustryToErp(List<Industry__c> industryList) throws InterruptedException {
        LOGGER.error("行业推送===");
        List<JSONObject> jsonObjectList = new ArrayList<>();

        for (Industry__c industry : industryList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", industry.getName());
            jsonObject.put("number", industry.getCodeAuto__c());
            jsonObject.put("enable", industry.getEnable__c()==2?1:0);
            jsonObjectList.add(jsonObject);
        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/shkd_industry/addindustry";

        String msg = sendToErp(url, jsonObjectList);

        return "行业推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public void sendIndustryToErp2(List<Industry__c> newList) throws ApiEntityServiceException {
        LOGGER.error("行业推送===");
        List<String> idList = new ArrayList<>();
        for (Industry__c industry : newList) {
            idList.add(industry.getId().toString());
        }
        LOGGER.info("sendIndustryToErp2 idList==" + idList);
        String res = CommonInterfaceUtil.sendInterfaceRequest("sendIndustryToErp", idList, "outbound", getAccessToken());
        LOGGER.info("sendIndustryToErp2 res==" + JSONObject.toJSONString(res));
    }

    public String sendOpportunityToErp(List<Opportunity> oppList) throws InterruptedException {
        LOGGER.error("项目推送===");
        List<JSONObject> jsonObjectList = new ArrayList<>();

        for (Opportunity opp : oppList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", opp.getOpportunityName());
            jsonObject.put("number", "gacFW");
            jsonObject.put("planbegindate", "2025-11-05");
            jsonObject.put("longnumber", "UBqHm");
            jsonObject.put("fullname", "5WIm0");
            jsonObject.put("isleaf", false);
            jsonObject.put("proaddress", "2NOJF");
            jsonObject.put("proadmindivision", "jFYus");
            jsonObject.put("createorg_number", "S5OLg");
            jsonObject.put("parent_number", "uoxHz");
            jsonObject.put("parent_name", "gmNwz");
            jsonObject.put("status", "A");
            jsonObjectList.add(jsonObject);
        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/bd_project/addproject";

        String msg = sendToErp(url, jsonObjectList);

        return "项目推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public String sendOpportunityToErpById(Set<Long> ids) throws InterruptedException, ApiEntityServiceException {
        LOGGER.error("项目推送===");

        List<JSONObject> jsonObjectList = new ArrayList<>();

        String sql = "SELECT" +
                " opportunityCode__c number" +
                ", opportunityName name" +
                ", start_Date__c planbegindate" +
                ", opportunity_Contract_Value__c shkd_opportunitycontractv" +
                ", (SELECT name__c shkd_contractname, contractCode shkd_contractcode FROM contract WHERE opportunityId = opportunity.id) contract" +
                ", TW_Signed_legal__c.Social_Security_Number__c shkd_contractsignedlocati" +
                ", contract_Type__c shkd_contracttype" +
                ", start_Date__c shkd_startdate" +
                ", end_Date__c shkd_enddate" +
                ", id shkd_projectid" +
                ", ownerId.dimDepart.departCode createorg_number" +
                ", parent_Opportunity__c.opportunityCode__c parent_number" +
                ", parent_Opportunity__c.opportunityName parent_name" +
                ", devDepart__c.departCode shkd_deliveryunit_number" +
                ", market__c shkd_market_number" +
                ", currencyUnit shkd_projectcurrency_number" +
                ", industry__c.code__c shkd_industryld_number" +
                ", ownerId.employeeCode shkd_opportunityowner_number" +
                " FROM opportunity WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(ids)) + ")";
        LOGGER.error("opportunity Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, false);
        if (query.getSuccess()) {
            for (JSONObject record : query.getRecords()) {
                JSONArray contractArray = record.getJSONArray("contract");
                if (contractArray != null && !contractArray.isEmpty()) {
                    JSONObject firstContract = contractArray.getJSONObject(0);
                    record.putAll(firstContract);
                    record.remove("contract");
                }

                JSONObject oppJson = JSONObject.parseObject(record.toString().replace("[","").replace("]",""));
                oppJson.put("status","C");
                oppJson.put("shkd_active","0");

                jsonObjectList.add(oppJson);
            }

        } else {
            LOGGER.error("opportunity query error: " + query.getErrorMessage());
        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/bd_project/addproject";

        String msg = sendToErp(url, jsonObjectList);

        return "项目推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public void sendOpportunityToErpById2(List<String> ids) throws ApiEntityServiceException {
        LOGGER.error("项目推送===");

        LOGGER.info("sendOpportunityToErpById2 idList==" + ids);
        String res = CommonInterfaceUtil.sendInterfaceRequest("sendOpportunityToErp", ids, "outbound", getAccessToken());
        LOGGER.info("sendOpportunityToErpById2 res==" + res);
    }

    public String sendContractToErp(List<Contract> contractList) throws InterruptedException {
        LOGGER.error("合同推送===");
        List<JSONObject> jsonObjectList = new ArrayList<>();

        for (Contract contract : contractList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", contract.getName__c());
            jsonObject.put("status", "C");
            jsonObject.put("enable", "1");
            jsonObject.put("org_name", "pa4Vz");
            jsonObject.put("org_number", "lvebg");
            jsonObject.put(" subconnum ", "");
            jsonObject.put(" subconname ", "");
            jsonObjectList.add(jsonObject);
        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/shkd_salescontract/addcontparties";

        String msg = sendToErp(url, jsonObjectList);

        return "合同推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public String sendContractToErpById(Set<Long> ids) throws InterruptedException, ApiEntityServiceException, IOException {
        LOGGER.error("合同推送===");

        List<JSONObject> jsonObjectList = new ArrayList<>();

        RkhdHttpClient client = RkhdHttpClient.instance();
        Map<Long, String> entityTypeMap = NeoCrmUtils.getEntityTypeMap(client, "contract");

        String sql = "SELECT" +
                " contractCode number" +
                ", title name" +
                ", entityType shkd_contracttype" +
                ", startDate shkd_startdate" +
                ", endDate shkd_enddate" +
                ", amount shkd_contractamount" +
                ", contract_Status__c shkd_starts" +
                " FROM contract WHERE id IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(ids)) + ")";
        LOGGER.error("contract Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, false);
        if (query.getSuccess()) {
            for (JSONObject record : query.getRecords()) {
                record.put("shkd_contracttype", entityTypeMap.get(record.getLong("shkd_contracttype")));
                record.put("status", "C");
                record.put("enable", "1");
                jsonObjectList.add(record);
            }

        } else {
            LOGGER.error("contract query error: " + query.getErrorMessage());
        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/shkd_salescontract/addcontparties";

        String msg = sendToErp(url, jsonObjectList);

        return "合同推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public void sendContractToErpById2(List<String> ids) throws ApiEntityServiceException {
        LOGGER.error("合同推送===");

        LOGGER.info("sendContractToErpById2 idList==" + ids);
        String res = CommonInterfaceUtil.sendInterfaceRequest("sendContractToErp", ids, "outbound", getAccessToken());
        LOGGER.info("sendContractToErpById2 res==" + res);
    }


    public String sendAccountToErpById(Set<Long> accIds) throws ApiEntityServiceException, InterruptedException {
        LOGGER.error("客户推送===");

        Map<Long, JSONObject> accIdToDataMap = new HashMap<>();

        String sql = "SELECT" +
                " account__c" +
                ", account__c.client_Code__c number" +
                ", account__c.accountName name" +
                ", account__c.Social_Security_Number__c__c societycreditcode" +
                ", account__c.type__c type" +
                ", account__c.country_ISO__c country_number" +
                ", account__c.email__c postal_code" +
                ", account__c.phone bizpartner_phone" +
                ", account__c.address bizpartner_address" +
                ", account__c.settlementType__c settlementtypeid_number" +
                ", account__c.receivingCondition__c receivingcondid_number" +
                ", account__c.invoiceType__c invoicecategory_number" +
                ", bank_Account__c bankaccount" +
                ", name accountname" +
                ", bank__c.number__c bank_number" +
                ", currencyUnit__c paymentcurrency_number" +
                " FROM billing_Account__c WHERE account__c IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(accIds)) + ")";
        LOGGER.error("billing_Account__c Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, false);
        if (query.getSuccess()) {
            for (JSONObject record : query.getRecords()) {
                Long accId = record.getLong("account__c");
                if (!accIdToDataMap.containsKey(accId)) {
                    JSONObject accJson = JSONObject.parseObject(record.toString().replace("[", "").replace("]", ""));
                    accJson.remove("account__c");
                    accJson.remove("bankaccount");
                    accJson.remove("accountname");
                    accJson.remove("bank_number");
                    accJson.remove("paymentcurrency_number");
                    accJson.put("enable", "1"); //默认值
                    accJson.put("entry_bank", new JSONArray());
                    accIdToDataMap.put(record.getLong("account__c"), accJson);
                }

                JSONObject billingAccJson = new JSONObject();
                billingAccJson.put("bankaccount", record.getString("bankaccount"));
                billingAccJson.put("accountname", record.getString("accountname"));
                billingAccJson.put("bank_number", record.getString("bank_number"));
                billingAccJson.put("paymentcurrency_number", record.getString("paymentcurrency_number"));

                JSONArray billingAccJsonList = accIdToDataMap.get(accId).getJSONArray("entry_bank");
                billingAccJsonList.add(billingAccJson);
            }
        } else {
            LOGGER.error("billing_Account__c query error: " + query.getErrorMessage());
        }

//        List<JSONObject> jsonObjectList = new ArrayList<>();

//        for(Long accId : accIds) {
//            JSONObject jsonObject = accIdToAccMap.get(accId);
//
//            jsonObject.put("entry_bank",accIdToAccMap.get(accId));
//
//            jsonObjectList.add(jsonObject);
//        }
//
//        for (Account account : accountList) {
//            JSONObject jsonObject = new JSONObject();
//
//            jsonObject.put("number", account.getClient_Code__c();
//            jsonObject.put("name", account.getAccountName());
//            jsonObject.put("societycreditcode", account.getSocial_Security_Number__c__c());
//            jsonObject.put("type", account.getType__c());
//            jsonObject.put("country_number",account.getCountry_ISO__c());
//            jsonObject.put("postal_code",account.getEmail__c());
//            jsonObject.put("bizpartner_phone",account.getPhone());
//            jsonObject.put("bizpartner_address",account.getAddress());
//            jsonObject.put("enable","1");
//
//
//            List<JSONObject> entryBankList = new ArrayList<>();
//            JSONObject entryBank = new JSONObject();
//            entryBank.put("bankaccount","bankaccount");
//            entryBank.put("accountname", "accountname");
//            entryBank.put("accountname", "accountname");
//            entryBankList.add(entryBank);
//            jsonObject.put("entry_bank", entryBankList);
//
//            jsonObjectList.add(jsonObject);
//        }

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/bd_customer/addcustomer";

        String msg = sendToErp(url, new ArrayList<>(accIdToDataMap.values()));

        return "客户推送" + (msg.isEmpty() ? "成功" : "失败: ") + msg;
    }

    public ResponseEntity sendAccountToErpById2(List<String> ids) throws ApiEntityServiceException {
        ResponseEntity response = new ResponseEntity();

        LOGGER.error("客户推送===");

        LOGGER.info("sendAccountToErpById2 idList==" + ids);
        String res = CommonInterfaceUtil.sendInterfaceRequest("sendAccountToErp", ids, "outbound", getAccessToken());
        LOGGER.info("sendAccountToErpById2 res==" + res);

        JSONObject resJson = JSONObject.parseObject(res);

        response.setSuccess(resJson.getBoolean("status"));
        response.setMessage(resJson.getString("message"));

        return response;
    }

    public JSONObject queryBank(JSONObject filter, int pageNo, int pageSize) throws InterruptedException {
        LOGGER.error("查询银行===");
        List<JSONObject> jsonObjectList = new ArrayList<>();

        JSONObject dataBody = new JSONObject();
        dataBody.put("data", filter);
        dataBody.put("pageNo", pageNo);
        dataBody.put("pageSize", pageSize);

        String url = "https://inspire.test.kdcloud.com/kapi/v2/shkd/basedata/bd_bebank/querybank";

        return getFromErp(url, dataBody);
    }

    public List<Bank__c> getBanks(int pageNo,int pageSize) throws InterruptedException, ApiEntityServiceException, ParseException, BatchJobException {
        LOGGER.error("开始获取银行");

        String msg = "";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<Bank__c> banks = new ArrayList<>();

        JSONObject resultJson = queryBank(new JSONObject(), pageNo, pageSize);

        if (!resultJson.getBoolean("status")) {
            msg += " 查询银行信息失败: " + resultJson.getString("message");

        }

//        if (resultJson.getJSONObject("data").getBoolean("lastPage")) {
//            return banks;
//        }

        JSONArray bankJsonArray = resultJson.getJSONObject("data").getJSONArray("rows");

        for (Object obj : bankJsonArray) {
            JSONObject bankJson = (JSONObject) obj;
            Bank__c bank = new Bank__c();
            bank.setNumber__c(bankJson.getString("number"));
            bank.setName(bankJson.getString("name"));
            bank.setEnable__c(bankJson.getInteger("enable") == 1);
            bank.setName_eng__c(bankJson.getString("name_eng"));
            bank.setModifytime__c(sdf.parse(bankJson.getString("modifytime")).getTime());
            bank.setUnion_number__c(bankJson.getString("union_number"));
            bank.setAddress__c(bankJson.getString("address"));
            bank.setProvincetxt__c(bankJson.getString("provincetxt"));
            bank.setCitytxt__c(bankJson.getString("citytxt"));
            bank.setBankcatename__c(bankJson.getString("bankcatename"));

            String status = bankJson.getString("status");

            switch (status) {
                case "A":
                    bank.setStatus__c(1);
                    break;
                case "B":
                    bank.setStatus__c(2);
                    break;
                case "C":
                    bank.setStatus__c(3);
                    break;
            }

            banks.add(bank);
        }

//            NeoCrmUtils.upsertWithField(banks, "bank__c", "number__c", 4084436793545493L, "同步银行");


        LOGGER.error("获取银行msg => " + msg);
        LOGGER.error("获取银行结束");

        return banks;
    }

    public List<SimpleMap> getBanksSimpleMap(JSONObject filter,int pageNo, int pageSize) throws InterruptedException, ApiEntityServiceException, ParseException, BatchJobException {
        LOGGER.error("开始获取银行SimpleMap");

        String msg = "";

        List<SimpleMap> banks = new ArrayList<>();

        JSONObject resultJson = queryBank(filter, pageNo, pageSize);

        if (!resultJson.getBoolean("status")) {
            msg += " 查询银行信息失败: " + resultJson.getString("message");
        }

//        if (resultJson.getJSONObject("data").getBoolean("lastPage")) {
//            return banks;
//        }

        JSONArray bankJsonArray = resultJson.getJSONObject("data").getJSONArray("rows");

        for (Object obj : bankJsonArray) {
            JSONObject bankJson = (JSONObject) obj;
            SimpleMap bank = new SimpleMap();
            bank.set("number",bankJson.getString("number"));
            bank.set("name",bankJson.getString("name"));
            bank.set("enable",bankJson.getInteger("enable").toString());
            bank.set("name_eng",bankJson.getString("name_eng"));
            bank.set("modifytime",bankJson.getString("modifytime"));
            bank.set("union_number",bankJson.getString("union_number"));
            bank.set("address",bankJson.getString("address"));
            bank.set("provincetxt",bankJson.getString("provincetxt"));
            bank.set("citytxt",bankJson.getString("citytxt"));
            bank.set("bankcatename",bankJson.getString("bankcatename"));
            bank.set("status",bankJson.getString("status"));

            banks.add(bank);
        }

//            NeoCrmUtils.upsertWithField(banks, "bank__c", "number__c", 4084436793545493L, "同步银行");


        LOGGER.error("获取银行msg => " + msg);
        LOGGER.error("获取银行结束");

        return banks;
    }

}
