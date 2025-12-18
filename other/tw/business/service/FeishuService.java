package other.tw.business.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.http.CommonData;
import com.rkhd.platform.sdk.http.CommonHttpClient;
import com.rkhd.platform.sdk.http.HttpResult;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class FeishuService {
    public static final Logger LOGGER = LoggerFactory.getLogger();
    private static String TENANT_ACCESS_TOKEN = "";
    private static Date EXPIRE_TIME = new Date();

    public String updateTenantAccessToken(){
        String tokenUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";

        JSONObject tokenBody = new JSONObject();

        tokenBody.put("app_id","cli_a9a4e84ad7ba9bb4");
        tokenBody.put("app_secret","CFXlihnMiUFetbTJQfQCKhYVBbjm7WB1");

        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();
        commonData.setCallString(tokenUrl);
        commonData.setCall_type("POST");
        commonData.setBody(tokenBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        JSONObject resJson = JSONObject.parseObject(result.getResult());

        LOGGER.error("resJson: "+resJson.toJSONString());

        TENANT_ACCESS_TOKEN = resJson.getString("tenant_access_token");
        LOGGER.error("tenantAccessToken: "+TENANT_ACCESS_TOKEN);

        EXPIRE_TIME = new Date(new Date().getTime() + resJson.getInteger("expire")*1000);

        return TENANT_ACCESS_TOKEN;
    }

    private String getTenantAccessToken() {
        Date now = new Date();
        if (TENANT_ACCESS_TOKEN.isEmpty() || !now.before(EXPIRE_TIME)) {
            updateTenantAccessToken();
        }

        return TENANT_ACCESS_TOKEN;
    }

    public JSONObject getUserByDepartment(String departmentId,int pageSize, String pageToken) throws UnsupportedEncodingException {
        CommonHttpClient client = new CommonHttpClient();
        CommonData commonData = new CommonData();

        String url ="https://open.feishu.cn/open-apis/contact/v3/users/find_by_department?department_id_type=department_id&department_id="+departmentId+"&page_size="+pageSize;
        if(pageToken != null && !pageToken.isEmpty()){
            url += "&page_token="+URLEncoder.encode(pageToken,"UTF-8");
        }

        commonData.setCallString(url);
        commonData.setCall_type("GET");
//        commonData.setBody(dataBody.toString());
        commonData.addHeader("Content-Type", "application/json; charset=utf-8");
        commonData.addHeader("Authorization", "Bearer "+getTenantAccessToken());
        LOGGER.error(JSON.toJSONString(commonData.getHeaders()));
//        LOGGER.error(commonData.getBody());

        HttpResult result = new HttpResult();

        result = client.execute(commonData);
//        LOGGER.error("返回值1===" + JSON.toJSONString(result));
        LOGGER.error("返回值2===" + result.getResult());

        JSONObject resJson = JSONObject.parseObject(result.getResult());

        if (resJson.getInteger("code")==0){
            return resJson.getJSONObject("data");
        }
        else{
            LOGGER.error("FeishuService 返回错误 getUserByDepartment => "+resJson);
            return null;
        }
    }


}
