package com.evcharge.entity.consumecenter.order;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单退款申请明细;
 *
 * @author : Jay
 * @date : 2025-12-16
 */
public class ConsumeOrderRefundApplyDetailEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 申请id,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 申请编号,用于关联ConsumeOrderRefundApply,;
     */
    public String apply_sn;
    /**
     * 消费订单号 关联消费订单表,;
     */
    public String consume_order_sn;
    /**
     * 订单金额,;
     */
    public String order_amount;
    /**
     * 退款金额,;
     */
    public BigDecimal refund_amount;
    /**
     * 申请状态 1=申请中 2=已处理,;
     */
    public int refund_status;
    /**
     * 创建时间,;
     */
    public long created_time;
    /**
     * 更新时间,;
     */
    public long updated_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static ConsumeOrderRefundApplyDetailEntity getInstance() {
        return new ConsumeOrderRefundApplyDetailEntity();
    }
}