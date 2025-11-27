package com.evcharge.entity.consumecenter.order;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 核心订单表;null
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeOrdersEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 订单ID,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 订单号,;
     */
    public String order_sn;
    /**
     * 产品类型 关联EvcProductsType表 用于支付回调使用策略模式,;
     */
    public String product_type;
    /**
     * 订单总金额,;
     */
    public BigDecimal total_price;
    /**
     * 订单实际金额,;
     */
    public BigDecimal order_price;
    /**
     * 优惠金额,;
     */
    public BigDecimal discount_price;
    /**
     * 消耗积分,;
     */
    public int use_integral;
    /**
     * 优惠券id 关联user_platform_coupon,;
     */
    public long coupon_id;
    /**
     * 备注,;
     */
    public String memo;
    /**
     * 支付方式,;
     */
    public String payment_type;
    /**
     * 已支付金额,;
     */
    public BigDecimal pay_price;
    /**
     * 支付流水号,;
     */
    public String pay_order_sn;
    /**
     * 支付状态;1=未支付 2=已完成 -1=已取消
     */
    public int payment_status;
    /**
     * 支付时间,;
     */
    public long pay_time;
    /**
     * 结算金额,;
     */
    public BigDecimal settle_amount;

    /**
     * 履行状态 PENDING, IN_PROGRESS, FULFILLED, FAILED,;
     */
    public String fulfillment_status;
    /**
     * 退款状态; 0=无退款, 1=部分退款, 2=全额退款,;
     */
    public int refund_status;
    /**
     * 站点id,;
     */
    public String CSId;
    /**
     * 设备编码,;
     */
    public String device_code;
    /**
     * 端口号,;
     */
    public int port;
    /**
     * 组织代码,;
     */
    public String organize_code;
    /**
     * 平台代码,;
     */
    public String platform_code;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static ConsumeOrdersEntity getInstance() {
        return new ConsumeOrdersEntity();
    }
}