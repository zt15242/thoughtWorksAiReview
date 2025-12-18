package other.tw.business.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rkhd.platform.sdk.exception.ApiEntityServiceException;
import com.rkhd.platform.sdk.exception.ScriptBusinessException;
import com.rkhd.platform.sdk.exception.XsyHttpException;
import com.rkhd.platform.sdk.http.RkhdHttpClient;
import com.rkhd.platform.sdk.http.RkhdHttpData;
import com.rkhd.platform.sdk.http.handler.ResponseBodyHandlers;
import com.rkhd.platform.sdk.log.Logger;
import com.rkhd.platform.sdk.log.LoggerFactory;
import com.rkhd.platform.sdk.model.BatchOperateResult;
import com.rkhd.platform.sdk.model.OperateResult;
import com.rkhd.platform.sdk.model.QueryResult;
import com.rkhd.platform.sdk.model.XObject;
import com.rkhd.platform.sdk.service.XObjectService;
import com.rkhd.platform.sdk.service.XoqlService;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NeoCrmUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger();

    public static <T> String convertListToString(List<T> strList) {
        StringBuffer sb = new StringBuffer();
        if (strList != null && !strList.isEmpty()) {
            for (int i = 0; i < strList.size(); i++) {
                if (i == 0) {
                    sb.append("'").append(strList.get(i)).append("'");
                } else {
                    sb.append(",").append("'").append(strList.get(i)).append("'");
                }
            }
        }
        return sb.toString();

    }

    public static <T> String convertSetToString(Set<T> strSet) {
        return convertListToString(new ArrayList<>(strSet));
    }


    public static <T extends XObject> Map<Long, T> objectJsonListToIdMap(List<JSONObject> objJsonList, Class<T> clazz) {
        Map<Long, T> idObjMap = new HashMap<>(objJsonList.size());

        for (JSONObject jsonObject : objJsonList) {
            Long id = jsonObject.getLong("id");
            if (id != null) {
                T obj = jsonObject.toJavaObject(clazz);
                idObjMap.put(id, obj);
            }
        }

        return idObjMap;
    }

    public static List<JSONObject> getObjectJsonList(List<Long> ids, String fields, String objApiName) throws ApiEntityServiceException {
        List<JSONObject> jsonList = new ArrayList<>();

        if (ids.isEmpty()) {
            LOGGER.error("ids is empty");
            return jsonList;
        }

        String sql = "SELECT id, " + fields + " FROM " + objApiName + " WHERE id IN (" + convertListToString(ids) + ")";
        LOGGER.info("getObjectJsonList Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            jsonList = query.getRecords();
        } else {
            LOGGER.error("getObjectJsonList query error: " + query.getErrorMessage());
        }

        return jsonList;
    }
    public static <T> List<T> executeQuery(String sql, Class<T> clazz) throws ApiEntityServiceException {
        QueryResult<JSONObject> queryResult = XoqlService.instance().query(sql, true, true);
        return queryResult.getRecords().stream()
                // 使用传入的 Class 类型参数将 JSONObject 转换为指定类型的对象
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }
    public static <T extends XObject> String update(List<T> updateList, String msg) {
        LOGGER.error(msg + "批量更新====");
        String errMsg = msg;
        if (!updateList.isEmpty()) {
            try {
                BatchOperateResult updateResult = XObjectService.instance().update(updateList, false, true);
                if (updateResult.getSuccess()) {
                    LOGGER.info(msg + "批量更新成功");
                    for (OperateResult operate : updateResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "更新失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "更新失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "更新失败 Code =>" + operate.getCode());
                        }
                    }
                } else {
                    errMsg += "批量更新失败: " + updateResult.getErrorMessage() + "\n";
                    LOGGER.error(msg + "批量更新失败 ErrorMessage =>" + updateResult.getErrorMessage());
                    LOGGER.error(msg + "批量更新失败 Code =>" + updateResult.getCode());
                    for (OperateResult operate : updateResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "更新失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + operate.getDataId() + "更新失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + operate.getDataId() + "更新失败 Code =>" + operate.getCode());
                        }
                    }
                }
                LOGGER.info(msg + "批量更新数量 =>" + updateList.size());
            } catch (Exception e) {
                errMsg += "更新失败 Exception: " + e.getMessage();
                LOGGER.error(msg + "更新 Exception =>" + e.getMessage());
                LOGGER.error(msg + "更新 Exception =>" + Arrays.toString(e.getStackTrace()));
            }
        } else {
            LOGGER.error(msg + "更新列表为空====");
        }

        return errMsg;
    }

    public static <T extends XObject> String update(List<T> updateList,boolean partialSuccess, String msg) {
        LOGGER.info(msg + "批量更新====");
        String errMsg = msg;
        if (!updateList.isEmpty()) {
            try {
                BatchOperateResult updateResult = XObjectService.instance().update(updateList, partialSuccess, true);
                if (updateResult.getSuccess()) {
                    LOGGER.info(msg + "批量更新成功");
                    for (OperateResult operate : updateResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "更新失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "更新失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "更新失败 Code =>" + operate.getCode());
                        }
                    }
                } else {
                    errMsg += "批量更新失败: " + updateResult.getErrorMessage() + "\n";
                    LOGGER.error(msg + "批量更新失败 ErrorMessage =>" + updateResult.getErrorMessage());
                    LOGGER.error(msg + "批量更新失败 Code =>" + updateResult.getCode());
                    for (OperateResult operate : updateResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "更新失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + operate.getDataId() + "更新失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + operate.getDataId() + "更新失败 Code =>" + operate.getCode());
                        }
                    }
                }
                LOGGER.info(msg + "批量更新数量 =>" + updateList.size());
            } catch (Exception e) {
                errMsg += "更新失败 Exception: " + e.getMessage();
                LOGGER.error(msg + "更新 Exception =>" + e.getMessage());
                LOGGER.error(msg + "更新 Exception =>" + Arrays.toString(e.getStackTrace()));
            }
        } else {
            LOGGER.error(msg + "更新列表为空====");
        }

        return errMsg;
    }


    public static <T extends XObject> String insert(List<T> insertList, String msg) {
        LOGGER.info(msg + "批量创建====");
        String errMsg = msg;
        if (!insertList.isEmpty()) {
            try {
                BatchOperateResult insertResult = XObjectService.instance().insert(insertList, false, true);
                if (insertResult.getSuccess()) {
                    LOGGER.info(msg + "批量创建成功");
                    for (OperateResult operate : insertResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "创建失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "创建失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "创建失败 Code =>" + operate.getCode());
                        }
                    }
                } else {
                    errMsg += "批量创建失败: " + insertResult.getErrorMessage() + "\n";
                    LOGGER.error(msg + "批量创建失败 ErrorMessage =>" + insertResult.getErrorMessage());
                    LOGGER.error(msg + "批量创建失败 Code =>" + insertResult.getCode());
                    for (OperateResult operate : insertResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "创建失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "创建失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "创建失败 Code =>" + operate.getCode());
                        }
                    }
                }
                LOGGER.info(msg + "批量创建数量 =>" + insertList.size());
            } catch (Exception e) {
                errMsg += "创建失败 Exception: " + e.getMessage();
                LOGGER.error(msg + "创建 Exception =>" + e.getMessage());
                LOGGER.error(msg + "创建 Exception =>" + Arrays.toString(e.getStackTrace()));
            }
        } else {
            LOGGER.error(msg + "创建列表为空====");
        }

        return errMsg;
    }

    public static <T extends XObject> String insert(List<T> insertList, boolean partialSuccess, String msg) {
        LOGGER.info(msg + "批量创建====");
        String errMsg = msg;
        if (!insertList.isEmpty()) {
            try {
                BatchOperateResult insertResult = XObjectService.instance().insert(insertList, partialSuccess, true);
                if (insertResult.getSuccess()) {
                    LOGGER.info(msg + "批量创建成功");
                    for (OperateResult operate : insertResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "创建失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "创建失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "创建失败 Code =>" + operate.getCode());
                        }
                    }
                } else {
                    errMsg += "批量创建失败: " + insertResult.getErrorMessage() + "\n";
                    LOGGER.error(msg + "批量创建失败 ErrorMessage =>" + insertResult.getErrorMessage());
                    LOGGER.error(msg + "批量创建失败 Code =>" + insertResult.getCode());
                    for (OperateResult operate : insertResult.getOperateResults()) {
                        if (!operate.getSuccess()) {
                            errMsg += "创建失败: " + operate.getErrorMessage() + "\n";
                            LOGGER.error(msg + "创建失败 ErrorMessage =>" + operate.getErrorMessage());
                            LOGGER.error(msg + "创建失败 Code =>" + operate.getCode());
                        }
                    }
                }
                LOGGER.info(msg + "批量创建数量 =>" + insertList.size());
            } catch (Exception e) {
                errMsg += "创建失败 Exception: " + e.getMessage();
                LOGGER.error(msg + "创建 Exception =>" + e.getMessage());
                LOGGER.error(msg + "创建 Exception =>" + Arrays.toString(e.getStackTrace()));
            }
        } else {
            LOGGER.error(msg + "创建列表为空====");
        }

        return errMsg;
    }

    public static <T extends XObject> String upsert(List<T> upsertList, String objApiName, Long entityType, String msg) throws ApiEntityServiceException {
        LOGGER.info(msg + "创建更新====");
        LOGGER.error(msg + " 创建更新数量 => "+upsertList.size());
        String errMsg = msg;

        if (upsertList.isEmpty()) {
            LOGGER.error(msg + "创建更新列表为空====");
            return errMsg;
        }

        Set<Long> existedIds = new HashSet<>();

        for (T xObject : upsertList) {
            existedIds.add(xObject.getId());
        }

        String sql = "SELECT id FROM " + objApiName + " WHERE id IN (" + convertListToString(new ArrayList<>(existedIds)) + ")";
        LOGGER.info("existed id Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            existedIds.clear();
            for (JSONObject record : query.getRecords()) {
                existedIds.add(record.getLong("id"));
            }
        } else {
            errMsg += " existed id query error: " + query.getErrorMessage();
            LOGGER.error("existed id query error: " + query.getErrorMessage());
            return errMsg;
        }

        List<T> insertList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();

        for (T obj : upsertList) {
            if (existedIds.contains(obj.getId())) {
                updateList.add(obj);
            } else {
                obj.setId(null);
                if (obj.getAttribute("entityType") == null) {
                    obj.setAttribute("entityType", entityType);
                }
                insertList.add(obj);
            }
        }

        errMsg += insert(insertList, msg);
        errMsg += update(updateList, msg);

        return errMsg;
    }

    public static <T extends XObject> String upsertWithKeyField(List<T> upsertList, String objApiName, String keyFieldApiName, Long entityType, String msg) throws ApiEntityServiceException {
        LOGGER.info(msg + "创建更新====");
        LOGGER.error(msg + " 创建更新数量 => "+upsertList.size());
        String errMsg = msg;

        if (upsertList.isEmpty()) {
            LOGGER.error(msg + "创建更新列表为空====");
            return errMsg;
        }

        Set<String> existed = new HashSet<>();

        for (T xObject : upsertList) {
            existed.add(xObject.getAttribute(keyFieldApiName).toString());
        }

        Map<String, Long> idMap = new HashMap<>();

        String sql = "SELECT id, " + keyFieldApiName + " FROM " + objApiName + " WHERE " + keyFieldApiName + " IN (" + convertListToString(new ArrayList<>(existed)) + ")";
        LOGGER.info("existed " + keyFieldApiName + " Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            existed.clear();
            for (JSONObject record : query.getRecords()) {
                existed.add(record.get(keyFieldApiName).toString());
                idMap.put(record.get(keyFieldApiName).toString(), record.getLong("id"));
            }
        } else {
            errMsg += " existed " + keyFieldApiName + " query error: " + query.getErrorMessage();
            LOGGER.error("existed " + keyFieldApiName + " query error: " + query.getErrorMessage());
            return errMsg;
        }

        List<T> insertList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();

        for (T obj : upsertList) {
            if (idMap.containsKey(obj.getAttribute(keyFieldApiName).toString())) {
                obj.setId(idMap.get(obj.getAttribute(keyFieldApiName).toString()));
                updateList.add(obj);
            } else {
                obj.setId(null);
                if (obj.getAttribute("entityType") == null) {
                    obj.setAttribute("entityType", entityType);
                }
                insertList.add(obj);
            }
        }

        errMsg += insert(insertList, msg);
        errMsg += update(updateList, msg);

        return errMsg;
    }

    public static <T extends XObject> String upsertWithKeyFieldWhenFieldChanged(List<T> upsertList, Class<T> clazz, boolean partialSuccess, String keyFieldApiName, List<String> checkChangedFieldList, Long entityType, String msg) throws ApiEntityServiceException {
        LOGGER.info(msg + " 创建更新====");
        LOGGER.error(msg + " 创建更新数量 => "+upsertList.size());
        String errMsg = msg;

        if (upsertList.isEmpty()) {
            LOGGER.error(msg + " 创建更新列表为空====");
            return errMsg;
        }

        LOGGER.info(msg +" checkChangedFieldList==="+checkChangedFieldList);

        String objApiName =upsertList.get(0).getApiKey();

        Set<String> existed = new HashSet<>();
        Map<String, T> existedObjMap = new HashMap<>();

        for (T xObject : upsertList) {
            existed.add(xObject.getAttribute(keyFieldApiName).toString());
        }
        String checkChangedFieldListStr = checkChangedFieldList.toString();
        checkChangedFieldListStr = checkChangedFieldListStr.substring(1,checkChangedFieldListStr.length()-1);

        String sql = "SELECT id, " + keyFieldApiName +", "+checkChangedFieldListStr+ " FROM " + objApiName + " WHERE " + keyFieldApiName + " IN (" + convertListToString(new ArrayList<>(existed)) + ")";
        LOGGER.error(msg+" existed " + keyFieldApiName + " Sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            existed.clear();
            for (JSONObject record : query.getRecords()) {
                existed.add(record.get(keyFieldApiName).toString());
                existedObjMap.put(record.get(keyFieldApiName).toString(), JSON.toJavaObject(record,clazz));
            }
        } else {
            errMsg += " existed " + keyFieldApiName + " query error: " + query.getErrorMessage();
            LOGGER.error(msg+" existed " + keyFieldApiName + " query error: " + query.getErrorMessage());
            return errMsg;
        }

        List<T> insertList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();

        for (T obj : upsertList) {
            if (existedObjMap.containsKey(obj.getAttribute(keyFieldApiName).toString())) {
                T existedObj = existedObjMap.get(obj.getAttribute(keyFieldApiName).toString());
                LOGGER.error("existedObj => "+JSON.toJSONString(existedObj));
                for (String changedField : checkChangedFieldList) {
                    if(!Objects.equals(obj.getAttribute(changedField),existedObj.getAttribute(changedField))){
                        LOGGER.error("data "+existedObj.getId()+" field "+changedField+" changed, from "+existedObj.getAttribute(changedField)+ " to "+obj.getAttribute(changedField));
                        obj.setId(existedObj.getId());
                        updateList.add(obj);
                        LOGGER.error("update obj => "+JSON.toJSONString(obj));
                        break;
                    }
                }
//                LOGGER.error("no need to update obj => "+JSON.toJSONString(obj));
            } else {
                obj.setId(null);
                if (obj.getAttribute("entityType") == null) {
                    obj.setAttribute("entityType", entityType);
                }
                insertList.add(obj);
                LOGGER.error("insert obj => "+JSON.toJSONString(obj));
            }
        }

        errMsg += insert(insertList,partialSuccess, msg);
        errMsg += update(updateList,partialSuccess, msg);

        return errMsg;
    }


    /**
     * xoql 查询接口
     *
     * @param client
     * @param sql
     * @return
     * @throws ScriptBusinessException
     * @throws IOException
     * @throws ApiEntityServiceException
     * @throws InterruptedException
     */
    public static JSONArray xoql(RkhdHttpClient client, String sql) throws ScriptBusinessException, XsyHttpException, InterruptedException {

        JSONArray records = new JSONArray();
        RkhdHttpData data = new RkhdHttpData();
        String url = "/rest/data/v2.0/query/xoql";
        LOGGER.debug("url--->" + url);
        LOGGER.debug("sql:" + sql);
        data.setCall_type("Post");
        data.setCallString(url);
        data.putFormData("xoql", sql);
        String result = client.execute(data, ResponseBodyHandlers.ofString());
        LOGGER.debug(result);

        if (StringUtils.isNotBlank(result)) {
            JSONObject resultJson = JSONObject.parseObject(result);
            String code = resultJson.getString("code");

            if (code.equals("200")) {
                JSONObject resultobj = resultJson.getJSONObject("data");
                int count = resultobj.getIntValue("count");
                if (count > 0) {
                    records.addAll(resultobj.getJSONArray("records"));
                }
            } else if (code.equals("1020025")) {
                //因为返回信息为频率过高所以暂停一秒钟
                Thread.sleep(1000);
                records = xoql(client, sql);
            } else {
                throw new ScriptBusinessException(sql + "错误:-->" + code + "|错误原因:" + resultJson.getString("msg"));
            }
        } else {
            throw new ScriptBusinessException(sql + "错误:-->返回为空");
        }
        return records;
    }

    /**
     * V2.0描述接口，获取单选和多选的值字段的键值对
     *
     * @param client RkhdHttpClient
     * @param source "apikey"或者"label"|apikey为单选或者多选配置中的【API名称】|label为单选或者多选配置中的【选项名称】
     * @param apiKey 对象名
     * @param kvMap  (apiKeyName(label:value) ) 或者 (apiKeyName(apikey:value) )
     * @param vkMap  (apiKeyName(value:label) ) 或者 (apiKeyName(value:apikey) )   结果集合和kvMap相反
     * @throws ScriptBusinessException
     */
    public static void getPicklistValue(RkhdHttpClient client, String apiKey, String source, Map<String, Map<String, Integer>> kvMap, Map<String, Map<Integer, String>> vkMap)
            throws ScriptBusinessException {
        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/xobjects/" + apiKey + "/description").build();
            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
            if (200 != result.getInteger("code")) {
                throw new ScriptBusinessException(result.getString("msg"));
            }

            for (Object obj : result.getJSONObject("data").getJSONArray("fields")) {
                JSONObject jsonObject = (JSONObject) obj;
                // 将单选类型的描述保存起来
                if ("picklist".equals(jsonObject.getString("type"))) {
                    Map<String, Integer> keyMap = new HashMap<String, Integer>();
                    Map<Integer, String> valueMap = new HashMap<Integer, String>();
                    JSONArray array = jsonObject.getJSONArray("selectitem");
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        keyMap.put(object.getString(source), object.getInteger("value"));
                        valueMap.put(object.getInteger("value"), object.getString(source));
                    }
                    kvMap.put(jsonObject.getString("apiKey"), keyMap);
                    vkMap.put(jsonObject.getString("apiKey"), valueMap);
                }
                // 将多选类型的描述保存起来
                if ("multipicklist".equals(jsonObject.getString("type"))) {
                    Map<String, Integer> keyMap = new HashMap<String, Integer>();
                    Map<Integer, String> valueMap = new HashMap<Integer, String>();
                    JSONArray array = jsonObject.getJSONArray("checkitem");
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        keyMap.put(object.getString(source), object.getInteger("value"));
                        valueMap.put(object.getInteger("value"), object.getString(source));
                    }
                    kvMap.put(jsonObject.getString("apiKey"), keyMap);
                    vkMap.put(jsonObject.getString("apiKey"), valueMap);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            LOGGER.error(e.getMessage(), e);
            throw new ScriptBusinessException(e);
        }
    }


    public static Integer getPicklistFieldValue(String fieldApiKey, String label, Map<String, Map<String, Integer>> kvMap) {
        if (kvMap.containsKey(fieldApiKey) && kvMap.get(fieldApiKey).containsKey(label)) {
            return kvMap.get(fieldApiKey).get(label);
        } else {
            return null;
        }

    }

    public static String getPicklistFieldLabel(String fieldApiKey, Integer value, Map<String, Map<Integer, String>> vkMap) {
        if (vkMap.containsKey(fieldApiKey) && vkMap.get(fieldApiKey).containsKey(value)) {
            return vkMap.get(fieldApiKey).get(value);
        } else {
            return null;
        }

    }

    /**
     * 根据ApiKey(对象名称)获取entityType（业务类型）的id
     *
     * @param apiKey
     * @param entityApiKey
     * @return entityTypeId
     */
    public static Long getEntityTypesId(RkhdHttpClient client, String apiKey, String entityApiKey) {
        Long entityTypesId = null;
        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/xobjects/" + apiKey + "/busiType").build();
            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
            if (200 != result.getInteger("code")) {
                throw new ScriptBusinessException(result.getString("msg"));
            }

            for (Object obj : result.getJSONObject("data").getJSONArray("records")) {
                JSONObject jsonObject = (JSONObject) obj;
                if (entityApiKey.equals(jsonObject.getString("apiKey"))) {
                    entityTypesId = jsonObject.getLong("id");
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            LOGGER.error("error->" + e.toString());
        }
        return entityTypesId;
    }

    public static Map<Long, String> getEntityTypeMap(RkhdHttpClient client, String objApi) {
        LOGGER.info("getEntityTypeMap===");
        Map<Long, String> typeMap = new HashMap<>();
        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/xobjects/" + objApi + "/busiType").build();
            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
            if (200 != result.getInteger("code")) {
                throw new ScriptBusinessException(result.getString("msg"));
            }

            for (Object obj : result.getJSONObject("data").getJSONArray("records")) {
                JSONObject jsonObject = (JSONObject) obj;
                typeMap.put(jsonObject.getLong("id"), jsonObject.getString("label"));
            }
        } catch (Exception e) {
            // TODO: handle exception
            LOGGER.error("getEntityTypeMap Exception message =>" + e.getMessage());
            LOGGER.error("getEntityTypeMap Exception =>" + Arrays.toString(e.getStackTrace()));
        }
        return typeMap;
    }

    //查询用户职能
    public static JSONArray getResponsibilities(long userId) throws IOException {
        LOGGER.info("查询用户职能===");
        RkhdHttpClient client = RkhdHttpClient.instance();
        JSONArray records = new JSONArray();

        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/privileges/users/"+userId+"/responsibilities").build();
            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());

            records = result.getJSONObject("data").getJSONArray("records");
            System.out.println(result);
        } catch (Exception e) {
            LOGGER.error("查询用户职能 Exception message =>" + e.getMessage());
            LOGGER.error("查询用户职能 Exception =>" + Arrays.toString(e.getStackTrace()));
        }
        return records;
    }

    public static List<JSONObject> query(String sql, String msg,  boolean useSimpleCode) throws ApiEntityServiceException {
        LOGGER.info(msg + " query sql: " + sql);

        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, useSimpleCode);
        if (query.getSuccess()) {
            return query.getRecords();
        } else {
            LOGGER.error(msg + " query code: " + query.getCode());
            LOGGER.error(msg + " query error: " + query.getErrorMessage());
        }

        return null;
    }

    public static List<JSONObject> query(String sql, boolean useSimpleCode) throws ApiEntityServiceException {
        LOGGER.info( "query sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, useSimpleCode);
        if (query.getSuccess()) {
            return query.getRecords();
        } else {
            LOGGER.error("query code: " + query.getCode());
            LOGGER.error("query error: " + query.getErrorMessage());
        }

        return null;
    }

    public static <T extends XObject> List<T> query(String sql, Class<T> clazz, String msg) throws ApiEntityServiceException {
        LOGGER.info(msg + " query sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true, true);
        if (query.getSuccess()) {
            return query.getRecords().stream()
                    // 使用传入的 Class 类型参数将 JSONObject 转换为指定类型的对象
                    .map(jsonObject -> jsonObject.toJavaObject(clazz))
                    .collect(Collectors.toList());
        } else {
            LOGGER.error(msg + " query code: " + query.getCode());
            LOGGER.error(msg + " query error: " + query.getErrorMessage());
        }

        return null;
    }

    public static <T extends XObject> List<T> query(String sql, Class<T> clazz) throws ApiEntityServiceException {
        LOGGER.info("query sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true,true);
        if (query.getSuccess()) {
            return query.getRecords().stream()
                    // 使用传入的 Class 类型参数将 JSONObject 转换为指定类型的对象
                    .map(jsonObject -> jsonObject.toJavaObject(clazz))
                    .collect(Collectors.toList());
        } else {
            LOGGER.error( "query code: " + query.getCode());
            LOGGER.error("query error: " + query.getErrorMessage());
        }

        return null;
    }

    public static <T extends XObject> Map<String,T> query(String sql,String fieldApiName, Class<T> clazz,String msg) throws ApiEntityServiceException {
        Map<String,T> map = new HashMap<>();
        LOGGER.info(msg+" query sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true,true);
        if (query.getSuccess()) {
            for(JSONObject obj : query.getRecords()) {
                T xObject = obj.toJavaObject(clazz);
                map.put(xObject.getAttribute(fieldApiName),xObject);
            }
        } else {
            LOGGER.error( msg+" query code: " + query.getCode());
            LOGGER.error(msg+" query error: " + query.getErrorMessage());
        }

        return map;
    }

    public static Map<String,JSONObject> query(String sql,String fieldApiName,String msg) throws ApiEntityServiceException {
        Map<String,JSONObject> map = new HashMap<>();
        LOGGER.info(msg+" query sql: " + sql);
        QueryResult<JSONObject> query = XoqlService.instance().query(sql, true,true);
        if (query.getSuccess()) {
            for(JSONObject obj : query.getRecords()) {
                map.put(obj.getString(fieldApiName),obj);
            }
        } else {
            LOGGER.error( msg+" query code: " + query.getCode());
            LOGGER.error(msg+" query error: " + query.getErrorMessage());
        }

        return map;
    }



    public <T extends XObject> Map<Object, List<T>> getFieldToObjectMap(Class<T> clazz, String mapKeyFieldApiName, String sql, String msg) throws ApiEntityServiceException {
        LOGGER.info("getFieldToObjectMap===");
        Map<Object, List<T>> map = new HashMap<>();

        List<JSONObject> records = query(sql, msg,  true);

        if (records == null || records.isEmpty()) {
            LOGGER.error(msg + " records is empty");
            return map;
        }

        for (JSONObject record : records) {
            T xObj = record.toJavaObject(clazz);
            if (!map.containsKey(xObj.getAttribute(mapKeyFieldApiName))) {
                map.put(xObj.getAttribute(mapKeyFieldApiName), new ArrayList<>());
            }
            map.get(xObj.getAttribute(mapKeyFieldApiName)).add(xObj);
        }

        return map;
    }

    public <T extends XObject> List<T> getObjectList(Class<T> clazz, String sql, String msg) throws ApiEntityServiceException {
        LOGGER.info("getObjectList===");
        List<T> list = new ArrayList<>();

        List<JSONObject> records = query(sql, msg, true);

        if (records == null || records.isEmpty()) {
            LOGGER.error(msg + " records is empty");
            return list;
        }

        for (JSONObject record : records) {
            T xObj = record.toJavaObject(clazz);
            list.add(xObj);
        }

        return list;
    }

    public static JSONObject getUserTaskLog(String entityApiKey, Long dataId, boolean stageFlg, Long userTaskLogId) throws IOException {

        RkhdHttpClient client = new RkhdHttpClient();
        LOGGER.info("getUserTaskLog===");
        try {
            RkhdHttpData data = RkhdHttpData.newBuilder().callType("GET")
                    .callString("/rest/data/v2.0/creekflow/history/filter?entityApiKey="+entityApiKey+"&dataId="+dataId+"&stageFlg="+stageFlg)
                    .build();

            JSONObject result = client.execute(data, ResponseBodyHandlers.ofJSON());
            System.out.println(result);
            if (200 != result.getInteger("code")) {
                throw new ScriptBusinessException(result.getString("msg"));
            }

            for (Object obj : result.getJSONArray("data")) {
                JSONObject jsonObject = (JSONObject) obj;
                if (jsonObject.getLong("id").equals(userTaskLogId)){
                    return jsonObject;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            LOGGER.error("getUserTaskLog Exception message =>" + e.getMessage());
            LOGGER.error("getUserTaskLog Exception =>" + Arrays.toString(e.getStackTrace()));
        }

        return new JSONObject();
    }

    public static <T extends XObject> void updateApprovalComment(T xObject, Long userTaskLogId, String commentFieldApiName) throws IOException {
        JSONObject taskLog = NeoCrmUtils.getUserTaskLog(xObject.getApiKey(),xObject.getId(),false,userTaskLogId);

        xObject.setAttribute(commentFieldApiName,taskLog.getString("opinion"));

        update(Collections.singletonList(xObject),"更新"+xObject.getApiKey()+"审批意见到"+commentFieldApiName);
    }

}
