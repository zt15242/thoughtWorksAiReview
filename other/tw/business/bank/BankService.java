package other.tw.business.bank;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Bank__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.BatchJobProService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import com.rkhd.platform.sdk.task.param.PrepareParam;
import com.rkhd.platform.sdk.task.param.SimpleMap;
import other.tw.business.service.ErpService;
import other.tw.business.util.NeoCrmUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BankService {

    private static final Logger LOGGER = LoggerFactory.getLogger();

    public void syncBank() throws BatchJobException, ParseException, InterruptedException, ApiEntityServiceException {

        int pageNo =1;

        boolean done = false;

        Bank__c bank = new Bank__c();
        bank.setId(4096210290266905L);
        bank.setEnable__c(done);

        NeoCrmUtils.update(Collections.singletonList(bank),"开始同步银行");

        while (!done) {
            PrepareParam param = new PrepareParam();
            param.set("pageNo", String.valueOf(pageNo));
            param.set("pageSize", "2000");
            BatchJobProService.instance().addBatchJob(BankSyncBatchJob.class, 500, param);

            bank = XObjectService.instance().get(bank);
            done = bank.getEnable__c();
            pageNo++;
        }
    }

}
