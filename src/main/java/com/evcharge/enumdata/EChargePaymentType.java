package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 充电支付方式
 */
public enum EChargePaymentType {
    /**
     * 余额
     */
    Balance(1),

    /**
     * 充电卡
     */
    ChargeCard(2),

    /**
     * 积分
     */
    Integral(3);

    public final int index;

    // 创建一个静态映射，以便通过整数值查找对应的枚举项
    private static Map<Integer, EChargePaymentType> map = Stream.of(EChargePaymentType.values())
            .collect(Collectors.toMap(e -> e.index, e -> e));

    EChargePaymentType(int index) {
        this.index = index;
    }

    // 根据整数值返回相应的枚举项，如果找不到则抛出异常
    public static EChargePaymentType valueOf(int index) {
        EChargePaymentType v = map.get(index);
        if (v == null) {
            throw new IllegalArgumentException("argument out of range");
        }
        return v;
    }
}
