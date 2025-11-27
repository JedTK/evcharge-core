package com.evcharge.libsdk.wechat.office;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.HttpClientForWechat;
import com.evcharge.libsdk.wechat.WechatSDK;
import com.xyzs.entity.DataService;
import com.xyzs.utils.*;
import org.springframework.web.client.RestTemplate;

/**
 * 公众号SDK
 */
public class OfficeAccountSDK {
    private final HttpClientForWechat httpClientForWechat;
    public static final String appId = ConfigManager.getString("wechat.officeAccountAppId");
    public static final String appSecret = ConfigManager.getString("wechat.officeAccountAppSecret");

    public OfficeAccountSDK() {
//        System.out.println("初始化构造函数");
        System.out.println(appId);
        System.out.println(appSecret);
        this.httpClientForWechat = new HttpClientForWechat(new RestTemplate());
        this.initAccessToken();
//        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * 初始化access_token
     */
    public void initAccessToken() {
        String access_token = DataService.getMainCache().getString("Wechat:WxOfficeAccessToken");
//        System.out.println("WxOfficeAccessToken=" + access_token);
        if (!("".equals(access_token))) {
            return;
        }
        String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appId, appSecret);
        try {
//            String text = Http2Util.get(url);
//            String text = httpClientForWechat.sendGet(url);
            String text =httpClientForWechat.sendGet(url);
//            System.out.println("httpClientForWechat="+text);
//            String text=HttpUtil.sendGet(url);
            if ("".equals(text)) {
                return;
            }
            JSONObject json = (JSONObject) JSONObject.parse(text);
            if (json.containsKey("errcode")) {
                return;
            }
            String token = json.getString("access_token");
            long expires_in = json.getLong("expires_in");
//            System.out.println(json.toJSONString());
            expires_in = (expires_in - 3600) * 1000;
//            int expires_in = 3600 * 1000;//2023-12-25 修改access_token为一小时

            DataService.getMainCache().set("Wechat:WxOfficeAccessToken", token, expires_in);
            return;
        } catch (Exception e) {
            LogsUtil.error(e, "", "【调试信息】%s.%s 通过API读取用户登录信息失败,code=%s", WechatSDK.class.getPackageName(), WechatSDK.class.getName());
            return;
        }
    }

    /**
     * 获取 jsapi_ticket：(请求次数有限制，建议全局存储与更新)
     *
     * @return
     */
    public String getJsApiTicket() {
        String ticket = DataService.getMainCache().getString("WxOfficeJsApiTicket");

        if ("".equals(ticket)) {
            String accessToken = DataService.getMainCache().getString("Wechat:WxOfficeAccessToken");
            String url = String.format("https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi&access_token=%s", accessToken);

            try {
                String text = HttpUtil.sendGet(url);
                if ("".equals(text)) {
                    return "";
                }
                JSONObject json = (JSONObject) JSONObject.parse(text);

                if (json.getInteger("errcode") != 0) {
                    return "";
                }

                ticket = json.getString("ticket");
                DataService.getMainCache().set("WxOfficeJsApiTicket", ticket, (json.getLong("expires_in") - 100) * 1000);
                return ticket;
            } catch (Exception e) {
                LogsUtil.error(e, "", String.format("[获取微信JsApiTicket],获取失败，失败原因:%s", e.getMessage()));
            }
        }
        return ticket;
    }

    /**
     * 获取jsapi签名
     *
     * @return
     */
    public JSONObject createSignPackage() {
        String jsApiTicket = this.getJsApiTicket();
        String nonceStr = this.createNonceStr();
        String url = HttpRequestUtil.getUrl();

        long timestamp = TimeUtil.getTimestamp() / 1000;

        try {

            String rawString = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s", jsApiTicket, nonceStr, timestamp, url);
            JSONObject signPackage = new JSONObject();
            String signature = SHAUtils.sha1(rawString);
            signPackage.put("jsapiTicket", jsApiTicket);
            signPackage.put("appId", appId);
            signPackage.put("nonceStr", nonceStr);
            signPackage.put("timestamp", timestamp);
            signPackage.put("url", url);
            signPackage.put("signature", signature);
            signPackage.put("rawString", rawString);
            return signPackage;

        } catch (Exception e) {
            LogsUtil.error(e, "", "签名失败");
            return new JSONObject();
        }

    }

    public JSONObject createSignPackage(String url) {
        String jsApiTicket = this.getJsApiTicket();
        String nonceStr = this.createNonceStr();

        long timestamp = TimeUtil.getTimestamp() / 1000;

        try {

            String rawString = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s", jsApiTicket, nonceStr, timestamp, url);
            JSONObject signPackage = new JSONObject();
            String signature = SHAUtils.sha1(rawString);
            signPackage.put("jsapiTicket", jsApiTicket);
            signPackage.put("appId", appId);
            signPackage.put("nonceStr", nonceStr);
            signPackage.put("timestamp", timestamp);
            signPackage.put("url", url);
            signPackage.put("signature", signature);
            signPackage.put("rawString", rawString);
            return signPackage;

        } catch (Exception e) {
            LogsUtil.error(e, "", "签名失败");
            return new JSONObject();
        }

    }


    /**
     * 获取随机数
     *
     * @return
     */
    private String createNonceStr() {
        int length = 10;
        String charts = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String str = "";
        for (int i = 0; i < length; i++) {
//            int len = charts.length();
            int rand = common.randomInt(0, charts.length() - 1);
//            String _s =charts.substring(rand-1, rand);
            str = str.concat(charts.substring(rand, rand + 1));
        }
        return str;
    }


}
