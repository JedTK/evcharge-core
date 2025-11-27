package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 充电计费类型：按峰值功率收费、按电量收费
 */
public enum EBillingType {
    /**
     * 按峰值功率收费
     */
    ChargeMaxPower(1),
    /**
     * 按电量收费
     */
    Electricity(2);

    public final int index;

    private static Map<Integer, EBillingType> map = Stream.of(EBillingType.values()).collect(Collectors.toMap(e -> e.index, e -> e));

    EBillingType(int index) {
        this.index = index;
    }

    public static EBillingType valueOf(int index) {
        EBillingType v = map.get(index);
        if (v == null) {
            throw new IllegalArgumentException("argument out of range");
        }
        return v;
    }

    @Override
    public String toString() {
        return String.valueOf(this.index);
    }
}
