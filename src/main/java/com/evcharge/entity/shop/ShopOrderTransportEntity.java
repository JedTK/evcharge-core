package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 订单地址表;
 * @author : Jay
 * @date : 2022-12-26
 */
@TargetDB("evcharge_shop")
public class ShopOrderTransportEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 状态
     */
    public long order_id ;
    /**
     * 收件人姓名
     */
    public String name ;
    /**
     * 收件人联系电话
     */
    public String phone ;
    /**
     * 邮政编码
     */
    public String postal_code ;
    /**
     * 省
     */
    public String province ;
    /**
     * 市
     */
    public String city ;
    /**
     * 区域
     */
    public String area ;
    /**
     * 街道门牌号
     */
    public String details ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopOrderTransportEntity getInstance() {
        return new ShopOrderTransportEntity();
    }
}