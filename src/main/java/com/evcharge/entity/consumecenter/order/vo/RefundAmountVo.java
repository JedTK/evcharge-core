package com.evcharge.entity.consumecenter.order.vo;

import java.math.BigDecimal;

public class RefundAmountVo {
    public BigDecimal refundCashAmount; // 实际退回支付渠道的现金
    public BigDecimal deductBalance;    // 需要从用户账户扣除的余额（等于当前总余额）
}