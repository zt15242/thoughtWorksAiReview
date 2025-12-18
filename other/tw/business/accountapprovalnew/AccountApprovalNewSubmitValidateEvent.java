package other.tw.business.accountapprovalnew;

import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Billing_Account__c;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.billingaccount.BillingAccountSelector;

import java.util.*;

public class AccountApprovalNewSubmitValidateEvent implements ApprovalEvent {

    private final Logger LOGGER = LoggerFactory.getLogger();

    private final BillingAccountSelector billingAccountSelector = new BillingAccountSelector();
    private final AccountApprovalNewSelector accountApprovalNewSelector = new AccountApprovalNewSelector();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行客户审批提交前校验事件");
//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
        //待处理任务Id
        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            Long accountId = accountApprovalNewSelector.getAccountIdByAccountApprovalId(request.getDataId());
            List<Billing_Account__c> billingAccountList = billingAccountSelector.getBillingAccountByAccountId(accountId);

            if (billingAccountList == null || billingAccountList.isEmpty()) {
                response.setSuccess(false);
                response.setMsg("The account has no billing account.");
            }else {
                response.setSuccess(true);
            }

        } catch (Exception e) {
            LOGGER.error("客户审批提交前校验事件执行失败: " + e.getMessage());
            LOGGER.error("客户审批提交前校验事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("客户审批提交前校验事件执行失败: " + e.getMessage());
        }

        return response;
    }

}
