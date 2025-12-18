package other.tw.business.user;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Account;
import com.rkhd.platform.sdk.data.model.User;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.*;

public class UserSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public Map<String, JSONObject> getOpenIdMap() throws ApiEntityServiceException {

        String sql = "SELECT id, openId__c, managerId.openId__c old_leader_OpenId__c, leader_OpenId__c FROM user WHERE openId__c is not null";

        return NeoCrmUtils.query(sql,"openId__c","getOpenIdMap");
    }
}
