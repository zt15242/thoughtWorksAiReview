package other.tw.business.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.service.CustomConfigService;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectMetaReq {

    private static final Logger logger = LoggerFactory.getLogger();

    private static final Integer RESULT_CODE = 200;

    public static ObjectMetaReq instance() throws IOException, XsyHttpException {
        return new ObjectMetaReq();
    }

    private Map<String, List<PickOption>> objectAllPicks;

    Map<String, String> objectApiKeyMap;

    public Map<String, String> objectApiNameMap;

    Map<String, String> objectColumnTypeMap;

    Map<String, Boolean> objectColumnIsUpdateMap;

    public Map<String, String> glbPickMap;

    public void initGlbPickMap() {
        try {
            Map<String, String> glbPickMap1 = new HashMap<>();
            RkhdHttpClient client = RkhdHttpClient.instance();
            RkhdHttpData data = new RkhdHttpData();
            data.setCall_type("GET");
            data.setCallString("/rest/metadata/v2.0/settings/globalPicks");
            String request = client.performRequest(data);
            logger.info(request);
            JSONObject jsonObject = JSONObject.parseObject(request);
            JSONObject data1 = jsonObject.getJSONObject("data");
            JSONArray records = data1.getJSONArray("records");
            for (int i = 0; i < records.size(); i++) {
                String label = records.getJSONObject(i).getString("label");
                String apikey = records.getJSONObject(i).getString("apiKey");
                glbPickMap1.put(label, apikey);
            }
            glbPickMap = glbPickMap1;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ObjectMetaReq getObjectAllMeta(String objectApiKey) throws IOException, XsyHttpException {
        Map<String, String> configProperties = null;
        try {
            CustomConfigService customConfigService = CustomConfigService.instance();
            configProperties = customConfigService.getConfigSet("rkhd_properties");
        } catch (Exception e) {
            logger.error("获取rkhd_properties配置文件异常！");
        }
        ObjectMetaReq objectMetaReq = new ObjectMetaReq();
        if (configProperties != null) {
            logger.info("rkhd_properties配置文件的参数：" + configProperties);
            boolean isLocal = configProperties.get("isLocal") != null ? Boolean.parseBoolean(configProperties.get("isLocal")) : true;
            //线上
            if (!isLocal) {
                RkhdHttpClient client = RkhdHttpClient.instance();
                RkhdHttpData data = new RkhdHttpData();
                data.setCall_type("GET");
                data.setCallString("/rest/data/v2.0/xobjects/" + objectApiKey + "/description");
                objectMetaReq = client.execute(data, (dataString) -> {
                    JSONObject responseObject = JSON.parseObject(dataString);
                    if (RESULT_CODE.equals(responseObject.getIntValue("code"))) {
                        JSONObject responseObjectData = (JSONObject) responseObject.get("data");
                        JSONArray fields = (JSONArray) responseObjectData.get("fields");
                        objectAllPicks = new HashMap<>();
                        objectApiKeyMap = new HashMap<>();
                        objectApiNameMap = new HashMap<>();
                        objectColumnTypeMap = new HashMap<>();
                        objectColumnIsUpdateMap = new HashMap<>();
                        for (Object item : fields) {
                            JSONObject itemJo = (JSONObject) item;
                            if (itemJo.get("type") != null) {
                                if ("picklist".equals(itemJo.get("type"))) {
                                    JSONArray selectitem = (JSONArray) itemJo.get("selectitem");
                                    if (selectitem != null && selectitem.size() > 0) {
                                        List<PickOption> pickOptionList = selectitem.stream().map(itemo -> {
                                            JSONObject itemojo = (JSONObject) itemo;
                                            PickOption pickOption = new PickOption();
                                            pickOption.setOptionCode(itemojo.getIntValue("value"));
                                            pickOption.setOptionLabel(itemojo.getString("label"));
                                            pickOption.setOptionApiKey(itemojo.getString("apiKey"));
                                            return pickOption;
                                        }).collect(Collectors.toList());
                                        objectAllPicks.put(itemJo.getString("apiKey"), pickOptionList);
                                        objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                                    }
                                }
                                if ("multipicklist".equals(itemJo.get("type"))) {
                                    JSONArray checkitem = (JSONArray) itemJo.get("checkitem");
                                    if (checkitem != null && checkitem.size() > 0) {
                                        List<PickOption> pickOptionList = checkitem.stream().map(itemo -> {
                                            JSONObject itemojo = (JSONObject) itemo;
                                            PickOption pickOption = new PickOption();
                                            pickOption.setOptionCode(itemojo.getIntValue("value"));
                                            pickOption.setOptionLabel(itemojo.getString("label"));
                                            pickOption.setOptionApiKey(itemojo.getString("apiKey"));
                                            return pickOption;
                                        }).collect(Collectors.toList());
                                        objectAllPicks.put(itemJo.getString("apiKey"), pickOptionList);
                                        objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                                    }
                                }
                                if ("reference".equals(itemJo.get("type"))) {
                                    objectApiKeyMap.put(itemJo.getString("apiKey"), itemJo.getJSONObject("referTo").getString("apiKey"));
                                    objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                                }
                                objectColumnTypeMap.put(itemJo.getString("apiKey"), String.valueOf(itemJo.get("type")));
                            }
                            objectColumnIsUpdateMap.put(itemJo.getString("apiKey"), itemJo.getBoolean("updateable"));
                        }
                        return this;
                    } else {
                        throw new CustomException("获取所有选项失败：objectApiKey:" + objectApiKey);
                    }
                });
            }
        } else {
            RkhdHttpClient client = RkhdHttpClient.instance();
            RkhdHttpData data = new RkhdHttpData();
            data.setCall_type("GET");
            data.setCallString("/rest/data/v2.0/xobjects/" + objectApiKey + "/description");
            objectMetaReq = client.execute(data, (dataString) -> {
                JSONObject responseObject = JSON.parseObject(dataString);
                if (RESULT_CODE.equals(responseObject.getIntValue("code"))) {
                    JSONObject responseObjectData = (JSONObject) responseObject.get("data");
                    JSONArray fields = (JSONArray) responseObjectData.get("fields");
                    objectAllPicks = new HashMap<>();
                    objectApiKeyMap = new HashMap<>();
                    objectApiNameMap = new HashMap<>();
                    objectColumnTypeMap = new HashMap<>();
                    objectColumnIsUpdateMap = new HashMap<>();
                    for (Object item : fields) {
                        JSONObject itemJo = (JSONObject) item;
                        if (itemJo.get("type") != null) {
                            if ("picklist".equals(itemJo.get("type"))) {
                                JSONArray selectitem = (JSONArray) itemJo.get("selectitem");
                                if (selectitem != null && selectitem.size() > 0) {
                                    List<PickOption> pickOptionList = selectitem.stream().map(itemo -> {
                                        JSONObject itemojo = (JSONObject) itemo;
                                        PickOption pickOption = new PickOption();
                                        pickOption.setOptionCode(itemojo.getIntValue("value"));
                                        pickOption.setOptionLabel(itemojo.getString("label"));
                                        pickOption.setOptionApiKey(itemojo.getString("apiKey"));
                                        return pickOption;
                                    }).collect(Collectors.toList());
                                    objectAllPicks.put(itemJo.getString("apiKey"), pickOptionList);
                                    objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                                }
                            }
                            if ("multipicklist".equals(itemJo.get("type"))) {
                                JSONArray checkitem = (JSONArray) itemJo.get("checkitem");
                                if (checkitem != null && checkitem.size() > 0) {
                                    List<PickOption> pickOptionList = checkitem.stream().map(itemo -> {
                                        JSONObject itemojo = (JSONObject) itemo;
                                        PickOption pickOption = new PickOption();
                                        pickOption.setOptionCode(itemojo.getIntValue("value"));
                                        pickOption.setOptionLabel(itemojo.getString("label"));
                                        pickOption.setOptionApiKey(itemojo.getString("apiKey"));
                                        return pickOption;
                                    }).collect(Collectors.toList());
                                    objectAllPicks.put(itemJo.getString("apiKey"), pickOptionList);
                                    objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                                }
                            }
                            if ("reference".equals(itemJo.get("type"))) {
                                objectApiKeyMap.put(itemJo.getString("apiKey"), itemJo.getJSONObject("referTo").getString("apiKey"));
                                objectApiNameMap.put(itemJo.getString("apiKey"), itemJo.getString("label"));
                            }
                            objectColumnTypeMap.put(itemJo.getString("apiKey"), String.valueOf(itemJo.get("type")));
                        }
                        objectColumnIsUpdateMap.put(itemJo.getString("apiKey"), itemJo.getBoolean("updateable"));
                    }
                    return this;
                } else {
                    throw new CustomException("获取所有选项失败：objectApiKey:" + objectApiKey);
                }
            });
        }
        return objectMetaReq;
    }

    public Integer getOptionByApiKey(String objectPickApiKey, String optionApiKey) {
        List<PickOption> pickOptionList = objectAllPicks.get(objectPickApiKey);
        if (pickOptionList != null && pickOptionList.size() > 0) {
            PickOption pickOption = pickOptionList.stream().filter(objectPick -> objectPick.getOptionApiKey().equals(optionApiKey)).findAny().orElse(null);
            if (pickOption != null) {
                return pickOption.getOptionCode();
            }
        }
        throw new CustomException("选项无该optionLabel：objectPickApiKey：" + objectPickApiKey + ";optionLabel:" + optionApiKey);
    }

    public String getOptionByCode(String objectPickApiKey, Integer optionCode) {
        List<PickOption> pickOptionList = objectAllPicks.get(objectPickApiKey);
        if (pickOptionList != null && pickOptionList.size() > 0) {
            PickOption pickOption = pickOptionList.stream().filter(objectPick -> objectPick.getOptionCode().equals(optionCode)).findAny().orElse(null);
            if (pickOption != null) {
                return pickOption.getOptionApiKey();
            }
        }
        throw new CustomException("选项无该optionCode：objectPickApiKey：" + objectPickApiKey + ";optionCode:" + optionCode);
    }

    public String getReferenceApiKey(String apiKey) {
        String referapiKey = objectApiKeyMap.get(apiKey);
        if (referapiKey == null) {
            throw new CustomException("没有连接该对象：apiKey：" + apiKey);
        }
        return referapiKey;
    }

    public String getObjectLabel(String apiKey) {
        String labelName = objectApiNameMap.get(apiKey);
        if (labelName == null) {
            throw new CustomException("没有连接该对象：labelName：" + labelName);
        }
        return labelName;
    }

    public String getColumnTypeByApiKey(String apiKey) {
        String columnType = objectColumnTypeMap.get(apiKey);
        /*if(columnType == null) {
            throw new CustomException("没有连接该对象：apiKey：" + apiKey);
        }*/
        return columnType;
    }

    public String getApiKeyByColumnType(String columnType) {
        if (objectColumnTypeMap == null) {
            throw new CustomException("没有连接该对象：columnType：" + columnType);
        }
        String apiKey = null;
        for (Map.Entry<String, String> entry : objectColumnTypeMap.entrySet()) {
            if (columnType.equals(entry.getValue())) {
                apiKey = entry.getKey();
            }
        }
        return apiKey;
    }

    public Boolean getIsUpdateByApiKey(String apiKey) {
        Boolean isUpdate = objectColumnIsUpdateMap.get(apiKey);
        if (isUpdate == null) {
            throw new CustomException("没有连接该对象：apiKey：" + apiKey);
        }
        return isUpdate;
    }

    public Map<String, List<PickOption>> getAllPicks() {
        return objectAllPicks;
    }
    public class CustomException extends RuntimeException{

        private static final long serialVersionUID = 1L;

        public CustomException(String message) {
            super(message);
        }

        public CustomException(Throwable cause) {
            super(cause);
        }

        public CustomException(String message, Throwable cause) {
            super(message, cause);
        }

        public CustomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
    public class PickOption implements Serializable {

        private Boolean isActive;

        private String optionLabel;

        private Integer optionCode;

        private String optionApiKey;
        public Boolean getActive() {
            return isActive;
        }

        public void setActive(Boolean active) {
            isActive = active;
        }

        public String getOptionLabel() {
            return optionLabel;
        }

        public void setOptionLabel(String optionLabel) {
            this.optionLabel = optionLabel;
        }

        public Integer getOptionCode() {
            return optionCode;
        }

        public void setOptionCode(Integer optionCode) {
            this.optionCode = optionCode;
        }

        public String getOptionApiKey() {
            return optionApiKey;
        }

        public void setOptionApiKey(String optionApiKey) {
            this.optionApiKey = optionApiKey;
        }

        @Override
        public String toString() {
            return "PickOption{" +
                    "isActive=" + isActive +
                    ", optionLabel='" + optionLabel + '\'' +
                    ", optionCode=" + optionCode +
                    ", optionApiKey='" + optionApiKey + '\'' +
                    '}';
        }
    }
}
