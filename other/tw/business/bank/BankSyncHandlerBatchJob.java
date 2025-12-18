package other.tw.business.bank;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.data.model.Bank__c;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.BatchJobException;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.task.BatchJobPro;
import com.rkhd.platform.sdk.task.param.*;
import other.tw.business.service.ErpService;
import other.tw.business.util.NeoCrmUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BankSyncHandlerBatchJob implements BatchJobPro<SimpleMap> {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    @Override
    public BatchJobData prepare(PrepareParam prepareParam) throws BatchJobException {
        LOGGER.info("BankSyncHandlerBatchJob prepare is start!");
        LOGGER.info("BankSyncHandlerBatchJob prepareParam ==> "+prepareParam.toString());

        ErpService erpService = new ErpService();

        JSONObject filter = JSON.parseObject(prepareParam.get("filter"));
        int pageNo = Integer.parseInt(prepareParam.get("pageNo"));
        int pageSize = Integer.parseInt(prepareParam.get("pageSize"));

        List<SimpleMap> data = new ArrayList<>();
        try {
            data = erpService.getBanksSimpleMap(filter,pageNo,pageSize);
        } catch (InterruptedException | ApiEntityServiceException | ParseException e) {
            LOGGER.error("BankSyncHandlerBatchJob Exception message =>" + e.getMessage());
            LOGGER.error("BankSyncHandlerBatchJob Exception =>" + Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }

        BatchJobDataBuilder batchJobDataBuilder = new BatchJobDataBuilder();

        return batchJobDataBuilder
                .setDataList(data)
                .buildListData();
    }

    @Override
    public void execute(List<SimpleMap> list, BatchJobParam batchJobParam) {
        LOGGER.info("BankSyncHandlerBatchJob execute list ==> "+list.toString());
        try {
            if(!list.isEmpty()) {
                todo(list);
            }
        } catch (ApiEntityServiceException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish(BatchJobParam batchJobParam) {
        LOGGER.info("BankSyncHandlerBatchJob finish");
    }

    private void todo(List<SimpleMap> list) throws ApiEntityServiceException,ParseException {
        // 对数据做处理
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Bank__c> banks = new ArrayList<>();
        for (SimpleMap simpleMap : list) {
            Bank__c bank = new Bank__c();
            bank.setNumber__c(simpleMap.get("number"));
            bank.setName(simpleMap.get("name"));
            bank.setEnable__c(simpleMap.get("enable").equals("1"));
            bank.setName_eng__c(simpleMap.get("name_eng"));
            bank.setModifytime__c(sdf.parse(simpleMap.get("modifytime")).getTime());
            bank.setUnion_number__c(simpleMap.get("union_number"));
            bank.setAddress__c(simpleMap.get("address"));
            bank.setProvincetxt__c(simpleMap.get("provincetxt"));
            bank.setCitytxt__c(simpleMap.get("citytxt"));
            bank.setBankcatename__c(simpleMap.get("bankcatename"));

            String status = simpleMap.get("status");

            switch (status) {
                case "A":
                    bank.setStatus__c(1);
                    break;
                case "B":
                    bank.setStatus__c(2);
                    break;
                case "C":
                    bank.setStatus__c(3);
                    break;
            }

            banks.add(bank);
        }

        NeoCrmUtils.upsertWithKeyField(banks, "bank__c", "number__c", 4084436793545493L, "同步银行");
    }

}
