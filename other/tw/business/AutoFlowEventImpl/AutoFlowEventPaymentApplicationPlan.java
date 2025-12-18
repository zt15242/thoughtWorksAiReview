package other.tw.business.AutoFlowEventImpl;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEvent;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventRequest;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventResponse;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEvent;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventRequest;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventResponse;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.util.CommonInterfaceUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AutoFlowEventPaymentApplicationPlan implements AutoFlowEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public AutoFlowEventResponse execute(AutoFlowEventRequest request)
            throws ScriptBusinessException {
        logger.error("开始执行自动流代码脚本事件");
        AutoFlowEventResponse response = new AutoFlowEventResponse();

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
     * 自动流代码脚本事件实现1
     * @param request
     */
    private void autoFlowEvent(AutoFlowEventRequest  request) throws ApiEntityServiceException, IOException {
        Long pursuitRequestId = request.getDataId();
        try {

            List<String> idList = Arrays.asList(pursuitRequestId.toString());
            String res = CommonInterfaceUtil.sendInterfaceRequest("PaymentApplicationPlanCRMtoJigsaw", idList, "outbound");
            logger.info("PaymentApplicationPlanCRMtoJigsaw res==" + JSONObject.toJSONString(res));
            
        } catch (
                ApiEntityServiceException e) {
            logger.info("RuntimeException==" + e.getMessage());
            throw new RuntimeException(e);
        }

    }
}
