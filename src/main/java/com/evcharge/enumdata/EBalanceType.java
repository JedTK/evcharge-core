package com.evcharge.enumdata;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 余额类型
 */
public enum EBalanceType {
    /**
     * 充值
     */
    RECHARGE(1001),

    /**
     * 提现
     */
    WITHDRAW(1002),

    /**
     * 转账
     */
    TRANSFER(1003),

    /**
     * 支付
     */
    PAYMENT(1004),

    /**
     * 收益
     */
    INCOME(1005),

    /**
     * 奖励
     */
    REWARD(1006),

    /**
     * 退款
     */
    REFUND(1010),
    /**
     * 冻结
     */
    LOCKED(1111),
    /**
     * 解冻
     */
    UNLOCKED(1112),
    ;

    public final int index;

    private static final Map<Integer, EBalanceType> map = Stream.of(EBalanceType.values())
            .collect(Collectors.toMap(e -> e.index, e -> e));

    EBalanceType(int index) {
        this.index = index;
    }

    public static EBalanceType valueOf(int index) {
        EBalanceType v = map.get(index);
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

