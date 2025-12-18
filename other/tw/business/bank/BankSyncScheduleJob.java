package other.tw.business.bank;


import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.ScheduleJob;
import com.rkhd.platform.sdk.api.annotations.RequestMethod;
import com.rkhd.platform.sdk.api.annotations.RestApi;
import com.rkhd.platform.sdk.api.annotations.RestBeanParam;
import com.rkhd.platform.sdk.api.annotations.RestMapping;
import com.rkhd.platform.sdk.data.model.Bank__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.param.ScheduleJobParam;
import com.rkhd.platform.sdk.service.BatchJobProService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.task.param.PrepareParam;
import other.tw.business.service.ErpService;
import other.tw.business.util.NeoCrmUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class BankSyncScheduleJob implements ScheduleJob {

    private static final Logger LOGGER = LoggerFactory.getLogger();

//    private static final BankService bankService = new BankService();

    @Override
    public void execute(ScheduleJobParam scheduleJobParam) {
        LOGGER.info("定时同步银行开始");
        try {
            JSONObject filter = new JSONObject();
            LocalDate today = LocalDate.now();
            filter.put("staterdate",today.minusDays(1)+" 00:00:00");
            filter.put("enddate",today+" 00:00:00");
            syncBank(filter,1,1500);
        } catch(Exception e) {
            LOGGER.error("定时同步银行 Exception message =>" + e.getMessage());
            LOGGER.error("定时同步银行 Exception =>" + Arrays.toString(e.getStackTrace()));
        }
        LOGGER.info("定时同步银行结束");
    }

    public void syncBank(JSONObject filter, int pageNo,int pageSize) throws BatchJobException, ParseException, InterruptedException, ApiEntityServiceException {
        JSONObject resultJson = new ErpService().queryBank(filter, 1, 1);

        int totalCount = resultJson.getJSONObject("data").getInteger("totalCount");

        LOGGER.error("同步银行数量 =>"+totalCount);
        if (totalCount==0){
            return;
        }

        int totalPage = (int) Math.ceil((double) totalCount / pageSize);

        if(pageNo < 1 || pageNo > totalPage){
            LOGGER.error("开始页号不在范围内 => "+"(1--"+totalPage+")");
            return;
        }

        PrepareParam param = new PrepareParam();
        param.set("filter",filter.toString());
        param.set("pageNo", String.valueOf(pageNo));
        param.set("pageSize", String.valueOf(pageSize));
        param.set("totalPage", String.valueOf(totalPage));

        BatchJobProService.instance().addBatchJob(BankSyncBatchJob.class, 10, param);
    }
}
