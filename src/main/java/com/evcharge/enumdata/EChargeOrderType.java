package com.evcharge.enumdata;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 充电订单类型：1=电动自行车充电订单，2=充电柜充电订单，3=新能源汽车充电订单
 */
public enum EChargeOrderType {
    /**
     * 电动自行车充电订单
     */
    EBike(1),
    /**
     * 充电柜充电订单
     */
    Cabinet(2),
    /**
     * 新能源汽车充电订单
     */
    NEVCar(3);

    public final int index;

    private static Map<Integer, EChargeOrderType> map = Stream.of(EChargeOrderType.values()).collect(Collectors.toMap(e -> e.index, e -> e));

    EChargeOrderType(int index) {
        this.index = index;
    }

    public static EChargeOrderType valueOf(int index) {
        EChargeOrderType v = map.get(index);
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
