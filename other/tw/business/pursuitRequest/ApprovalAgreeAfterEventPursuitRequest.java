package other.tw.business.pursuitRequest;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;

import other.tw.business.util.CommonInterfaceUtil;

import java.util.Arrays;

import java.util.List;

public class ApprovalAgreeAfterEventPursuitRequest implements ApprovalEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */

    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        logger.warn("开始执行通过后事件");
        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //提交后事件实现
            agreeAfter(request);

            response.setSuccess(true);
            response.setMsg("接口执行成功");
        } catch (Exception e) {
            logger.error("接口执行失败" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("接口执行失败");
        }

        return response;
    }
    /**
     * 可视化流程阶段推进后事件逻辑
     * @param request
     */
    private void agreeAfter(ApprovalEventRequest request) {
        //数据id
        Long pursuitRequestId = request.getDataId();
        try {
            List<String> idList = Arrays.asList(pursuitRequestId.toString()); // 3924062138614808/3900668407006226
            String res = CommonInterfaceUtil.sendInterfaceRequest("PursuitCRMToJigsaw", idList, "outbound");
            logger.info("PursuitCRMToJigsaw res==" + JSONObject.toJSONString(res));
        } catch (
                ApiEntityServiceException e) {
            logger.info("RuntimeException==" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
