package other.tw.business.AutoFlowEventImpl;

import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEvent;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventRequest;
import com.rkhd.platform.sdk.creekflow.ruleevent.RuleEventResponse;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;

import other.tw.business.api.Interfaces;

import java.io.IOException;

public class RuleEventAccount implements RuleEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public RuleEventResponse execute(RuleEventRequest request)
            throws ScriptBusinessException {
        logger.error("开始执行自动流代码脚本事件");
        RuleEventResponse response = new RuleEventResponse();

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
     *
     * @param request
     */
    private void autoFlowEvent(RuleEventRequest request) throws ApiEntityServiceException, IOException, ScriptBusinessException {
        Long pursuitRequestId = request.getDataId();
        Interfaces.ReturnResult syncAccountJigSawResult = Interfaces.syncAccountJigSaw(String.valueOf(pursuitRequestId));
        if (!syncAccountJigSawResult.getIsSuccess()) {
            throw new ScriptBusinessException(syncAccountJigSawResult.getMessage());
        }
    }
}
