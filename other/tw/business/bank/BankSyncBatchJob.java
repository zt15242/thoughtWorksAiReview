package other.tw.business.bank;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Bank__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.BatchJobProService;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.*;
import other.tw.business.service.ErpService;
import other.tw.business.util.NeoCrmUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

public class BankSyncBatchJob implements BatchJobPro<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        LOGGER.info("BankSyncBatchJob prepare is start!");

//        LOGGER.info(prepareParam.getParamMap().toString());

        List<Integer> data = new ArrayList<>();

        String filter = prepareParam.get("filter");
        int pageNo = Integer.parseInt(prepareParam.get("pageNo"));
        int pageSize = Integer.parseInt(prepareParam.get("pageSize"));
        int totalPage = Integer.parseInt(prepareParam.get("totalPage"));

        try {
            IntStream.range(pageNo, totalPage + 1)
                    .forEach(data::add);

            LOGGER.info("BankSyncBatchJob bank totalPage ==>"+totalPage);
            LOGGER.info("BankSyncBatchJob bank pageSize ==>"+pageSize);
            LOGGER.info("BankSyncBatchJob prepare data ==>"+data);
        } catch (Exception e) {
            LOGGER.error("BankSyncBatchJob Exception message =>" + e.getMessage());
            LOGGER.error("BankSyncBatchJob Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();

        return batchJobDataBuilder
                .setBatchJobParam("pageSize", String.valueOf(pageSize))
                .setBatchJobParam("filter", filter)
                .setDataList(data)
                .buildListData();
    }

    @Override
    public void execute(List<Integer> list, BatchJobParam batchJobParam) {
        LOGGER.info("BankSyncBatchJob execute list ==> "+list.toString());
        LOGGER.info("BankSyncBatchJob execute batchJobParam ==> "+batchJobParam.toString());
        String filter = batchJobParam.get("filter");
        int pageSize = Integer.parseInt(batchJobParam.get("pageSize"));
        try {
            todo(filter,list,pageSize);
        } catch (ApiEntityServiceException | BatchJobException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {
        LOGGER.info("BankSyncBatchJob finish");
    }

    private void todo(String filter,List<Integer> pageNoList,int pageSize) throws ApiEntityServiceException, BatchJobException {
        // 对数据做处理
        for (Integer pageNo : pageNoList) {
            PrepareParam param = new PrepareParam();
            param.set("filter",filter);
            param.set("pageNo", pageNo.toString());
            param.set("pageSize", String.valueOf(pageSize));
            BatchJobProService.instance().addBatchJob(BankSyncHandlerBatchJob.class, 500, param);
        }
    }

}
