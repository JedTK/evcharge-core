package com.evcharge.strategy.ConsumeCenter.Payment.config;

import java.util.HashMap;
import java.util.Map;

public class PaymentTypeMapper {

    private static final Map<String, String> SERVICE_TO_DB_MAP = new HashMap<>();

    static {
        SERVICE_TO_DB_MAP.put("WechatPay_MP", "wechat_mp");
        SERVICE_TO_DB_MAP.put("ALIPAY_MP", "alipay_mp");
        SERVICE_TO_DB_MAP.put("HmPay_MP", "hmpay_mp");
    }

    public static String toDbValue(String serviceKey) {
        return SERVICE_TO_DB_MAP.get(serviceKey);
    }
}