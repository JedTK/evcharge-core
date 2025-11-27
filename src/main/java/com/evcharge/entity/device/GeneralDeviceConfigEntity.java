package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 一般设备配置;
 *
 * @author : JED
 * @date : 2024-5-22
 */
public class GeneralDeviceConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 序列号
     */
    public String serialNumber;
    /**
     * 配置信息，JSON数据
     */
    public String config;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static GeneralDeviceConfigEntity getInstance() {
        return new GeneralDeviceConfigEntity();
    }

    /**
     * 查询设备详情
     * @param serialNumber
     * @param inCache
     * @return
     */
    public GeneralDeviceConfigEntity getBySerialNumber(String serialNumber, boolean inCache) {
        GeneralDeviceConfigEntity entity = new GeneralDeviceConfigEntity();
        if (inCache) {
            entity.cache(String.format("BaseData:GeneralDevice:%s:Config", serialNumber));
        }
        return entity
                .where("serialNumber", serialNumber)
                .findEntity();
    }
}
