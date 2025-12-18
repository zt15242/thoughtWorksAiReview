package other.tw.business.accountapprovalnew;

import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.XObjectService;
import other.tw.business.ResponseEntity;
import other.tw.business.service.ErpService;

import java.util.*;

public class AccountApprovalNewAgreeAfterEvent implements ApprovalEvent {

    private final Logger LOGGER = LoggerFactory.getLogger();

    private final AccountApprovalNewSelector accountApprovalSelector = new AccountApprovalNewSelector();

    /**
     * 执行入口
     */
    public ApprovalEventResponse execute(ApprovalEventRequest request)
            throws ScriptBusinessException {
        LOGGER.warn("开始执行客户审批通过后事件");
//        // 对象apikey
//        request.getEntityApiKey();
//        //数据Id
//        request.getDataId();
//        //待处理任务Id
//        request.getUsertaskLogId();

        ApprovalEventResponse response = new ApprovalEventResponse();

        try {
            //提交后事件实现
            Long accId = accountApprovalSelector.getAccountIdByAccountApprovalId(request.getDataId());

            Account account = new Account();
            account.setId(accId);
            account = XObjectService.instance().get(account);

            if (account.getApproval_Status__c()==3) {

                ErpService crmToErp = new ErpService();
                ResponseEntity res = crmToErp.sendAccountToErpById2(Collections.singletonList(accId.toString()));

                response.setSuccess(res.getSuccess());
                response.setMsg(res.getMessage());
            }else {
                response.setSuccess(true);
            }
        } catch (Exception e) {
            LOGGER.error("客户审批通过后事件执行失败: " + e.getMessage());
            LOGGER.error("客户审批通过后事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("客户审批通过后事件执行失败: " + e.getMessage());
        }

        return response;
    }

}
