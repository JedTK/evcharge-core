package com.evcharge.entity.device;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 驱动类型;
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class DeviceTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 名称
     */
    public String name;
    /**
     * 设备类型Code
     */
    public String typeCode;
    /**
     * 备注
     */
    public String remark;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceTypeEntity getInstance() {
        return new DeviceTypeEntity();
    }
}
