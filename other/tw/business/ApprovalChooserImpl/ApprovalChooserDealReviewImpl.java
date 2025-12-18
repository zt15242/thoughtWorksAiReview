package other.tw.business.ApprovalChooserImpl;

import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooser;
import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooserRequest;
import com.rkhd.platform.sdk.creekflow.approvalchooser.ApprovalChooserResponse;
import com.rkhd.platform.sdk.data.model.DealReview__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.ArrayList;
import java.util.List;

public class ApprovalChooserDealReviewImpl implements ApprovalChooser {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public ApprovalChooserResponse execute(ApprovalChooserRequest request)
            throws ScriptBusinessException {
        logger.error("开始执行动态选人脚本");
        // 对象apikey
        request.getEntityApiKey();
        //数据Id
        request.getDataId();
        //待处理任务Id
        request.getUsertaskLogId();

        //流程定义id
        request.getProcdefId();
        //节点apikey
        request.getTaskDefKey();
        //节点名称
        request.getTaskDefName();

        ApprovalChooserResponse response = new ApprovalChooserResponse();

        try {
            //调用实现
            //审批人Id
            List<Long> userIds = approvalChooser(request);
            response.setData(userIds);

            response.setSuccess(true);
            response.setMsg("动态选人执行成功");
        } catch (Exception e) {
            logger.error("动态选人执行失败:" + e.getMessage());
            response.setSuccess(false);
            response.setMsg("动态选人执行失败");
        }

        return response;
    }

    /**
     * 获取审批人逻辑
     * @param request
     * @return
     */
    private List<Long> approvalChooser(ApprovalChooserRequest request) {
        logger.error("执行动态选人脚本逻辑");

        List<Long> userIds = new ArrayList<Long>();
        Long dataId = request.getDataId();
        String consql = "Select id,delivery_Approver__c,finance_Approver__c,legal_Approver__c,demand_Approver__c from dealReview__c  where id ="+dataId;
        List<DealReview__c> conIds = null;
        try {
            conIds = NeoCrmUtils.executeQuery(consql, DealReview__c.class);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        logger.error("conIds="+conIds);
        /**
         * 审批人Id
         */
        if(conIds != null && conIds.size() > 0){
            DealReview__c conId = conIds.get(0);
            if(conId.getDelivery_Approver__c() != null){
                userIds.add(conId.getDelivery_Approver__c());
            }
            if(conId.getFinance_Approver__c() != null){
                userIds.add(conId.getFinance_Approver__c());
            }
            if(conId.getLegal_Approver__c() != null){
                userIds.add(conId.getLegal_Approver__c());
            }
            if(conId.getDemand_Approver__c() != null){
                userIds.add(conId.getDemand_Approver__c());
            }
        }
        return userIds;
    }
}
