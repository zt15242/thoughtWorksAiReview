package other.tw.business.contract;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.PaymentApplicationPlan;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.paymentApplicationPlan.PaymentApplicationPlanSelector;

import java.util.Arrays;
import java.util.List;

public class ContractSubmitValidateEvent implements ApprovalEvent {

    private final Logger LOGGER = LoggerFactory.getLogger();

    private final PaymentApplicationPlanSelector paymentApplicationPlanSelector = new PaymentApplicationPlanSelector();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行合同审批提交前校验事件");
        LOGGER.info("合同审批提交前校验 ApprovalEventRequest =>"+ JSON.toJSONString(request));
        LOGGER.info("getUsertaskLogId => "+request.getUsertaskLogId());
        LOGGER.info("getInstanceId => "+request.getInstanceId());
        LOGGER.info("getProcessApiKey => "+request.getProcessApiKey());
        LOGGER.info("taskInfo => "+JSON.toJSONString(request.getTaskInfo()));
//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
        //待处理任务Id
//        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            List<PaymentApplicationPlan> paymentApplicationPlan = paymentApplicationPlanSelector.getPaymentApplicationPlanByContractId(request.getDataId());

            if(paymentApplicationPlan==null || paymentApplicationPlan.isEmpty() ){
                response.setSuccess(false);
                response.setMsg("The contract has no payment application plan");
            } else {
                response.setSuccess(true);
            }

        } catch (Exception e) {
            LOGGER.error("合同审批提交前校验事件执行失败: " + e.getMessage());
            LOGGER.error("合同审批提交前校验事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("合同审批提交前校验事件执行失败: " + e.getMessage());
        }

        return response;
    }

}
