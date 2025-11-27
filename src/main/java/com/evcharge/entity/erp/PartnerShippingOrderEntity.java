package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合伙人：设备配送订单;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerShippingOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 申领设备订单号
     */
    public String receiveOrderSN;
    /**
     * 状态：-1-取消，0-草稿，1-待审核，2-审核通过，3-审核不通过，4-待发货，5-已发货，6-已完成
     */
    public int status;
    /**
     * 物流公司，可选
     */
    public String deliveryCompany;
    /**
     * 物流运单号，可选
     */
    public String deliveryOrderSN;
    /**
     * 物流运费，可选
     */
    public double deliveryFee;
    /**
     * 备注
     */
    public String remark;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PartnerShippingOrderEntity getInstance() {
        return new PartnerShippingOrderEntity();
    }
}
