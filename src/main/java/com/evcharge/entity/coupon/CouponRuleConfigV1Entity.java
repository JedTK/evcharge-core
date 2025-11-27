package com.evcharge.entity.coupon;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 优惠券配置;
 * @author : JED
 * @date : 2024-8-14
 */
public class CouponRuleConfigV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 配置名
     */
    public String setting_name ;
    /**
     * 关联coupon_rule_type_v1规则类型:满减券/时长券/折扣券
     */
    public long rule_type ;
    /**
     * 发放总量：0-不限制，1-限制
     */
    public int count_stint ;
    /**
     * 金额使用限制：0-不限制，1-满多少元可用
     */
    public int amount_use_stint ;
    /**
     * 有效期限制：0-无期限，1-固定日期，2-领取卷当日开始N天内有效，3-领到卷次日开始N天内有效
     */
    public int expired_stint ;
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
    public static CouponRuleConfigV1Entity getInstance() {
        return new CouponRuleConfigV1Entity();
    }
}