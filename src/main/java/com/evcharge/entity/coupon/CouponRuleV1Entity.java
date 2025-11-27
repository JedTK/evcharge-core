package com.evcharge.entity.coupon;


import com.evcharge.entity.shop.ShopCouponRuleV1Entity;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券规则;
 * @author : Jay
 * @date : 2024-8-14
 */
public class CouponRuleV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 关联coupon_rule_type_v1规则类型:满减券/时长券/折扣券
     */
    public long rule_type ;
    /**
     * 配置id 关联config_id
     */
    public long setting_id ;
    /**
     * 规则key
     */
    public String rule_key ;
    /**
     * 规则名称
     */
    public String rule_name ;
    /**
     * 副标题
     */
    public String subtitle ;
    /**
     * 状态 0=未启用 1=已启动
     */
    public int status ;
    /**
     * 使用状态 0=未使用 1=已使用
     */
    public int use_status ;
    /**
     * 优惠金额
     */
    public BigDecimal amount ;
    /**
     * 折扣金额
     */
    public BigDecimal discount ;
    /**
     * 使用时长，毫秒级时间 比如2小时是 60*60*2*1000
     */
    public long charge_duration ;
    /**
     * 使用限制：金额限制：满xx元减xx元，满xx元打xx折
     */
    public BigDecimal amount_factor ;
    /**
     * 使用限制：金额限制：满xx元减xx元，满xx元打xx折
     */
    public BigDecimal discount_factor ;
    /**
     * 固定日期：生效时间
     */
    public long start_time ;
    /**
     * 固定日期：过期时间
     */
    public long end_time ;
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
    public static CouponRuleV1Entity getInstance() {
        return new CouponRuleV1Entity();
    }

    /**
     * 根据规则id获取优惠券信息
     *
     * @param id
     * @return
     */
    public CouponRuleV1Entity getRuleById(long id) {
        return this.cache(String.format("Platform:Coupon:RuleV1Info:%s", id), 86400 * 1000)
                .where("id", id)
                .findEntity();
    }


}