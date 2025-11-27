package com.evcharge.libsdk.dahua;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;

import java.util.*;

public class DaHuaAuthSDK {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String AccessTokenCache = "DaHua:AccessToken";

    /**
     * 创建请求头
     *
     * @return Map<String, Object>
     */
    public Map<String, Object> createHeader(Map<String, Object> requestBody) {
        try {
            String requestBodyStr = objectMapper.writeValueAsString(requestBody);
            String nonce = common.randomStr(32);
            String traceIdHeader = common.randomStr(32);
            String timestamp = String.valueOf(TimeUtil.getTimestamp());
            String appAccessToken = this.getAppAccessToken();

            // 公共 header
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("Accept-Language", "zh-CN");
            header.put("Version", "v1");
            header.put("AccessKey", DaHuaConfig.accessKey);
            header.put("Timestamp", timestamp);
            header.put("Nonce", nonce);
            header.put("AppAccessToken", appAccessToken);
            header.put("X-TraceId-Header", traceIdHeader);
            header.put("ProductId", DaHuaConfig.productId);

            // 计算签名（使用 header 中已有的字段）
            String sign = SignatureUtil.openSign(
                    DaHuaConfig.accessKey,
                    appAccessToken,
                    timestamp,
                    nonce,
                    "POST",
                    castToStringMap(header), // 这里需要 Map<String, String>
                    requestBodyStr,
                    DaHuaConfig.secretAccessKey
            );
            header.put("Sign", sign);

            return header;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 辅助方法：将 Map<String, Object> 转成 Map<String, String>
     */
    private Map<String, String> castToStringMap(Map<String, Object> source) {
        Map<String, String> target = new HashMap<>();
        source.forEach((k, v) -> target.put(k, String.valueOf(v)));
        return target;
    }


    public String getAppAccessToken() {

        String appAccessToken = DataService.getMainCache().getString(AccessTokenCache);

        if (StringUtils.hasLength(appAccessToken)) {
            return appAccessToken;
        }

        SyncResult r = initAppAccessToken();
        if (r.code != 0) {
            return null;
        }

        return (String) r.data;
    }


    /**
     * 获取AccessToken
     */
    private SyncResult initAppAccessToken() {

        String url = DaHuaConfig.authUrl;

        String nonce = common.randomStr(32);
        String traceIdHeader = common.randomStr(32);
        long timestamp = TimeUtil.getTimestamp();
        String signature = SignatureUtil.generateSign(
                DaHuaConfig.accessKey
                , DaHuaConfig.secretAccessKey
                , String.format("%s", timestamp)
                , nonce
                , "POST"
        );

        Map<String, Object> param = new LinkedHashMap<>();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("Version", "v1");
        header.put("AccessKey", DaHuaConfig.accessKey);
        header.put("Timestamp", timestamp);
        header.put("Nonce", nonce);
        header.put("Sign", signature);
        header.put("ProductId", DaHuaConfig.productId); //需要加上配置
        header.put("X-TraceId-Header", traceIdHeader);

        String text = Http2Util.post(url, param, header, "application/json");

        if (!StringUtils.hasLength(text)) {
            return new SyncResult(1, "获取token失败，text为空");
        }
        JSONObject jsonObject = JSONObject.parseObject(text);

        if (jsonObject.getIntValue("code") != 200) {
            LogsUtil.info(this.getClass().getName(), "【大华云联】获取AccessToken失败，失败原因：" + jsonObject.getString("msg"));
            return new SyncResult(1, "【大华云联】获取AccessToken失败，失败原因：" + jsonObject.getString("msg"));
        }
        JSONObject data = jsonObject.getJSONObject("data");

        String accessToken = data.getString("appAccessToken");

        long validitySeconds = data.getLongValue("validitySeconds");

        DataService.getMainCache().set(AccessTokenCache, accessToken, TimeUtil.getTimestamp() + validitySeconds - 600);

        return new SyncResult(0, "success", accessToken);
    }


}
