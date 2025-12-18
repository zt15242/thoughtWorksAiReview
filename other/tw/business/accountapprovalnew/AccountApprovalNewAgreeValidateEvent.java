package other.tw.business.accountapprovalnew;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.AccountApprovalNew__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.XObjectService;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class AccountApprovalNewAgreeValidateEvent implements ApprovalEvent {

    private final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行客户审批通过前校验事件");
//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
        //待处理任务Id
//        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        ScriptRuntimeContext src = ScriptRuntimeContext.instance();
        long userId = src.getUserId();

        try {
            JSONArray responsibilities = NeoCrmUtils.getResponsibilities(userId);

            AccountApprovalNew__c approval = new AccountApprovalNew__c();
            approval.setId(request.getDataId());

            approval = XObjectService.instance().get(approval);

            LOGGER.info(responsibilities.toString());
            LOGGER.info(JSON.toJSONString(approval));

            boolean legalFail = responsibilities.toString().contains("\"code\":\"Legal\"") && approval.getLegal_RiskLevel__c()==null;
            boolean financeFail = responsibilities.toString().contains("\"code\":\"finance\"") && approval.getFinance_RiskLevel__c()==null;

            if(!legalFail && !financeFail){
                response.setSuccess(true);
            }else if(legalFail && financeFail){
                response.setSuccess(false);
                response.setMsg("Finance Risk Level, Legal Risk Level required");
            } else if (legalFail) {
                response.setSuccess(false);
                response.setMsg("Legal Risk Level required");
            } else {
                response.setSuccess(false);
                response.setMsg("Finance Risk Level required");
            }

        } catch (Exception e) {
            LOGGER.error("客户审批通过前校验事件执行失败: " + e.getMessage());
            LOGGER.error("客户审批通过前校验事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("客户审批通过前校验事件执行失败: " + e.getMessage());
        }

        return response;
    }

}
