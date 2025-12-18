package other.tw.business.AutoFlowEventImpl;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEvent;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventRequest;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventResponse;
import com.rkhd.platform.sdk.data.model.Opportunity;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.CommonInterfaceUtil;
import other.tw.business.util.NeoCrmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RuleEventOpportunity implements RuleEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public RuleEventResponse execute(RuleEventRequest request)
            throws ScriptBusinessException {
        logger.error("开始执行自动流代码脚本事件");
        RuleEventResponse  response = new RuleEventResponse ();

        try {
            //调用实现
            autoFlowEvent(request);
        } catch (Exception e) {
            logger.error("接口执行失败:" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("接口执行失败");
        }

        return response;
    }

    /**
     * 自动流代码脚本事件实现
     * @param request
     */
    private void autoFlowEvent(RuleEventRequest  request) throws ApiEntityServiceException, IOException {
        Long pursuitRequestId = request.getDataId();
        try {
            List<String> idList = Arrays.asList(pursuitRequestId.toString());
            String jsonResponse = CommonInterfaceUtil.sendInterfaceRequest("OpportunityCRMtoJigsaw", idList, "outbound");
            logger.info("OpportunityCRMtoJigsaw jsonResponse==" + JSONObject.toJSONString(jsonResponse));
            InterfaceResponse res = JSONObject.parseObject(jsonResponse, InterfaceResponse.class);

            logger.info("OpportunityCRMtoJigsaw res==" + JSONObject.toJSONString(res));

            // 2. Check msgty from ResultInfo to decide business logic execution
            if (res != null) {
                // Assume "00" means success (adjust value based on actual business requirements)
                if (Boolean.TRUE.equals(res.getSuccess())) {
                    Opportunity opportunity = new Opportunity();
                    List<Opportunity> opportunities = new ArrayList<>();
                    opportunity.setId(pursuitRequestId);
                    opportunity.setSyncJigsawflag__c(true);
                    opportunities.add(opportunity);
                    String updateres = NeoCrmUtils.update(opportunities, "Opportunity");
                    logger.info("update Opportunity res==" + JSONObject.toJSONString(updateres));
                } else {
                    logger.info("msgty is invalid (" + res.getErrorMessage() + "), skipping business code");
                }
            } else {
                logger.error("InterfaceResponse or ResultInfo is null");
            }


        } catch (
                ApiEntityServiceException e) {
            logger.info("RuntimeException==" + e.getMessage());
            throw new RuntimeException(e);
        }

    }
    public static class InterfaceResponse {
        private Boolean success;
        private String errorMessage;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
