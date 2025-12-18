package other.tw.business.DealReview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rkhd.platform.sdk.context.ScriptRuntimeContext;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.DealReview__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.XObjectService;
import other.tw.business.util.NeoCrmUtils;

import java.util.Arrays;

public class DealReviewEntryWithoutPOAgreeValidateEvent implements ApprovalEvent {

    private final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行DealReview审批通过前校验事件");
//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
        //待处理任务Id
        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        ScriptRuntimeContext src = ScriptRuntimeContext.instance();
        long userId = src.getUserId();

        try {
            JSONArray responsibilities = NeoCrmUtils.getResponsibilities(userId);

            DealReview__c dealReview = new DealReview__c();
            dealReview.setId(request.getDataId());

            dealReview = XObjectService.instance().get(dealReview);

            LOGGER.info(responsibilities.toString());
            LOGGER.info(JSON.toJSONString(dealReview));

            boolean financeFail = responsibilities.toString().contains("\"code\":\"finance\"") && dealReview.getComfirm_Revenue__c()==null;

            if(financeFail){
                response.setSuccess(false);
                response.setMsg("Revenue Confirm required");
            }else{
                response.setSuccess(true);
            }

        } catch (Exception e) {
            LOGGER.error("DealReview审批通过前校验事件执行失败: " + e.getMessage());
            LOGGER.error("DealReview审批通过前校验事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("DealReview审批通过前校验事件执行失败: " + e.getMessage());
        }

        return response;
    }
}
