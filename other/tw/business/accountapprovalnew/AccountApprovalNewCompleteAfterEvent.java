package other.tw.business.accountapprovalnew;

import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEvent;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventRequest;
import com.rkhd.platform.sdk.creekflow.approvalevent.ApprovalEventResponse;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.ResponseEntity;
import other.tw.business.service.ErpService;

import java.util.*;

public class AccountApprovalNewCompleteAfterEvent implements ApprovalEvent {

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
            Long accId = accountApprovalSelector.getAccountIdByAccountApprovalId(request.getDataId());

            ErpService crmToErp = new ErpService();
            ResponseEntity res = crmToErp.sendAccountToErpById2(Collections.singletonList(accId.toString()));

            response.setSuccess(res.getSuccess());
            response.setMsg(res.getMessage());
        } catch (Exception e) {
            LOGGER.error("客户审批通过后事件执行失败: " + e.getMessage());
            LOGGER.error("客户审批通过后事件执行失败 =>" + Arrays.toString(e.getStackTrace()));
            response.setSuccess(false);
            response.setMsg("客户审批通过后事件执行失败: " + e.getMessage());
        }

        return response;
    }

}
