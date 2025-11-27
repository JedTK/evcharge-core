package com.evcharge.entity.coupon;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 接收优惠券记录表;
 * @author : Jay
 * @date : 2024-8-14
 */
public class ThirdPartyCouponReceiveLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 优惠券商
     */
    public long dealer_id ;
    /**
     * 内容
     */
    public String content ;
    /**
     * 状态
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
    public static ThirdPartyCouponReceiveLogEntity getInstance() {
        return new ThirdPartyCouponReceiveLogEntity();
    }
}