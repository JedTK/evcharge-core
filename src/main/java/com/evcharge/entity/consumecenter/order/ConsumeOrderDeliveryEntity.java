package com.evcharge.entity.consumecenter.order;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 订单物流表;null
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeOrderDeliveryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 物流ID,;
     */
    public long id;
    /**
     * 关联订单ID,;
     */
    public long order_id;
    /**
     * 订单编号,;
     */
    public String order_sn;
    /**
     * 物流单号,;
     */
    public String tracking_no;
    /**
     * 物流公司,;
     */
    public String carrier;
    /**
     * 收件人姓名,;
     */
    public String recipient_name;
    /**
     * 收件人电话,;
     */
    public String recipient_phone;
    /**
     * 收货地址,;
     */
    public String address;
    /**
     * 物流状态 SHIPPED, DELIVERED,;
     */
    public String status;
    /**
     * 发货时间,;
     */
    public long shipment_time;
    /**
     * 创建时间,;
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
    public static ConsumeOrderDeliveryEntity getInstance() {
        return new ConsumeOrderDeliveryEntity();
    }
}