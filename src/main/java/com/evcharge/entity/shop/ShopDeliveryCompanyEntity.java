package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 快递鸟快递公司列表;
 * @author : Jay
 * @date : 2022-12-30
 */
@TargetDB("evcharge_shop")
public class ShopDeliveryCompanyEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 快递公司编码
     */
    public String delivery_code ;
    /**
     * 快递公司名称
     */
    public String delivery_name ;
    /**
     * 是否支持电子面单下单
     */
    public int can_online_order ;
    /**
     * 是否支持取消订单
     */
    public int can_cancel_order ;
    /**
     * 是否支持查询面单余额, 1表示支持
     */
    public int can_get_quota ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopDeliveryCompanyEntity getInstance() {
        return new ShopDeliveryCompanyEntity();
    }
}