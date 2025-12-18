package other.tw.business.industry;

import com.rkhd.platform.sdk.data.model.Industry__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import other.tw.business.service.ErpService;

import java.util.List;

public class IndustryService {
    public void sendToErp(List<Industry__c> newList) throws InterruptedException, ApiEntityServiceException {
        ErpService erpService = new ErpService();

        erpService.sendIndustryToErp2(newList);
    }
}
