package com.evcharge.service.Active;

import com.evcharge.service.Active.base.IACTStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动策略工厂
 */
public class ACTStrategyFactory {
    /**
     * 策略保存，快速查找
     */
    private static final Map<String, IACTStrategy> MAP = new ConcurrentHashMap<>();

    /**
     * 注册活动策略
     *
     * @param strategy 活动策略
     */
    public static void register(IACTStrategy strategy) {
        if (strategy == null || strategy.code() == null) return;
        MAP.put(strategy.code(), strategy);
    }

    /**
     * 获取活动策略
     *
     * @param activity_code 活动编码
     * @return 活动策略
     */
    public static IACTStrategy get(String activity_code) {
        return MAP.get(activity_code);
    }
}
