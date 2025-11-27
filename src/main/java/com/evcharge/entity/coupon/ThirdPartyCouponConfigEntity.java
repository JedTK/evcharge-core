package com.evcharge.entity.coupon;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 第三方券配置;
 *
 * @author : Jay
 * @date : 2024-8-14
 */
public class ThirdPartyCouponConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 券商id
     */
    public long dealer_id;
    /**
     * 类型,关联coupon_rule_type_v1 满减券/时长券/折扣券
     */
    public long type_id;
    /**
     * 关联charge_coupon_rule_v1表，优惠券id
     */
    public long coupon_id;
    /**
     * 映射id
     */
    public String mapping_id;
    /**
     * 状态 0=未发放 1=已发放 2=已核销
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return 实体类
     */
    public static ThirdPartyCouponConfigEntity getInstance() {
        return new ThirdPartyCouponConfigEntity();
    }


    /**
     *
     * @param dealerId 券商id
     * @param mappingId 映射id
     * @return 实体类
     */
    public ThirdPartyCouponConfigEntity getConfigByMappingId(long dealerId, String mappingId) {
        return this
                .where("dealer_id",dealerId)
                .where("mapping_id", mappingId)
                .cache(String.format("ThirdParty:Coupon:Config:Mapping:%s:%s", dealerId, mappingId), 86400 * 1000)
                .findEntity();
    }


}