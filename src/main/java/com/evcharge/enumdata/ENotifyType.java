package com.evcharge.enumdata;

import lombok.Getter;

/**
 * 通知等级
 */
@Getter
public enum ENotifyType {
    // 定义不同的通知等级，每个等级有优先级和描述
    NONE(0, "无"),
    WECHATCORPBOT(1, "企业微信机器人通知类型"),
    SMS(2, "短信通知类型"),
    VOICE(3, "语音通知类型");

    /**
     * 索引
     */
    private final int index;
    /**
     * 描述
     */
    private final String description;

    // 构造函数
    ENotifyType(int index, String description) {
        this.index = index;
        this.description = description;
    }

    // 根据优先级获取对应的通知等级
    public static ENotifyType fromIndex(int index) {
        for (ENotifyType level : ENotifyType.values()) {
            if (level.getIndex() == index) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的优先级: " + index);
    }
}
