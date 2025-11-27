package com.evcharge.libsdk.wechat;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.client.RestTemplate;

public class WechatSDK {
    private final HttpClientForWechat httpClientForWechat;
    public static final String appId = ConfigManager.getString("wechat.appId");
    public static final String appSecret = ConfigManager.getString("wechat.appSecret");


    public WechatSDK() {
//        System.out.println("初始化构造函数");
        this.httpClientForWechat = new HttpClientForWechat(new RestTemplate());
        this.initAccessToken();
//        Security.addProvider(new BouncyCastleProvider());
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 更新session_key 通过jscode获取session_key 和open_id 并写入缓存
     *
     * @param code
     * @return
     */
    public SyncResult updateSessionKey(String code) {
        String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", appId, appSecret, code);
        try {
            String text = HttpUtil.sendGet(url);

            if ("".equals(text)) {
                return new SyncResult(1, "code不能为空");
            }
            JSONObject json = (JSONObject) JSONObject.parse(text);

            if (json.containsKey("errcode")) {
                return new SyncResult(2, "解密失败");
            }
            DataService.getMainCache().set(String.format("User:session_key:%s", json.getString("openid")), json.getString("session_key"));

            return new SyncResult(0, "success", json);
        } catch (Exception e) {
            LogsUtil.error(e, "", "【调试信息】%s.%s 通过API读取用户登录信息失败,code=%s", WechatSDK.class.getPackageName(), WechatSDK.class.getName(), code);
            return new SyncResult(3, e.getMessage());
        }
    }

    /**
     * 解密微信数据
     *
     * @param encryptedData
     * @param sessionKey
     * @param iv
     * @return
     * @throws Exception
     */
    public SyncResult decryptData(String encryptedData, String sessionKey, String iv) throws Exception {

        if (sessionKey.length() < 24) {
            return new SyncResult(1, "sessionKey非法");
        }

        if (iv.length() < 24) {
            return new SyncResult(1, "iv非法");
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] _aesIV = decoder.decode(iv);
        byte[] _sessionKey = decoder.decode(sessionKey);
        byte[] _encryptedData = decoder.decode(encryptedData);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKey spec = new SecretKeySpec(_sessionKey, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            parameters.init(new IvParameterSpec(_aesIV));
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);
            byte[] resultByte = cipher.doFinal(_encryptedData);
            if (null != resultByte && resultByte.length > 0) {
                String result = new String(WechatPKCS7Encoder.decode(resultByte));
                JSONObject json = JSONObject.parseObject(result);
                return new SyncResult(0, "success", json);
            } else {
                return new SyncResult(2, "小程序解析失败");
            }
        } catch (NoSuchAlgorithmException | IllegalBlockSizeException | InvalidAlgorithmParameterException |
                 InvalidParameterSpecException | InvalidKeyException | BadPaddingException | NoSuchPaddingException e) {
            e.printStackTrace();
            LogsUtil.trace("小程序解析出错1{}", e.getMessage());
            return new SyncResult(2, e.getMessage());
        }
    }

    /**
     * 获取手机号码
     *
     * @param code
     * @return
     */
    public SyncResult getPhoneInfo(String code) {

        String accessToken = DataService.getMainCache().getString("Wechat:AccessToken");

        String url = String.format("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s", accessToken);
        try {
            JSONObject param = new JSONObject();
            param.put("code", code);
//            param.put("access_token", access_token);
            String httpParam = param.toJSONString();
            String contentType = "application/json";
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("Accept", "application/json");
            String text = HttpUtil.sendPost(url, httpParam, header, contentType);
//            String text = HttpUtil.sendPost(url, http_param);
            if ("".equals(text)) {
                return new SyncResult(1, "code不能为空");
            }
            JSONObject json = (JSONObject) JSONObject.parse(text);
            System.out.println(json);
            if (json.containsKey("errcode") && !json.get("errcode").equals(0)) {
                DataService.getMainCache().del("Wechat:AccessToken");
                return new SyncResult(2, "解密失败");
            }
            return new SyncResult(0, "success", json.get("phone_info"));
        } catch (Exception e) {
            LogsUtil.error(e, "", "【调试信息】%s.%s 通过API读取用户登录信息失败,code=%s", WechatSDK.class.getPackageName(), WechatSDK.class.getName(), code);
            return new SyncResult(3, e.getMessage());
        }

    }

    /**
     * 发送小程序消息 //弃用，转移到WechatSubscribeTmplSDK发送消息
     *
     * @param openId     用户openid
     * @param jumpUrl    页面跳转地址
     * @param templateId 模版id
     * @param msgObj     消息主体
     * @return
     */
    @Deprecated
    public SyncResult sendSubscribeMessage(String openId, String jumpUrl, String templateId, Map<String, Object> msgObj) {

        String accessToken = DataService.getMainCache().getString("Wechat:AccessToken");
        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s", accessToken);

        try {
            TempMessageEntity tempMessage = new TempMessageEntity();
            tempMessage.setTouser(openId);
            tempMessage.setLang("zh_CN");
            tempMessage.setPage(jumpUrl);
            tempMessage.setTemplate_id(templateId);
            tempMessage.setMiniprogram_state("formal"); //跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版

            JSONObject messageBodyObj = new JSONObject();

            Iterator it = msgObj.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                Object v = msgObj.get(key);

                JSONObject valuejson = new JSONObject();
                valuejson.put("value", v);
                messageBodyObj.put(key, valuejson);
            }

            tempMessage.setData(messageBodyObj);

//            String httpParam = JsonUtil.toJsonString(tempMessage);
            String httpParam = JSONObject.toJSONString(tempMessage, JSONWriter.Feature.IgnoreNonFieldGetter
                    , JSONWriter.Feature.IgnoreErrorGetter
                    , JSONWriter.Feature.IgnoreNoneSerializable);
            httpParam = httpParam.replace("\\\\", "\\");
            Map<String, Object> header = new LinkedHashMap<>();

            String text = HttpUtil.sendPost(url, httpParam, header, "application/json");

            JSONObject json = (JSONObject) JSONObject.parse(text);

            if (json.containsKey("errcode") && !json.get("errcode").equals(0)) {
                return new SyncResult(2, "发送失败 " + text);
            }
            return new SyncResult(0, "success");
        } catch (Exception e) {
            return new SyncResult(3, e.getMessage());
        }
    }

    /**
     * 初始化access_token
     */
    public void initAccessToken() {
        String accessToken = DataService.getMainCache().getString("Wechat:AccessToken");
        if (!("".equals(accessToken))) {
            return;
        }
        String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appId, appSecret);
        try {
            String text = httpClientForWechat.sendGet(url);
            if ("".equals(text)) {
                return;
            }
            JSONObject json = (JSONObject) JSONObject.parse(text);
            if (json.containsKey("errcode")) {
                return;
            }
            String token = json.getString("access_token");
            Long expiresIn = json.getLong("expires_in");
            expiresIn = (expiresIn - 3600) * 1000;
            DataService.getMainCache().set("Wechat:AccessToken", token, expiresIn);
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
        String ticket = DataService.getMainCache().getString("WxJsApiTicket");

        if ("".equals(ticket)) {
            String accessToken = DataService.getMainCache().getString("Wechat:AccessToken");
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
                DataService.getMainCache().set("WxJsApiTicket", ticket, (json.getLong("expires_in") - 100) * 1000);
                return ticket;
            } catch (Exception e) {
                LogsUtil.error(e, "", "[获取微信JsApiTicket],获取失败，失败原因:%s", e.getMessage());
            }
        }
        return ticket;
    }

    /**
     * 获取jsapi签名
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    public JSONObject createSignPackage() throws NoSuchAlgorithmException {

        String jsApiTicket = this.getJsApiTicket();
        String nonceStr = this.createNonceStr();
        String url = HttpRequestUtil.getUrl();

        long timestamp = TimeUtil.getTimestamp() / 1000;


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
