package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电卡配置省份限制;
 *
 * @author : JED
 * @date : 2023-12-7
 */
public class ChargeCardConfigDistrictLimitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电卡配置ID(待删除)
     */
    @Deprecated
    public long config_id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     *
     */
    public String code;
    /**
     * 行政区域
     */
    public String district;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeCardConfigDistrictLimitEntity getInstance() {
        return new ChargeCardConfigDistrictLimitEntity();
    }
}