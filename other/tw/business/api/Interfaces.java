package other.tw.business.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.*;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.MetadataService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.AutoFlowEventImpl.RuleEventOpportunity;
import other.tw.business.util.CommonInterfaceUtil;
import other.tw.business.util.NeoCrmUtils;
import other.tw.business.util.ObjectMetaReq;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestApi(baseUrl = "/service/apexrest")
public class Interfaces {
    private static final Logger logger= LoggerFactory.getLogger();
    @RestMapping(value = "/opportunityPursuit", method = RequestMethod.POST)
    public String opportunityPursuit(@RestBeanParam(name = "reqBody") String reqBody) throws IOException, ApiEntityServiceException {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResultinfo(new ResultInfo());
        apiResponse.setInterinfo(new InterInfo());
        apiResponse.getInterinfo().setResponsetime(String.valueOf(new Date()));
        apiResponse.getInterinfo().setRequesttime(String.valueOf(new Date()));
        apiResponse.getInterinfo().setInstid("opportunityPursuit");
        apiResponse.getInterinfo().setAttr3("");
        apiResponse.getInterinfo().setAttr2("");
        apiResponse.getInterinfo().setAttr1("");
        logger.info("reqBody:"+reqBody);
        JSONObject reqJson = JSONObject.parseObject(reqBody);
        JSONArray items = reqJson.getJSONArray("resultinfo");
        Map<String, String> itemCodeToEmpCode = new HashMap<>();
        Set<String> empCodes = new HashSet<>();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                JSONObject it = items.getJSONObject(i);
                String itemCode = it.getString("itemCode");
                String empCode = it.getString("mNameCode");
                if (itemCode != null && !itemCode.isEmpty() && empCode != null && !empCode.isEmpty()) {
                    itemCodeToEmpCode.put(itemCode, empCode);
                    empCodes.add(empCode);
                }
            }
        }
        Map<String, Long> empCodeToUserId = new HashMap<>();
        if (!empCodes.isEmpty()) {
            String userSql = "SELECT id,employeeCode FROM user WHERE employeeCode IN (" + NeoCrmUtils.convertListToString(new ArrayList<>(empCodes)) + ")";
            logger.error("userSql:" + userSql);
            QueryResult<JSONObject> userQuery = XoqlService.instance().query(userSql, true, true);
            if (userQuery.getSuccess()) {
                for (JSONObject record : userQuery.getRecords()) {
                    String ec = record.getString("employeeCode");
                    String uid = record.getString("id");
                    if (ec != null && uid != null) {
                        empCodeToUserId.put(ec, Long.parseLong(uid));
                    }
                }
            }
        }
        Map<String, Long> itemCodeToUserId = new HashMap<>();
        for (Map.Entry<String, String> e : itemCodeToEmpCode.entrySet()) {
            Long uid = empCodeToUserId.get(e.getValue());
            if (uid != null) {
                itemCodeToUserId.put(e.getKey(), uid);
            }
        }
        logger.info("reqJson:"+reqBody);
        JSONObject respJson = CommonInterfaceUtil.autoMapping("opportunityPursuit", "inbound", reqJson);
        logger.info("respJson:"+respJson);
        List<Pursuit_Request_Item__c> pursuitRequestList = JSON.parseArray(respJson.getJSONArray("pursuit_Request_Item__c").toJSONString(), Pursuit_Request_Item__c.class);
        logger.info("pursuitRequestList:"+pursuitRequestList);
        // Convert pursuitRequestList to map (key: Original_ID__c, value: Pursuit_Request__c)
        Map<String, Pursuit_Request_Item__c> pursuitRequestMap = pursuitRequestList.stream()
                .collect(Collectors.toMap(
                        Pursuit_Request_Item__c::getCodeAuto__c,  // Key: Original_ID__c
                        Function.identity()                     // Value: The object itself
                ));
        logger.info("pursuitRequestMap:" + pursuitRequestMap);

        List<String> pursuitRequestIdList = pursuitRequestList.stream().map(Pursuit_Request_Item__c::getCodeAuto__c).collect(Collectors.toList());

        String contractSql = "SELECT id,codeAuto__c FROM pursuit_Request_Item__c WHERE codeAuto__c IN (" + NeoCrmUtils.convertListToString(pursuitRequestIdList) + ") ";
        logger.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        if (contractQuery.getRecords().isEmpty()){
            logger.error("getOpportunityContractDateByOppId contractQuery is null");
            apiResponse.getResultinfo().setMsgty("E");
            apiResponse.getResultinfo().setMsgtx("处理失败，异常：未查询到数据");
            apiResponse.getInterinfo().setReturnstatus(apiResponse.getResultinfo().getMsgty());
            apiResponse.getInterinfo().setReturnmsg(apiResponse.getResultinfo().getMsgtx());
            apiResponse.getInterinfo().setReturncode(apiResponse.getResultinfo().getMsgty().equals("S")?"S001":"E001");
            return JSONObject.toJSONString(apiResponse);
        }
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {

                if (record.getString("codeAuto__c")!=null){
                    if (pursuitRequestMap.containsKey(record.getString("codeAuto__c"))){
                        Pursuit_Request_Item__c pursuitRequest = pursuitRequestMap.get(record.getString("codeAuto__c"));
                        pursuitRequest.setId(Long.parseLong(record.getString("id")));
                        Long uid = itemCodeToUserId.get(record.getString("codeAuto__c"));
                        if (uid != null) {
                            pursuitRequest.setMNameCode__c(uid);
                        }
                    }
                }
            }
        }
        List<Pursuit_Request_Item__c> updateList = pursuitRequestMap.values().stream().filter(item -> item.getId()!=null).collect(Collectors.toList());
        logger.info("updateList:" + updateList);
        BatchOperateResult batchResult = XObjectService.instance().update(updateList, true, true);
        if (!batchResult.getSuccess()) {
            logger.info("Batch update failed: " + batchResult.getOperateResults().get(0).getErrorMessage());
            apiResponse.getResultinfo().setMsgty("E");
            apiResponse.getResultinfo().setMsgtx("处理失败，异常：" + batchResult.getOperateResults().get(0).getErrorMessage());
        } else {
            apiResponse.getResultinfo().setMsgty("S");
            apiResponse.getResultinfo().setMsgtx("处理成功");
        }
        apiResponse.getInterinfo().setReturnstatus(apiResponse.getResultinfo().getMsgty());
        apiResponse.getInterinfo().setReturnmsg(apiResponse.getResultinfo().getMsgtx());
        apiResponse.getInterinfo().setReturncode(apiResponse.getResultinfo().getMsgty().equals("S")?"S001":"E001");
        return JSONObject.toJSONString(apiResponse);
    }
    @RestMapping(value = "/Accountjigsaw", method = RequestMethod.POST)
    public String Accountjigsaw(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException {
        ReturnResult result = new ReturnResult();
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        String contractSql = "SELECT id,entityType,client_Code__c FROM account WHERE id ="+Long.parseLong(id);
        logger.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        JSONObject account = contractQuery.getRecords().get(0);
        Long recodeTypeid = MetadataService.instance().getBusiType("account", "defaultBusiType").getId();
        if (!Objects.equals(account.getString("entityType"), recodeTypeid.toString())){
            result.setIsSuccess(false);
            result.setMessage("Processing failed, exception:  entityType is not account");
            return JSONObject.toJSONString(result);
        }
        if (account.getString("client_Code__c")==null|| Objects.equals(account.getString("client_Code__c"), "")){
            result.setIsSuccess(false);
            result.setMessage("Processing failed, exception:  clientCode cannot be empty");
            return JSONObject.toJSONString(result);
        }
        result.setIsSuccess(true);
        result.setMessage("Processing success");
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/Oppjigsaw", method = RequestMethod.POST)
    public String Oppjigsaw(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException {
        ReturnResult result = new ReturnResult();;
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        String contractSql = "SELECT id,opportunityCode__c,accountId.client_Code__c FROM opportunity WHERE id ="+Long.parseLong(id);
        logger.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        JSONObject account = contractQuery.getRecords().get(0);
        if (!Objects.equals(account.getString("opportunityCode__c"), "")&&(account.getString("accountId.client_Code__c")!=null|| !Objects.equals(account.getString("accountId.client_Code__c"), ""))){
            result = Interfaces.syncOpportunityJigSaw(id);
        }else {
            result.setIsSuccess(false);
            result.setMessage("Processing failed, exception: opportunityCode or clientCode cannot be empty");
        }

        return JSONObject.toJSONString(result);
    }


    @RestMapping(value = "/PursuitRequest", method = RequestMethod.POST)
    public String PursuitRequest(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException {
        ReturnResult result;
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        result = Interfaces.syncPursuitRequestJigSaw(id);
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/DRjigsaw", method = RequestMethod.POST)
    public String DRjigsaw(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException {
        ReturnResult result;
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        result = Interfaces.syncDRjigsaw(id);
        return JSONObject.toJSONString(result);
    }
    @RestMapping(value = "/syncpajigsaw", method = RequestMethod.POST)
    public String pajigsaw(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException {
        ReturnResult result;
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        result = Interfaces.syncpajigsaw(id);
        return JSONObject.toJSONString(result);
    }
    public static ReturnResult syncAccountJigSaw(String accountId) {
        return syncJigSaw(accountId, "Account");
    }

    public static ReturnResult syncOpportunityJigSaw(String opportunityId) {
        return syncJigSaw(opportunityId, "Opportunity");
    }
    public static ReturnResult syncPursuitRequestJigSaw(String pursuitRequestId) {
        return syncJigSaw(pursuitRequestId, "Pursuit_Request__c");
    }
    public static ReturnResult syncDRjigsaw(String drId) {
        return syncJigSaw(drId, "DealReview__c");
    }
    public static ReturnResult syncpajigsaw(String drId) {
        return syncJigSaw(drId, "PaymentApplicationPlan");
    }
    @RestMapping(value = "/GetDealDeskCoaches", method = RequestMethod.POST)
    public static String GetDealDeskCoaches(@RestBeanParam(name = "data") String param) throws IOException, ApiEntityServiceException, XsyHttpException {
        ReturnResult result = new ReturnResult();
        logger.error("进来调接口param===" + param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        String  id = jsonObject.getString("id");
        String contractSql = "SELECT id,opportunity__c.market__c,opportunity__c.Estimated_Margin_Range__c,opportunity__c.contract_Type__c,entityType FROM dealReview__c WHERE id ="+Long.parseLong(id);
        logger.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        JSONObject account = contractQuery.getRecords().get(0);
        String market = account.getString("opportunity__c.market__c");
        String marginRange = account.getString("opportunity__c.Estimated_Margin_Range__c");
        logger.info("marginRange1===" + marginRange);
        Long entityTypeId = Long.parseLong(account.getString("entityType"));
        logger.info("entityTypeId===" + entityTypeId);
        Long Tenderapproval = MetadataService.instance().getBusiType("dealReview__c", "defaultBusiType").getId();
        logger.info("Tenderapproval===" + Tenderapproval);
        Long quotationApprove = MetadataService.instance().getBusiType("dealReview__c", "quotationApprove__c").getId();
//        Long NoPoApprove = MetadataService.instance().getBusiType("dealReview__c", "NoPoApprove__c").getId();
//        Long Contract_DR_Approval = MetadataService.instance().getBusiType("dealReview__c", "Contract_DR_Approval__c").getId();
        String contractType = account.getString("opportunity__c.contract_Type__c");
        String GlobalDealSql = "SELECT id,demand_Coach_Employee_ID__c,finance_Coach_Employee_ID__c,legal_Coach_Employee_ID__c,delivery_Coach_Employee_ID__c FROM global_Deal_Desk_Delegation__c  ";
        ObjectMetaReq objectMetaReq = ObjectMetaReq.instance().getObjectAllMeta("global_Deal_Desk_Delegation__c");
        Integer contractTypes = null;
        Integer marginRanges = null;
        if (!Objects.equals(contractType, "")){
             contractTypes = objectMetaReq.getOptionByApiKey("contract_Type__c", contractType);
        }
        if (!Objects.equals(marginRange, "")){
            marginRanges = objectMetaReq.getOptionByApiKey("cGM_Range__c", marginRange);
        }
        
        List<String> conditions = new ArrayList<>();
        if (!Objects.equals(market, "")) {
            conditions.add("market__c = '" + market + "'");
        }

        if (marginRanges!=null) {
            conditions.add("cGM_Range__c = " + marginRanges);
        }
        if (contractTypes != null) {
            conditions.add("contract_Type__c = " + contractTypes);
        }

        if (!conditions.isEmpty()) {
            GlobalDealSql += " WHERE " + String.join(" AND ", conditions)+" limit 1";
        }
        logger.error("getOpportunityContractDateByOppId GlobalDealSql:" + GlobalDealSql);
        QueryResult<JSONObject> GlobalDealQuery = XoqlService.instance().query(GlobalDealSql, true, true);
        if (GlobalDealQuery.getRecords().isEmpty()){
            result.setIsSuccess(false);
            result.setMessage("   No records found");
            return JSONObject.toJSONString(result);
        }
        JSONObject GlobalDeal = GlobalDealQuery.getRecords().get(0);

        String demandCode = GlobalDeal.getString("demand_Coach_Employee_ID__c");
        String financeCode = GlobalDeal.getString("finance_Coach_Employee_ID__c");
        String legalCode = GlobalDeal.getString("legal_Coach_Employee_ID__c");
        String deliveryCode = GlobalDeal.getString("delivery_Coach_Employee_ID__c");

        List<String> empCodes = new ArrayList<>();
        if (demandCode != null && !Objects.equals(demandCode, "")) empCodes.add(demandCode);
        if (financeCode != null && !Objects.equals(financeCode, "")) empCodes.add(financeCode);
        if (legalCode != null && !Objects.equals(legalCode, "")) empCodes.add(legalCode);
        if (deliveryCode != null && !Objects.equals(deliveryCode, "")) empCodes.add(deliveryCode);

        Map<String, Long> codeToUserId = new HashMap<>();
        if (!empCodes.isEmpty()) {
            String userSql = "SELECT id, employeeCode FROM user WHERE employeeCode IN (" + NeoCrmUtils.convertListToString(empCodes) + ")";
            QueryResult<JSONObject> userQuery = XoqlService.instance().query(userSql, true, true);
            if (userQuery.getSuccess()) {
                for (JSONObject record : userQuery.getRecords()) {
                    String ec = record.getString("employeeCode");
                    String uid = record.getString("id");
                    if (ec != null && uid != null) {
                        codeToUserId.put(ec, Long.parseLong(uid));
                    }
                }
            }
        }

        DealReview__c dealReview__c = new DealReview__c();
        dealReview__c.setId(Long.parseLong(id));
        if (demandCode != null && codeToUserId.containsKey(demandCode)) {
            dealReview__c.setDemand_Approver__c(codeToUserId.get(demandCode));
        }
        if (financeCode != null && codeToUserId.containsKey(financeCode)) {
            dealReview__c.setFinance_Approver__c(codeToUserId.get(financeCode));
        }
        if (legalCode != null && codeToUserId.containsKey(legalCode) && !entityTypeId.equals(quotationApprove)) {
            dealReview__c.setLegal_Approver__c(codeToUserId.get(legalCode));
        }
        if (deliveryCode != null && codeToUserId.containsKey(deliveryCode) && !entityTypeId.equals(Tenderapproval)) {
            dealReview__c.setDelivery_Approver__c(codeToUserId.get(deliveryCode));
        }
        List<DealReview__c> DRs = new ArrayList<>();
        DRs.add(dealReview__c);
        String updateres = NeoCrmUtils.update(DRs, "DealReview__c");
        logger.info("update DealReview__c res==" + JSONObject.toJSONString(updateres));
        if (updateres.equals("DealReview__c")){
            result.setIsSuccess(true);
        }else {
            result.setIsSuccess(false);
            result.setMessage(updateres);
        }
        return JSONObject.toJSONString(result);
    }
    public static void main(String[] args) throws ApiEntityServiceException {

        syncJigSaw("4106712727622505", "Opportunity");
    }
    public static ReturnResult syncJigSaw(String objectId, String objectName) {
        List<String> idList = Arrays.asList(objectId);
        String jsonResponse = null;
        ReturnResult qsrj = new ReturnResult();
        qsrj.setIsSuccess(false);
        qsrj.setMessage("syncJigSaw failed for " + objectName);

        try {
            if (objectName.equals("Pursuit_Request__c")){
                jsonResponse = CommonInterfaceUtil.sendInterfaceRequest("PursuitCRMToJigsaw", idList, "outbound");
            }else if (objectName.equals("DealReview__c")){
                jsonResponse = CommonInterfaceUtil.sendInterfaceRequest("NoPoCRMtoJigsaw", idList, "outbound");
            }else if (objectName.equals("PaymentApplicationPlan")){
                jsonResponse = CommonInterfaceUtil.sendInterfaceRequest("PaymentApplicationPlanCRMtoJigsaw", idList, "outbound");
            }else {
                jsonResponse = CommonInterfaceUtil.sendInterfaceRequest(objectName + "CRMtoJigsaw", idList, "outbound");
            }

        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }

        logger.info(objectName + "CRMtoJigsaw jsonResponse==" + JSONObject.toJSONString(jsonResponse));
        RuleEventOpportunity.InterfaceResponse res = JSONObject.parseObject(jsonResponse, RuleEventOpportunity.InterfaceResponse.class);
        logger.info(objectName + "CRMtoJigsaw res==" + JSONObject.toJSONString(res));

        if (res != null) {
            if (Boolean.TRUE.equals(res.getSuccess())) {
                try {
                    Class<?> clazz = Class.forName("com.rkhd.platform.sdk.data.model." + objectName);
                    logger.info("Successfully loaded class: " + clazz.getName());

                    XObject object = (XObject) clazz.getConstructor().newInstance();
                    object.setId(Long.valueOf(objectId));
                    object.getClass().getMethod("setSyncJigsawflag__c", Boolean.class).invoke(object, Boolean.TRUE);

                    List<XObject> objects = new ArrayList<>();
                    objects.add(object);
                    BatchOperateResult batchResult = XObjectService.instance().update(objects, true, true);
                    if (!batchResult.getSuccess()) {
                        qsrj.setIsSuccess(false);
                        qsrj.setMessage("Failed to create/update " + objectName + " instance: " + batchResult.getOperateResults().get(0).getErrorMessage());
                        logger.info("Batch update failed: " + batchResult.getOperateResults().get(0).getErrorMessage());
                    }else {
                        logger.info("update " + objectName + " res==" + JSONObject.toJSONString(batchResult));
                        qsrj.setIsSuccess(true);
                        qsrj.setMessage("success");
                    }
                } catch (Exception e) {
                    qsrj.setIsSuccess(false);
                    qsrj.setMessage("Failed to create/update " + objectName + " instance: " + e.getMessage());
                    logger.error("Error processing " + objectName, e);
                }
            } else {
                qsrj.setIsSuccess(false);
                qsrj.setMessage(res.getErrorMessage());
            }
            logger.info("msgty is invalid (" + res.getErrorMessage() + "), skipping business code");
        } else {
            logger.error("InterfaceResponse or ResultInfo is null");
        }
        logger.info("qsrj" + JSONObject.toJSONString(qsrj));
        return qsrj;
    }

    // Then we can keep the original methods as wrappers for backward compatibility:

    @RestMapping(value = "/opportunityStaffing", method = RequestMethod.POST)
    public String opportunityStaffing(@RestBeanParam(name = "reqBody") String reqBody) throws IOException, ApiEntityServiceException {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setResultinfo(new ResultInfo());
        apiResponse.setInterinfo(new InterInfo());

        apiResponse.getInterinfo().setResponsetime(String.valueOf(new Date()));
        apiResponse.getInterinfo().setRequesttime(String.valueOf(new Date()));
        apiResponse.getInterinfo().setInstid("opportunityStaffing");
        apiResponse.getInterinfo().setAttr3("");
        apiResponse.getInterinfo().setAttr2("");
        apiResponse.getInterinfo().setAttr1("");
        logger.info("reqBody:"+reqBody);
        JSONObject reqJson = JSONObject.parseObject(reqBody);
        logger.info("reqJson:"+reqBody);
        JSONObject respJson = CommonInterfaceUtil.autoMapping("opportunityStaffing", "inbound", reqJson);
        logger.info("respJson:"+respJson);
        List<Opportunity> pursuitRequestList = JSON.parseArray(respJson.getJSONArray("opportunity").toJSONString(), Opportunity.class);
        logger.info("pursuitRequestList:"+pursuitRequestList);
        // Convert pursuitRequestList to map (key: Original_ID__c, value: Pursuit_Request__c)
        Map<String, Opportunity> pursuitRequestMap = pursuitRequestList.stream()
                .collect(Collectors.toMap(
                        Opportunity::getOpportunityCode__c,  // Key: Original_ID__c
                        Function.identity()                     // Value: The object itself
                ));
        logger.info("pursuitRequestMap:" + pursuitRequestMap);

        List<String> pursuitRequestIdList = pursuitRequestList.stream().map(Opportunity::getOpportunityCode__c).collect(Collectors.toList());

        String contractSql = "SELECT id,opportunityCode__c FROM opportunity WHERE opportunityCode__c IN (" + NeoCrmUtils.convertListToString(pursuitRequestIdList) + ") ";
        logger.error("getOpportunityContractDateByOppId contractSql:" + contractSql);
        QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
        logger.info("pursuitRequestMap:" + contractQuery);
        if (contractQuery.getRecords().isEmpty()){
            logger.error("getOpportunityContractDateByOppId contractQuery is null");
            apiResponse.getResultinfo().setMsgty("E");
            apiResponse.getResultinfo().setMsgtx("处理失败，异常：未查询到数据");
            apiResponse.getInterinfo().setReturnstatus(apiResponse.getResultinfo().getMsgty());
            apiResponse.getInterinfo().setReturnmsg(apiResponse.getResultinfo().getMsgtx());
            apiResponse.getInterinfo().setReturncode(apiResponse.getResultinfo().getMsgty().equals("S")?"S001":"E001");
            return JSONObject.toJSONString(apiResponse);
        }
        if (contractQuery.getSuccess()) {
            for (JSONObject record : contractQuery.getRecords()) {

                if (record.getString("opportunityCode__c")!=null){
                    if (pursuitRequestMap.containsKey(record.getString("opportunityCode__c"))){
                        Opportunity pursuitRequest = pursuitRequestMap.get(record.getString("opportunityCode__c"));
                        pursuitRequest.setId(Long.parseLong(record.getString("id")));
                    }
                }
            }
        }
        List<Opportunity> updateList = pursuitRequestMap.values().stream().filter(item -> item.getId()!=null).collect(Collectors.toList());
        logger.info("updateList:" + updateList);
        BatchOperateResult batchResult = XObjectService.instance().update(updateList, true, true);
        if (!batchResult.getSuccess()) {
            logger.info("Batch update failed: " + batchResult.getOperateResults().get(0).getErrorMessage());
            apiResponse.getResultinfo().setMsgty("E");
            apiResponse.getResultinfo().setMsgtx("处理失败，异常：" + batchResult.getOperateResults().get(0).getErrorMessage());
        } else {
            apiResponse.getResultinfo().setMsgty("S");
            apiResponse.getResultinfo().setMsgtx("处理成功");
        }
        apiResponse.getInterinfo().setReturnstatus(apiResponse.getResultinfo().getMsgty());
        apiResponse.getInterinfo().setReturnmsg(apiResponse.getResultinfo().getMsgtx());
        apiResponse.getInterinfo().setReturncode(apiResponse.getResultinfo().getMsgty().equals("S")?"S001":"E001");
        return JSONObject.toJSONString(apiResponse);
    }
    public class ApiResponse {
        private ResultInfo resultinfo;
        private InterInfo interinfo;

        // Default constructor
        public ApiResponse() {}

        // Getters and Setters
        public ResultInfo getResultinfo() {
            return resultinfo;
        }

        public void setResultinfo(ResultInfo resultinfo) {
            this.resultinfo = resultinfo;
        }

        public InterInfo getInterinfo() {
            return interinfo;
        }

        public void setInterinfo(InterInfo interinfo) {
            this.interinfo = interinfo;
        }


    }
    public static class  ReturnResult {
        // 表示操作是否成功
        private Boolean isSuccess;
        // 返回的消息
        private String message;

        // 构造函数
        public ReturnResult() {}

        // Getter 和 Setter 方法
        public Boolean getIsSuccess() {
            return isSuccess;
        }

        public void setIsSuccess(Boolean isSuccess) {
            this.isSuccess = isSuccess;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }
    public class InterInfo {
        private String returnstatus;
        private String returnmsg;
        private String returncode;
        private String responsetime;
        private String requesttime;
        private String instid;
        private String attr3;
        private String attr2;
        private String attr1;

        // Default constructor
        public InterInfo() {}

        // Getters and Setters
        public String getReturnstatus() {
            return returnstatus;
        }

        public void setReturnstatus(String returnstatus) {
            this.returnstatus = returnstatus;
        }

        public String getReturnmsg() {
            return returnmsg;
        }

        public void setReturnmsg(String returnmsg) {
            this.returnmsg = returnmsg;
        }

        public String getReturncode() {
            return returncode;
        }

        public void setReturncode(String returncode) {
            this.returncode = returncode;
        }

        public String getResponsetime() {
            return responsetime;
        }

        public void setResponsetime(String responsetime) {
            this.responsetime = responsetime;
        }

        public String getRequesttime() {
            return requesttime;
        }

        public void setRequesttime(String requesttime) {
            this.requesttime = requesttime;
        }

        public String getInstid() {
            return instid;
        }

        public void setInstid(String instid) {
            this.instid = instid;
        }

        public String getAttr3() {
            return attr3;
        }

        public void setAttr3(String attr3) {
            this.attr3 = attr3;
        }

        public String getAttr2() {
            return attr2;
        }

        public void setAttr2(String attr2) {
            this.attr2 = attr2;
        }

        public String getAttr1() {
            return attr1;
        }

        public void setAttr1(String attr1) {
            this.attr1 = attr1;
        }
    }
    public class ResultInfo {
        private String msgty;
        private String msgtx;

        // Default constructor
        public ResultInfo() {}

        // Getters and Setters
        public String getMsgty() {
            return msgty;
        }

        public void setMsgty(String msgty) {
            this.msgty = msgty;
        }

        public String getMsgtx() {
            return msgtx;
        }

        public void setMsgtx(String msgtx) {
            this.msgtx = msgtx;
        }
    }
}
