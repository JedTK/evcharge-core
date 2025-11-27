package com.evcharge.entity.payment;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付类型统计;
 *
 * @author : JED
 * &#064;date  : 2024-4-24
 */
public class PaymentTypeSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 支付ID
     */
    public long paymentTypeId;
    /**
     * 支付标题
     */
    public String paymentTypeText;
    /**
     * 充值订单数
     */
    public int rechargeOrderCount;
    /**
     * 充值金额（毛）
     */
    public BigDecimal rechargeAmount;
    /**
     * 充值人数
     */
    public int rechargeUsers;
    /**
     * 充值退款订单数
     */
    public int rechargeRefundOrderCount;
    /**
     * 充值退款订单金额
     */
    public BigDecimal rechargeRefundAmount;
    /**
     * 充电卡订单数
     */
    public int chargeCardOrderCount;
    /**
     * 充电卡金额（毛）
     */
    public BigDecimal chargeCardAmount;
    /**
     * 充电卡人数
     */
    public int chargeCardUsers;
    /**
     * 充电卡退款订单数
     */
    public int chargeCardRefundOrderCount;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal chargeCardRefundAmount;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static PaymentTypeSummaryEntity getInstance() {
        return new PaymentTypeSummaryEntity();
    }
}
