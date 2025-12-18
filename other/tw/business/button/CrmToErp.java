package other.tw.business.button;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.XObjectService;
import other.tw.business.Constants;
import other.tw.business.service.ErpService;
import other.tw.business.ResponseEntity;

import java.util.Collections;

@RestApi(baseUrl = "/button")
public class CrmToErp {
    public static final Logger LOGGER = LoggerFactory.getLogger();

    @RestMapping(value = "/sendAccountToErp", method = RequestMethod.POST)
    public String sendAccountToErp(@RestBeanParam(name = "data") String param) throws ApiEntityServiceException {
        LOGGER.error("sendAccountToErp param => "+param);
        JSONObject jsonObject = JSONObject.parseObject(param);
        LOGGER.error("jsonObject="+jsonObject.toJSONString());
        String id = jsonObject.getString("id");

        ResponseEntity response = new ResponseEntity();

        Account account = new Account();
        account.setId(Long.valueOf(id));
        account = XObjectService.instance().get(account);

        if (!Constants.ACCOUNT_APPROVAL_STATUS.equals(account.getApproval_Status__c())){
            response.setSuccess(false);
            response.setMessage("The account is not approved. ");
        }
        else{
            response = new ErpService().sendAccountToErpById2(Collections.singletonList(id));
        }

        return JSONObject.toJSONString(response);
    }
}
