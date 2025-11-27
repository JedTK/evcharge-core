package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 退款状态枚举
 */
public enum ERefundStatus {
    /**
     * 无状态
     */
    None(-1),
    /**
     * 待处理
     */
    Pending(0),
    /**
     * 处理中
     */
    InProgress(1),
    /**
     * 已完成
     */
    Completed(2),
    /**
     * 已取消
     */
    Cancelled(3),
    /**
     * 失败
     */
    Failed(4),
    /**
     * 部分退款
     */
    PartialRefund(5),
    /**
     * 全额退款
     */
    FullRefund(6),
    /**
     * 审核中
     */
    UnderReview(7),
    /**
     * 审核失败
     */
    ReviewFailed(8);

    public final int index;

    private static final Map<Integer, ERefundStatus> map = Stream.of(ERefundStatus.values())
            .collect(Collectors.toMap(e -> e.index, e -> e));

    ERefundStatus(int index) {
        this.index = index;
    }

    public static ERefundStatus valueOf(int index) {
        return map.getOrDefault(index, null);
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
