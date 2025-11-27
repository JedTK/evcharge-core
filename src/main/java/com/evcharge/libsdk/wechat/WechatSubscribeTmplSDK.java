package com.evcharge.libsdk.wechat;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

public class WechatSubscribeTmplSDK {
    private final HttpClientForWechat httpClientForWechat;
    public static final String appId = ConfigManager.getString("wechat.appId");
    public static final String appSecret = ConfigManager.getString("wechat.appSecret");


    public WechatSubscribeTmplSDK() {
        this.httpClientForWechat = new HttpClientForWechat(new RestTemplate());
        this.initAccessToken();

    }

    /**
     * 初始化access_token
     */
    public String initAccessToken() {
        String accessToken = DataService.getMainCache().getString("Wechat:StableAccessToken");
        if (!("".equals(accessToken))) {
            return accessToken;
        }
        String url = "https://api.weixin.qq.com/cgi-bin/stable_token";
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", appId);
            params.put("secret", appSecret);
            params.put("force_refresh", true);
            String text = httpClientForWechat.sendPost(url, params, MediaType.APPLICATION_JSON);
            System.out.println("【调试信息】%s.%s 通过API读取用户登录信息:%s"+text);
            if ("".equals(text)) {
                return null;
            }
            JSONObject json = (JSONObject) JSONObject.parse(text);
            if (json.containsKey("errcode")) {
                return null;
            }
            System.out.println(json);
            String token = json.getString("access_token");
            Long expiresIn = json.getLong("expires_in");
            expiresIn = (expiresIn - 3600) * 1000;
            DataService.getMainCache().set("Wechat:StableAccessToken", token, expiresIn);
            return token;
        } catch (Exception e) {
            LogsUtil.error(e, "", "【调试信息】%s.%s 通过API读取用户登录信息失败,code=%s", WechatSDK.class.getPackageName(), WechatSDK.class.getName());
            return null;
        }
    }

    /**
     * 发送小程序消息
     *
     * @param openId     用户openid
     * @param jumpUrl    页面跳转地址
     * @param templateId 模版id
     * @param msgObj     消息主体
     * @return
     */
    public SyncResult sendSubscribeMessage(String openId, String jumpUrl, String templateId, Map<String, Object> msgObj) {

        String accessToken = DataService.getMainCache().getString("Wechat:StableAccessToken");
        if (!StringUtils.hasLength(accessToken)) {
            accessToken = initAccessToken();
        }
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s", accessToken);

        try {
            JSONObject tempMessage = new JSONObject();
//            Map<String,Object> tempMessage = new LinkedHashMap<>();

            tempMessage.put("touser", openId);
            tempMessage.put("template_id", templateId);
            tempMessage.put("page", jumpUrl);
            tempMessage.put("miniprogram_state", "formal");
            tempMessage.put("lang", "zh_CN");

            JSONObject messageBodyObj = new JSONObject();

            for (String key : msgObj.keySet()) {
                Object v = msgObj.get(key);

                JSONObject valueJson = new JSONObject();
                valueJson.put("value", v);
                messageBodyObj.put(key, valueJson);
            }
            tempMessage.put("data", messageBodyObj);
            String text = httpClientForWechat.sendPost(url, tempMessage,MediaType.APPLICATION_JSON);
            JSONObject json = JSONObject.parse(text);

            if (json.containsKey("errcode") && !json.get("errcode").equals(0)) {
                return new SyncResult(2, "发送失败：" + text);
            }
            return new SyncResult(0, "success");
        } catch (Exception e) {
            return new SyncResult(3, e.getMessage());
        }
    }

    /**
     * @param templateStr //short_thing1={msgTitle}&time2={checkinTime}&thing3={tips}&thing4={ChargeTime}&thing9={integralText}
     * @param sourceData
     * @return
     */
    public Map<String, Object> createTemplateData(String templateStr, Map<String, Object> sourceData) {
        try {
            Map<String, Object> msgData = new LinkedHashMap<>();
            for (String key : sourceData.keySet()) {
                String value = MapUtil.getString(sourceData, key);
                templateStr = templateStr.replace(String.format("{%s}", key), value);
            }
            String[] temp = templateStr.split("&");
            for (String str : temp) {
                String[] s = str.split("=");
                String key = s[0];
                Object value = s[1];
                msgData.put(key, value);
            }
            return msgData;
        } catch (Exception e) {
            return null;
        }

    }

}
