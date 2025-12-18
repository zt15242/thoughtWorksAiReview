package other.tw.business.opplinksolution;

import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OppLinkSolutionSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public Map<Long, Double> getOpportunityPercentageMap(Set<Long> oppIds) throws ApiEntityServiceException {
        Map<Long, Double> oppToPercentageMap = new HashMap<>();

        if(oppIds.isEmpty()){
            LOGGER.error("getSolutionTotalPercentage oppIds is empty");
            return oppToPercentageMap;
        }

        String sql = "SELECT sum(Percentage__c) totalPercentage, opportunity__c FROM oppLinkSolution__c" +
                " WHERE opportunity__c IN (" + NeoCrmUtils.convertSetToString(oppIds) + ")" +
                " GROUP BY opportunity__c";
        LOGGER.error("getSolutionTotalPercentage sql => "+sql);
        List<JSONObject> records = NeoCrmUtils.query(sql,"getSolutionTotalPercentage", true);

        if (records != null) {
            for (JSONObject record : records) {
                oppToPercentageMap.put(record.getLong("opportunity__c"), record.getDouble("totalPercentage"));
            }
        }

        return oppToPercentageMap;
    }
}
