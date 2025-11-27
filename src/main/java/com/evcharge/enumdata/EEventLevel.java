package com.evcharge.enumdata;

import lombok.Getter;

/**
 * 事件等级
 */
@Getter
public enum EEventLevel {
    // 定义不同的通知等级，每个等级有优先级和描述
    NONE(0, "无等级，不需要理会"),
    EMERGENCY(1, "紧急事件，需要立即处理"),
    IMPORTANT(2, "重要事件，及时处理"),
    NORMAL(3, "普通事件，关注即可"),
    INFO(4, "信息事件，无需立即响应");


    // 获取优先级
    // 定义优先级和描述字段
    private final int index;
    // 获取描述
    private final String description;

    // 构造函数
    EEventLevel(int index, String description) {
        this.index = index;
        this.description = description;
    }

    // 根据优先级获取对应的通知等级
    public static EEventLevel fromIndex(int index) {
        for (EEventLevel level : EEventLevel.values()) {
            if (level.getIndex() == index) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的优先级: " + index);
    }
}
