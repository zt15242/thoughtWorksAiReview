package other.tw.business.AutoFlowEventImpl;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEvent;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventRequest;
import com.rkhd.platform.sdk.creekflow.autoflowevent.AutoFlowEventResponse;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.Opportunity;
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

public class AutoFlowEventOpportunity implements AutoFlowEvent {
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
        String contractSql = "SELECT id,oppNamePinyin__c,oppAutoCodes__c,opportunityCode__c,accountId.client_Code__c FROM opportunity WHERE id = " + pursuitRequestId;
        String code = "";
        QueryResult<JSONObject> contractQuery = null;
        try {
            contractQuery = XoqlService.instance().query(contractSql, true, true);
        } catch (ApiEntityServiceException e) {
            throw new RuntimeException(e);
        }
        logger.error("contractQuery:" + contractQuery);
        String pinyin = contractQuery.getRecords().get(0).getString("oppNamePinyin__c");
        String clientCode = contractQuery.getRecords().get(0).getString("accountId.client_Code__c");
        String clientLastTwo = clientCode == null ? "" : (clientCode.length() >= 2 ? clientCode.substring(clientCode.length() - 2) : clientCode);
        String codePinyin = ClientCodeService.generateUniqueFromName(pinyin);
        logger.error("codePinyin:" + codePinyin);
        String accountCodAuto = contractQuery.getRecords().get(0).getString("oppAutoCodes__c");
        if(!codePinyin.isEmpty()){
            code = codePinyin+accountCodAuto;
        }
        logger.error("code:" + code);
        Opportunity opp = new Opportunity();
        List<XObject> updateList = new ArrayList<>();
        opp.setId(pursuitRequestId);

        if (contractQuery.getRecords().get(0).getString("opportunityCode__c").isEmpty()){
            opp.setOpportunityCode__c(clientLastTwo+"-"+code);
        }
        updateList.add(opp);
        logger.info("updateList:" + updateList);
        BatchOperateResult batchResult = XObjectService.instance().update(updateList, true, true);
        logger.info("batchResult:" + batchResult.getOperateResults().get(0));
    }

}