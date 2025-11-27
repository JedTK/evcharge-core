package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充点卡类型;
 *
 * @author : JED
 * @date : 2025-3-13
 */
public class ChargeCardTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 标题
     */
    public String title;
    /**
     * 描述
     */
    public String description;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeCardTypeEntity getInstance() {
        return new ChargeCardTypeEntity();
    }
}
