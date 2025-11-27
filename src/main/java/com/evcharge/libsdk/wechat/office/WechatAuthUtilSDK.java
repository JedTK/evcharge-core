package com.evcharge.libsdk.wechat.office;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.utils.LogsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


public class WechatAuthUtilSDK {

    private static final Logger logger = LoggerFactory.getLogger(WechatAuthUtilSDK.class);

    // 微信公众号配置信息
    private String appId;          // 公众号appId
    private String appSecret;      // 公众号appSecret
    private String redirectUri;    // 回调地址

    // 微信接口URL
    private static final String AUTH_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String REFRESH_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/refresh_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    /**
     * 构造函数
     * @param appId 公众号appId
     * @param appSecret 公众号appSecret
     */
    public WechatAuthUtilSDK(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    /**
     * 获取微信授权URL
     * 重定向用户到这个URL以获取授权
     *
     * @param state 状态参数，可用于防止CSRF攻击
     * @return 完整的授权URL
     */
    public String getAuthorizationUrl(String redirectUri,String state) {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return AUTH_URL +
                "?appid=" + appId +
                "&redirect_uri=" + encodedRedirectUri +
                "&response_type=code" +
                "&scope=snsapi_userinfo" + // 使用snsapi_userinfo作用域以获取用户信息
                "&state=" + state +
                "#wechat_redirect";
    }

    /**
     * 通过code获取access_token和openid
     *
     * @param code 授权回调获取的临时票据
     * @return 包含access_token, openid等信息的JSONObject
     */
    public JSONObject getAccessToken(String code) {
        String urlBuilder = ACCESS_TOKEN_URL +
                "?appid=" + appId +
                "&secret=" + appSecret +
                "&code=" + code +
                "&grant_type=authorization_code";

        String result = httpGet(urlBuilder);
        if (result != null) {
            return JSONObject.parse(result);
        }
        return null;
    }

    /**
     * 刷新access_token
     *
     * @param refreshToken 用户刷新access_token的refresh_token
     * @return 包含新access_token的JSONObject
     */
    public JSONObject refreshAccessToken(String refreshToken) {
        String urlBuilder = REFRESH_TOKEN_URL +
                "?appid=" + appId +
                "&grant_type=refresh_token" +
                "&refresh_token=" + refreshToken;

        String result = httpGet(urlBuilder);
        if (result != null) {
            return JSONObject.parse(result);
        }
        return null;
    }

    /**
     * 获取用户信息，包括unionid (如果用户已关注公众号或开发者已获取授权)
     *
     * @param accessToken 访问令牌
     * @param openId 用户的OpenID
     * @return 包含用户信息的JSONObject，包括unionid(如果可获取)
     */
    public JSONObject getUserInfo(String accessToken, String openId) {
        String urlBuilder = USER_INFO_URL +
                "?access_token=" + accessToken +
                "&openid=" + openId +
                "&lang=zh_CN";

        String result = httpGet(urlBuilder);
        if (result != null) {
            return JSONObject.parse(result);
        }
        return null;
    }

    /**
     * 执行完整的授权流程并获取用户信息
     *
     * @param code 授权回调获取的临时票据
     * @return 用户完整信息，包括unionid(如果可获取)
     */
    public JSONObject getUserInfoByCode(String code) {
        // 1. 获取access_token和openid
        JSONObject tokenInfo = getAccessToken(code);
        if (tokenInfo == null ) {
            LogsUtil.error(this.getClass().getName(),"获取access_token失败: {tokenInfo 为null}");
            return null;
        }

        String accessToken = tokenInfo.getString("access_token");
        String openId = tokenInfo.getString("openid");

        // 2. 使用access_token和openid获取用户信息
        return getUserInfo(accessToken, openId);
    }

    /**
     * 执行HTTP GET请求
     *
     * @param urlStr 请求URL
     * @return 响应内容
     */
    private String httpGet(String urlStr) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // 创建连接
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                logger.error("HTTP GET请求失败，响应码: {}", responseCode);
            }
        } catch (Exception e) {
            logger.error("执行HTTP GET请求时发生异常", e);
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("关闭reader失败", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    /**
     * 示例用法
     */
    public static void main(String[] args) {
        // 初始化工具类
        WechatAuthUtilSDK util = new WechatAuthUtilSDK(
                "YOUR_APP_ID",
                "YOUR_APP_SECRET"
        );

        // 1. 前端使用的授权URL
        String authUrl = util.getAuthorizationUrl( "YOUR_REDIRECT_URI","STATE");
        System.out.println("授权URL: " + authUrl);

        // 2. 回调后获取用户信息
        // 假设已从回调请求中获取到code
        String code = "CODE_FROM_CALLBACK";
        JSONObject userInfo = util.getUserInfoByCode(code);

        if (userInfo != null) {
            System.out.println("用户信息:");
            System.out.println("openid: " + userInfo.getString("openid"));
            System.out.println("nickname: " + userInfo.getString("nickname"));

            // 获取unionid (如果存在)
//            if (userInfo.has("unionid")) {
//                System.out.println("unionid: " + userInfo.getString("unionid"));
//            } else {
//                System.out.println("未获取到unionid, 可能需要用户关注公众号或在微信开放平台绑定公众号");
//            }
        }
    }

}
