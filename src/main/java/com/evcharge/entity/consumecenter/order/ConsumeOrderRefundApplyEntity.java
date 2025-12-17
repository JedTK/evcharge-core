package com.evcharge.entity.consumecenter.order;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单退款申请;
 *
 * @author : Jay
 * @date : 2025-12-16
 */
public class ConsumeOrderRefundApplyEntity extends BaseEntity implements Serializable {
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
     * 申请编号,;
     */
    public String apply_sn;
    /**
     * 产品类型 关联EvcProductsType表 用于支付回调使用策略模式,;
     */
    public String product_type;
    /**
     * 订单号 可以有多个订单 需要用in查询,;
     */
    public String order_sn;
    /**
     * 退款金额,;
     */
    public BigDecimal refund_amount;
    /**
     * 申请退款金额,;
     */
    public BigDecimal apply_refund_amount;
    /**
     * 退款理由,;
     */
    public String refund_reason;
    /**
     * 退款描述,;
     */
    public String refund_description;
    /**
     * 退款时间,;
     */
    public long refund_time;
    /**
     * 申请状态 1=申请中 2=已处理, 3=用户取消 4=后台管理员取消;
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
    public static ConsumeOrderRefundApplyEntity getInstance() {
        return new ConsumeOrderRefundApplyEntity();
    }
}