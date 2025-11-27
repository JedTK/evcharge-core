package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 收费模式
 */
public enum EPaymentOrderType {
    /**
     * 先付后用
     */
    PaymentBeforeUse(1),
    /**
     * 先用后付
     */
    UseBeforePayment(2);

    public final int index;

    private static Map<Integer, EPaymentOrderType> map = Stream.of(EPaymentOrderType.values()).collect(Collectors.toMap(e -> e.index, e -> e));

    EPaymentOrderType(int index) {
        this.index = index;
    }

    public static EPaymentOrderType valueOf(int index) {
        EPaymentOrderType v = map.get(index);
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
