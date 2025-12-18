package other.tw.business.paymentApplicationPlan;


import com.rkhd.platform.sdk.data.model.PaymentApplicationPlan;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import other.tw.business.util.NeoCrmUtils;

import java.util.List;

public class PaymentApplicationPlanSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    public List<PaymentApplicationPlan> getPaymentApplicationPlanByContractId(Long conId) throws ApiEntityServiceException {
        String sql = "SELECT id FROM paymentApplicationPlan WHERE contractId = "+conId;
        return NeoCrmUtils.query(sql,PaymentApplicationPlan.class);
    }
}
