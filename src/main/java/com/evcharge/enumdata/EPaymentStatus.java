package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 支付和订单完成状态（包含一些支付常用的枚举）
 */
public enum EPaymentStatus {
    /**
     * 无状态
     */
    None(-1),
    /**
     * 待支付
     */
    PendingPayment(0),
    /**
     * 支付失败
     */
    PaymentFailed(1),
    /**
     * 支付完成
     */
    PaymentCompleted(2),
    /**
     * 全额退款
     */
    FullRefund(3),
    /**
     * 部分退款
     */
    PartialRefund(4),
    /**
     * 已取消
     */
    Cancelled(5),
    /**
     * 已过期
     */
    Expired(6),
    /**
     * 异常订单
     */
    ExceptionOrder(7),
    /**
     * 待审核
     */
    AwaitingApproval(8),
    /**
     * 审核失败
     */
    ApprovalFailed(9);

    public final int index;

    private static final Map<Integer, EPaymentStatus> map = Stream.of(EPaymentStatus.values())
            .collect(Collectors.toMap(e -> e.index, e -> e));

    EPaymentStatus(int index) {
        this.index = index;
    }

    public static EPaymentStatus valueOf(int index) {
        return map.getOrDefault(index, null);
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
