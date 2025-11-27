package com.evcharge.enumdata;

import lombok.Getter;

/**
 * 订单状态
 */
@Getter
public enum EConsumeOrderStatus {
    /**
     * 待支付
     */
    PENDING(1, "待支付"),

    /**
     * 已支付
     */
    PAID(2, "已支付"),

    /**
     * 已取消
     */
    CANCELED(-1, "已取消"),

    /**
     * 全额退款
     */
    FULL_REFUND(3, "全额退款"),

    /**
     * 部分退款
     */
    PART_REFUND(4, "部分退款");

    // 状态码
    private final int code;

    // 状态描述
    private final String description;

    // 构造方法
    EConsumeOrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码获取枚举
     */
    public static EConsumeOrderStatus fromCode(int code) {
        for (EConsumeOrderStatus status : EConsumeOrderStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态码: " + code);
    }
}
