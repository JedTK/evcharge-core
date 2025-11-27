package com.evcharge.entity.chargecard;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 用户充电卡站点限制;
 *
 * @author : JED
 * @date : 2023-12-7
 */
public class UserChargeCardStationLimitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 卡号
     */
    public String cardNumber;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * 允许的充电桩，空值或All表示所有都可以使用，如果存在太多的数据，可以新增多一条进行限制
     */
    public String allow_cs_ids;

    //endregion

    /**
     * 获得一个实例
     */
    public static UserChargeCardStationLimitEntity getInstance() {
        return new UserChargeCardStationLimitEntity();
    }

    /**
     * 根据充电卡号获取站点限制配置
     *
     * @param cardNumber 充电卡号码
     * @return 充电卡站点限制配置
     */
    public UserChargeCardStationLimitEntity getWithCardNumber(String cardNumber) {
        return getWithCardNumber(cardNumber, true);
    }

    /**
     * 根据充电卡号获取站点限制配置
     *
     * @param cardNumber 充电卡号码
     * @param inCache    是否优先从缓存中获取
     * @return 充电卡站点限制配置
     */
    public UserChargeCardStationLimitEntity getWithCardNumber(String cardNumber, boolean inCache) {
        this.where("cardNumber", cardNumber);
        if (inCache) this.cache(String.format("User:ChargeCard:StationLimit:%s", cardNumber));
        return this.findEntity();
    }
}
