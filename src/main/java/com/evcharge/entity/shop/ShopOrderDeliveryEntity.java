package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 订单物流表;
 * @author : Jay
 * @date : 2022-12-26
 */
@TargetDB("evcharge_shop")
public class ShopOrderDeliveryEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 订单id
     */
    public long order_id ;
    /**
     * 订单运费id
     */
    public long order_fare_id ;
    /**
     * 订单收货地址id
     */
    public long order_transport_id ;
    /**
     * 物流接口code
     */
    public String delivery_apicode ;
    /**
     * 快递公司id
     */
    public long delivery_id ;
    /**
     * 快递公司名
     */
    public String delivery_name ;
    /**
     * 运单号
     */
    public String waybill_id ;
    /**
     * 发货运单信息
     */
    public String waybill_data ;
    /**
     * 状态：0=未发货，1=已发货，2=确认收货
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopOrderDeliveryEntity getInstance() {
        return new ShopOrderDeliveryEntity();
    }
}