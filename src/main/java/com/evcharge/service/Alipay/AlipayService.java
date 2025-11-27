package com.evcharge.service.Alipay;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.aliyun.AliPaySDK;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayService {
    private final AliPaySDK aliPaySDK = new AliPaySDK();


    /**
     * 使用授权码从支付宝获取用户访问令牌和 openId。
     *
     * @param authCode 支付宝的授权码。
     * @return 包含 'access_token' 和 'open_id' 的 Map。
     */
    public Map<String, Object> getAuthInfo(String authCode) {
        if (!StringUtils.hasLength(authCode)) {
            throw new IllegalArgumentException("授权码不能为空");
        }

        SyncResult r = aliPaySDK.getAccessToken(authCode);
        if (r.code != 0) {
            throw new RuntimeException("获取支付宝授权失败: " + r.msg);
        }

        return (Map<String, Object>) r.data;
    }

    /**
     * 从支付宝获取用户的手机号。
     *
     * @param response 支付宝返回的加密响应字符串。
     * @return 用户的手机号码。
     */
    public String getMobileNumber(String response) {
        SyncResult result = aliPaySDK.getPhoneInfo(response);
        System.out.println("getMobileNumber=" + result);
        if (result.code != 0) {
            throw new RuntimeException("获取手机号失败: " + result.msg);
        }
        JSONObject phoneInfo = JSONObject.parseObject((String) result.data);
        return phoneInfo.getString("mobile");
    }


    /**
     * 根据支付宝授权信息在系统中创建新用户。
     *
     * @param openId 用户的支付宝 openId。
     * @param accessToken 用户的支付宝访问令牌。
     * @param deviceCode 设备信息。
     * @param csId 充电站ID。
     * @return 新创建的用户ID。
     */
    public long createAlipayUser(String openId, String accessToken, String deviceCode, String devicePort, String deviceIndex, long csId) {
        SyncResult result = aliPaySDK.createUser(openId, accessToken, deviceCode, devicePort, deviceIndex, csId);
        if (result.code != 0) {
            throw new RuntimeException("用户创建失败: " + result.msg);
        }
        return MapUtil.getLong((Map<String, Object>) result.data, "uid");
    }




}
