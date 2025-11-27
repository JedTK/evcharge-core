package com.evcharge.entity.platform;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电平台 - 充电桩 记录合作平台的充电桩id关系;
 *
 * @author : JED
 * @date : 2023-11-23
 */
public class EvPlatformToChargeStationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电平台代码，表示此充电桩所属平台
     */
    public String platform_code;
    /**
     * 合作平台充电桩代码，表示此充电桩所在平台的唯一编码
     */
    public String platform_cs_id;
    /**
     * 自己平台充电桩唯一编号
     */
    public String CSId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static EvPlatformToChargeStationEntity getInstance() {
        return new EvPlatformToChargeStationEntity();
    }
}