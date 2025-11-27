package com.evcharge.libsdk.genkigo;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 元气充SDK
 */
public class GenkigoSDK {

    //region 属性
    /**
     * 平台代码
     */
    private String PlatformCode;
    /**
     * 平台加密key
     */
    private String AppSecret;
    /**
     * RestAPI请求地址
     */
    private String RestAPIUrl = "http://consoleapi.genkigo.net";

    /**
     * 设置平台代码
     */
    public GenkigoSDK setPlatformCode(String platformCode) {
        PlatformCode = platformCode;
        return this;
    }

    /**
     * 设置平台App秘钥
     */
    public GenkigoSDK setAppSecret(String appSecret) {
        AppSecret = appSecret;
        return this;
    }

    /**
     * 设置RestAPI请求地址
     */
    public GenkigoSDK setRestAPIUrl(String restAPIUrl) {
        RestAPIUrl = restAPIUrl;
        return this;
    }
    //endregion

    /**
     * 获得平台授权码
     *
     * @return
     */
    public String getAccessToken() {
        if (!StringUtils.hasLength(this.PlatformCode)) return "";

        String AccessToken = DataService.getMainCache().getString("Libs:GenkigoSDK:AccessToken");
        if (StringUtils.hasLength(AccessToken)) return AccessToken;

        String url = String.format("%s/access_token?platformCode=%s", this.RestAPIUrl, this.PlatformCode);

        String responseText = Http2Util.get(url);
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.warn(this.getClass().getSimpleName(), "响应空白结果");
            return "";
        }

        /**
         * {
         *     "code": 0,
         *     "msg": "",
         *     "data": {
         *         "AccessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYmYiOjE3MDEwNzI1NjQsImlzcyI6IllrVWZQMzV5bk5KY0Z5NXp5WFl3aUNYdkRwTDVzRTZLIiwiZXhwIjoxNzAxMDc2NDYzLCJpYXQiOjE3MDEwNzI1NjMsImp0aSI6ImI0ZTFiM2E2LTY5NjYtNGE5Yy1iNzVlLTQ5NTNhMzc4MDUzZCJ9.v5-r8TsZZEk4HZa4GHxoRxfE--UjxUSXmfsXcroZnZE",
         *         "expired": 3600000
         *     },
         *     "create_time": 1701072563985
         * }
         */
        JSONObject json = JSONObject.parseObject(responseText);
        if (json == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "json格式错误：%s", responseText);
            return "";
        }
        if (json.getIntValue("code", -1) != 0) {
            LogsUtil.warn(this.getClass().getSimpleName(), "获取授权码错误：%s", responseText);
            return "";
        }

        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "获取授权码错误,无Data节点数据：%s", responseText);
            return "";
        }
        AccessToken = data.getString("AccessToken");
        long expired = data.getLong("expired");
        if (StringUtils.hasLength(AccessToken) && expired > 0) {
            DataService.getMainCache().set("Libs:GenkigoSDK:AccessToken", AccessToken, expired);
        }
        return AccessToken;
    }

    /**
     * 使用ResAPI：同步一个充电桩的信息，建议在竣工的时候操作同步
     *
     * @return
     */
    public SyncResult syncChargeStation(String CSId, String name, JSONObject params) {
        String AccessToken = getAccessToken();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("AccessToken", AccessToken);

        // 上层平台代码和其他相关信息
        params.put("platform_code", this.PlatformCode);
        params.put("CSId", CSId);
        params.put("name", name);

        String url = String.format("%s/Sync/ChargeStation", this.RestAPIUrl);
        String responseText = Http2Util.postContent(url, params.toJSONString(), header, "application/json");
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.warn(this.getClass().getSimpleName(), "响应空白结果");
            return new SyncResult(403, "请求成功，无响应结果");
        }

        JSONObject json = JSONObject.parseObject(responseText);
        if (json == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "json格式错误：%s", responseText);
            return new SyncResult(404, "请求成功，结果格式错误");
        }
        if (json.getIntValue("code", -1) != 0) {
            LogsUtil.warn(this.getClass().getSimpleName(), "- %s - 发生错误：%s", url, responseText);
            return new SyncResult(401, "请求成功，发生错误");
        }

        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "- %s - 无Data数据：%s", url, responseText);
            return new SyncResult(405, "请求成功，无相关数据处理");
        }

        Map<String, Object> cbdata = new LinkedHashMap<>(data);
        return new SyncResult(0, "", cbdata);
    }

    /**
     * 使用ResAPI：同步设备信息
     * {
     * "platform_code": "genkigo",
     * "projectId": "项目ID",
     * "platform_cs_id": "上层充电桩ID",
     * "meterNo": "电表编号",
     * "host": {
     * "deviceCode": "设备码",
     * "deviceName": "设备名称",
     * "deviceNumber": "设备物理ID",
     * "deviceUnitId": "关联的设备单元ID",
     * "simCode": "SIM卡号",
     * "isHost": 1,
     * "chargeStandardConfigId": 1,
     * "chargeTimeConfigId": 1,
     * "parkingConfigId": 0,
     * "safeCharge": 0,
     * "safeChargeFee": 0,
     * "display_status": 0
     * },
     * "device": {
     * "deviceCode": "设备码",
     * "deviceName": "设备名称",
     * "deviceNumber": "设备物理ID",
     * "deviceUnitId": "关联的设备单元ID",
     * "simCode": "SIM卡号",
     * "isHost": 0,
     * "chargeStandardConfigId": 1,
     * "chargeTimeConfigId": 1,
     * "parkingConfigId": 0,
     * "safeCharge": 0,
     * "safeChargeFee": 0,
     * "display_status": 0
     * }
     * }
     */
    public SyncResult syncDevice(String CSId, JSONObject params, JSONObject host, JSONArray devices) {
        String AccessToken = getAccessToken();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("AccessToken", AccessToken);

        // 上层平台代码和其他相关信息
        params.put("platform_code", this.PlatformCode);
        params.put("platform_cs_id", CSId);

        if (host != null) params.put("host", host);
        if (devices != null) params.put("devices", devices);

        String url = String.format("%s/Sync/Device", this.RestAPIUrl);
        String responseText = Http2Util.postContent(url, params.toJSONString(), header, "application/json");
        if (!StringUtils.hasLength(responseText)) {
            LogsUtil.warn(this.getClass().getSimpleName(), "响应空白结果");
            return new SyncResult(403, "请求成功，无响应结果");
        }

        JSONObject json = JSONObject.parseObject(responseText);
        if (json == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "json格式错误：%s", responseText);
            return new SyncResult(404, "请求成功，结果格式错误");
        }
        if (json.getIntValue("code", -1) != 0) {
            LogsUtil.warn(this.getClass().getSimpleName(), "- %s - 发生错误：%s", url, responseText);
            return new SyncResult(401, "请求成功，发生错误");
        }
        return new SyncResult(0, "");
    }
}
