package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 物流和后续处理状态
 */
public enum EDeliveryStatus {
    /**
     * 无状态
     */
    None(-1),
    /**
     * 待发货
     */
    AwaitingShipment(0),
    /**
     * 已发货
     */
    Shipped(1),
    /**
     * 已收货
     */
    Delivered(2),
    /**
     * 待评价
     */
    AwaitingReview(3),
    /**
     * 已完成
     */
    Completed(4),
    /**
     * 换货中
     */
    Exchanging(5),
    /**
     * 换货完成
     */
    ExchangeCompleted(6);

    public final int index;

    private static final Map<Integer, EDeliveryStatus> map = Stream.of(EDeliveryStatus.values())
            .collect(Collectors.toMap(e -> e.index, e -> e));

    EDeliveryStatus(int index) {
        this.index = index;
    }

    public static EDeliveryStatus valueOf(int index) {
        return map.getOrDefault(index, null);
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
