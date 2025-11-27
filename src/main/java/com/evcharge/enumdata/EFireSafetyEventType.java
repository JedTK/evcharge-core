package com.evcharge.enumdata;

import lombok.Getter;

/**
 * 事件等级
 */
@Getter
public enum EFireSafetyEventType {
    // 定义不同的通知等级，每个等级有优先级和描述
    NONE(0, "不确定类型"),
    FIRE_SAFETY_EVENT(1001, "消防应急事件"),
    DEVICE_EVENT(1002, "设备消息事件"),
    NORMAL(1003, "普通事件"),
    INFO(1004, "信息事件");


    // 获取优先级
    // 定义优先级和描述字段
    private final int index;
    // 获取描述
    private final String description;

    // 构造函数
    EFireSafetyEventType(int index, String description) {
        this.index = index;
        this.description = description;
    }

    // 根据优先级获取对应的通知等级
    public static EFireSafetyEventType fromIndex(int index) {
        for (EFireSafetyEventType level : EFireSafetyEventType.values()) {
            if (level.getIndex() == index) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的优先级: " + index);
    }
}
