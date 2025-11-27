package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备单元 - 插座;
 *
 * @author : JED
 * @date : 2022-10-25
 */
public class DeviceUnitSocketEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 关联的设备单元ID
     */
    public long deviceUnitId;
    /**
     * 关联的插座单元ID;表示此设备拥有什么插座
     */
    public long socketId;
    /**
     * 插座编号
     */
    public int index;
    /**
     * 端口号
     */
    public int port;

    /**
     * 单口限制充电功率
     */
    public double limitChargePower;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceUnitSocketEntity getInstance() {
        return new DeviceUnitSocketEntity();
    }

    /**
     * 查询设备单元端口数据
     *
     * @param deviceUnitId 设备单元id
     * @return 端口单元数据
     */
    public List<DeviceUnitSocketEntity> getListWithDeviceUnitId(long deviceUnitId) {
        return getListWithDeviceUnitId(deviceUnitId, true);
    }

    /**
     * 查询设备单元端口数据
     *
     * @param deviceUnitId 设备单元id
     * @param inCache      是否优先从缓存中获得
     * @return 端口单元数据
     */
    public List<DeviceUnitSocketEntity> getListWithDeviceUnitId(long deviceUnitId, boolean inCache) {
        if (deviceUnitId == 0) return new ArrayList<>();
        if (inCache) this.cache(String.format("BaseData:DeviceUnitSocket:%s", deviceUnitId));
        return this.where("deviceUnitId", deviceUnitId)
                .page(1, 100)
                .selectList();
    }
}
