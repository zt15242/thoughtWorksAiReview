package other.tw.business.contract;

import com.alibaba.fastjson.JSON;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Contract;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.Arrays;

public class ContractRejectAfterEvent implements ApprovalEvent {
    private final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行合同审批拒绝后校验事件");
        LOGGER.info("合同审批拒绝后校验 ApprovalEventRequest =>"+ JSON.toJSONString(request));

//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
        //待处理任务Id
//        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            Contract contract = new Contract();
            contract.setId(request.getDataId());

            NeoCrmUtils.updateApprovalComment(contract,request.getUsertaskLogId(),"last_review_comments__c");

            response.setSuccess(true);
            response.setMsg("接口执行成功");
        } catch (Exception e) {
            LOGGER.error("合同审批拒绝后校验事件执行失败: " + e.getMessage());
            LOGGER.error("合同审批拒绝后校验事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("合同审批拒绝后校验事件执行失败: " + e.getMessage());
        }

        return response;
    }

}