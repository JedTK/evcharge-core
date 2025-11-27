package com.evcharge.libsdk.abcpay;


import com.alibaba.fastjson2.JSONObject;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 农行支付sdk
 */
public class ABCPaySDK {

    public static final String openBankKey = "bankabc1";
    public static final String openBankPadding = "abchina1";
    public static final String openBankUrl = "https://wx.abchina.com/webank/main-view/openTagForGD?id=%s&dynamicData=%s";




    /**
     * 获取tokenid
     *
     * @param content 报文内容
     * @return String
     */
    public static String getTokenID(JSONObject content) {
        if (content == null) return null;
        String tokenId = "";
        try {
            JSONObject message = content.getJSONObject("MSG").getJSONObject("Message");
            String paymentURL = message.getString("PaymentURL");
            tokenId = extractToken(paymentURL);

            return tokenId;
        } catch (Exception e) {
            LogsUtil.error("ABCPaySDK", "获取tokenId失败：" + e.getMessage());
            return null;
        }
    }


    /**
     * 创建跳转链接
     *
     * @return string
     */
    public static String createJumpUrl(String tokenId) {
        try {
            String encryptStr = new String(ABCPayUtilsSDK.encryptCBCMode("tokenID="+tokenId, "bankabc1", "abchina1"));
            String resultEnc = URLEncoder.encode(encryptStr, StandardCharsets.UTF_8);
            return "https://wx.abchina.com/webank/main-view/openTagForGD?id=%2BdnYI3df4A0%3D&dynamicData=" + resultEnc;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }


    /**
     * Extracts the TOKEN parameter value from a URL
     *
     * @param url The URL containing the TOKEN parameter
     * @return The value of the TOKEN parameter, or null if not found
     */
    public static String extractToken(String url) {
        try {
            // Create URI object from the URL string
            URI uri = new URI(url);
            // Get the query part of the URL
            String query = uri.getQuery();
            if (query != null) {
                // Split the query into parameter pairs
                String[] parameters = query.split("&");
                // Search for the TOKEN parameter
                for (String parameter : parameters) {
                    String[] keyValue = parameter.split("=");
                    if (keyValue.length == 2 && "TOKEN".equals(keyValue[0])) {
                        return keyValue[1];
                    }
                }
            }
            // TOKEN parameter not found
            return null;
        } catch (URISyntaxException e) {
            // Handle invalid URL
            System.err.println("Invalid URL: " + e.getMessage());
            return null;
        }
    }






}
