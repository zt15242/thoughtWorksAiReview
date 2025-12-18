package other.tw.business.AutoFlowEventImpl;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEvent;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventRequest;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventResponse;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import other.tw.business.api.Interfaces;
import other.tw.business.util.ClientCodeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoFlowEventAccount implements AutoFlowEvent {
    private Logger logger = LoggerFactory.getLogger();

    /**
     * 执行入口
     */
    public AutoFlowEventResponse execute(AutoFlowEventRequest request)
            throws ScriptBusinessException {
        logger.error("开始执行自动流代码脚本事件");
        AutoFlowEventResponse response = new AutoFlowEventResponse();

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
    private void autoFlowEvent(AutoFlowEventRequest request) throws ApiEntityServiceException, IOException, ScriptBusinessException {
        Long pursuitRequestId = request.getDataId();
        String contractSql = "SELECT id,accountNamepinyin__c,accountCodeAuto__c,client_Code__c FROM account WHERE id = " + pursuitRequestId;
        String code = "";
        QueryResult<JSONObject> contractQuery = null;
        try {
            contractQuery = XoqlService.instance().query(contractSql, true, true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        logger.error("contractQuery:" + contractQuery);
        String pinyin = contractQuery.getRecords().get(0).getString("accountNamepinyin__c");

        String codePinyin = ClientCodeService.generateUniqueFromName(pinyin);
        logger.error("codePinyin:" + codePinyin);
        String accountCodAuto = contractQuery.getRecords().get(0).getString("accountCodeAuto__c");
        String lastTwo = accountCodAuto == null ? "" : (accountCodAuto.length() >= 2 ? accountCodAuto.substring(accountCodAuto.length() - 2) : accountCodAuto);
        if(!codePinyin.isEmpty()){
            code = codePinyin + lastTwo;
        }
        logger.error("code:" + code);
        Account account = new Account();
        List<XObject> updateList = new ArrayList<>();
        account.setId(pursuitRequestId);

        if (contractQuery.getRecords().get(0).getString("client_Code__c").isEmpty()){
            account.setClient_Code__c(code);
        }
        updateList.add(account);
        logger.info("updateList:" + updateList);
        BatchOperateResult batchResult = XObjectService.instance().update(updateList, true, true);
        logger.info("batchResult:" + batchResult.getOperateResults().get(0));
        Interfaces.ReturnResult syncAccountJigSawResult = Interfaces.syncAccountJigSaw(String.valueOf(pursuitRequestId));
        if (!syncAccountJigSawResult.getIsSuccess()) {
            throw new ScriptBusinessException(syncAccountJigSawResult.getMessage());
        }
    }

}
