package com.evcharge.entity.station;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电支付方式; 和枚举 EChargePaymentType 相互映射 这里只是记录作用和查询作用
 *
 * @author : JED
 * @date : 2023-12-27
 */
public class ChargePaymentTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 充电支付方式名称
     */
    public String name;
    /**
     * 图标
     */
    public String icon;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargePaymentTypeEntity getInstance() {
        return new ChargePaymentTypeEntity();
    }
}