package com.evcharge.entity.consumecenter.order;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单退款表;null
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeOrderRefundsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 退款单ID,;
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 关联订单ID,;
     */
    public long order_id;
    /**
     * 关联订单表 订单编号,;
     */
    public String order_sn;
    /**
     * 退款订单编号,;
     */
    public String refund_order_sn;
    /**
     * 退款金额,;
     */
    public BigDecimal refund_amount;
    /**
     * 退款原因,;
     */
    public String refund_reason;
    /**
     * 退款银行订单号,;
     */
    public String refund_bank_order_no;
    /**
     * 退款银行流水号,;
     */
    public String refund_bank_trx_no;
    /**
     * 退款状态 PENDING SUCCESS FAILED;
     */
    public String status;
    /**
     * 退款申请时间,;
     */
    public long create_time;
    /**
     * 退款完成时间,;
     */
    public long update_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static ConsumeOrderRefundsEntity getInstance() {
        return new ConsumeOrderRefundsEntity();
    }
}