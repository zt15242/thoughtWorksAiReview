package other.tw.business.contract;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.Constants;
import other.tw.business.contractopportunityallocation.ContractOppSelector;
import other.tw.business.opportunity.OpportunityService;
import other.tw.business.service.ErpService;
import other.tw.business.util.CommonInterfaceUtil;
import other.tw.business.util.NeoCrmUtils;

import java.io.IOException;
import java.util.*;

public class ApprovalAgreeAfterEventContract implements ApprovalEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */

    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        logger.warn("开始执行通过后事件");
        logger.info("合同审批通过后 ApprovalEventRequest =>"+ JSON.toJSONString(request));
//        logger.info("getUsertaskLogId => "+request.getUsertaskLogId());
//        logger.info("getInstanceId => "+request.getInstanceId());
//        logger.info("getProcessApiKey => "+request.getProcessApiKey());
//        logger.info("taskInfo => "+JSON.toJSONString(request.getTaskInfo()));
        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //提交后事件实现
            agreeAfter(request);

            response.setSuccess(true);
            response.setMsg("接口执行成功");

            //重新计算商机OCV
            reCalOpportunityOCV(request.getDataId());
        } catch (Exception e) {
            logger.error("接口执行失败" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("接口执行失败");
        }

        return response;
    }

    //重新计算商机OCV
    private void reCalOpportunityOCV(Long contractId) throws ScriptBusinessException, ApiEntityServiceException {
        List<Opportunity> oppList = new ContractOppSelector().getOpportunityByContractId(Collections.singleton(contractId));

        new OpportunityService().reCalOpportunityContractValue(oppList);

        NeoCrmUtils.update(oppList,"合同审批通过，重新计算商机OCV");
    }

    /**
     * 可视化流程阶段推进后事件逻辑
     * @param request
     */
    private void agreeAfter(ApprovalEventRequest request) {
        //数据id
        Long pursuitRequestId = request.getDataId();
        Long userTaskLogId = request.getUsertaskLogId();
        try {
            String contractSql = "SELECT id FROM paymentApplicationPlan WHERE contractId = " + pursuitRequestId ;
            logger.error("paymentApplicationPlan Sql:" + contractSql);
            QueryResult<JSONObject> contractQuery = XoqlService.instance().query(contractSql, true, true);
            if (contractQuery.getSuccess()) {
                for (JSONObject record : contractQuery.getRecords()) {
                    if (record.getString("id")!=null&&!record.getString("id").isEmpty()){
                        List<String> idList = Arrays.asList(record.getString("id"));
                        String res = CommonInterfaceUtil.sendInterfaceRequest("PaymentApplicationPlanCRMtoJigsaw", idList, "outbound");
                        logger.info("NoPoCRMtoJigsaw res==" + JSONObject.toJSONString(res));
                    }
                }
            }

            //推送到金蝶
            new ErpService().sendContractToErpById2(Collections.singletonList(pursuitRequestId.toString()));

            Contract contract = new Contract();
            contract.setId(pursuitRequestId);
            NeoCrmUtils.updateApprovalComment(contract,userTaskLogId,"last_review_accepted_comments__c");
        } catch (ApiEntityServiceException | IOException e) {
            logger.info("RuntimeException==" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
