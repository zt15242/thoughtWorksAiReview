package other.tw.business.accountteammember;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account_Team_Member__c;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandler;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.trigger.DataResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AccountTeamMemberService {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    public void createTeamMember(List<Account_Team_Member__c> newList, Map<Account_Team_Member__c, DataResult> resultMap) throws XsyHttpException, IOException {

        RkhdHttpClient client = new RkhdHttpClient();

        String url = "/rest/data/v2.0/xobjects/teamMember";

        RkhdHttpData rkhdHttpData = new RkhdHttpData();
        rkhdHttpData.setCallString(url);
        rkhdHttpData.setCall_type("POST");
        rkhdHttpData.putHeader("Content-Type", "application/json");
        rkhdHttpData.putHeader("charset", "utf-8");

        for (Account_Team_Member__c accountTeamMember : newList) {

            JSONObject dataObj = new JSONObject();
            dataObj.put("userId", accountTeamMember.getTeam_Member__c());
            dataObj.put("recordFrom", 1);
            dataObj.put("recordFrom_data", accountTeamMember.getAccount__c());
            Integer permission = accountTeamMember.getRead_And_Write_Permissions__c();
            dataObj.put("ownerFlag", permission != null && permission == 2 ? 1 : 2);

            JSONObject dataBody = new JSONObject();
            dataBody.put("data", dataObj);

            rkhdHttpData.setBody(dataBody.toString());
            LOGGER.error("创建团队成员主体rkhdHttpData =>" + rkhdHttpData);
            LOGGER.error("创建团队成员主体rkhdHttpData.getHeaders() =>" + rkhdHttpData.getHeaderMap());
            LOGGER.error("创建团队成员主体rkhdHttpData.getBody() =>" + rkhdHttpData.getBody());

            JSONObject resObj = (JSONObject) client.execute(rkhdHttpData, new ResponseBodyHandler<Object>() {
                @Override
                public JSONObject handle(String s) {
                    LOGGER.error("创建团队成员api返回： " + s);
                    return JSON.parseObject(s);
                }
            });

            String msg = resObj.getInteger("code") == 200 ? "" : "fail to create teamMember: " + resObj.getString("msg");
            DataResult result = resultMap.get(accountTeamMember);
            result.setMsg(result.getMsg() + " " + msg);
            if (resObj.getInteger("code") != 200) {
                result.setSuccess(false);
            }
        }
    }
}
