package com.evcharge.entity.coupon;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 优惠券类型表;
 * @author : JED
 * @date : 2024-8-14
 */
public class CouponRuleTypeV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 父级id
     */
    public long parent_id ;
    /**
     * 类型名称
     */
    public String name ;
    /**
     * 代码
     */
    public String code ;
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
    public static CouponRuleTypeV1Entity getInstance() {
        return new CouponRuleTypeV1Entity();
    }
}