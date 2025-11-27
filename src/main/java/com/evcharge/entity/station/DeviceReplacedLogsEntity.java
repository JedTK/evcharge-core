package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电设备更换记录;
 *
 * @author : JED
 * @date : 2023-7-10
 */
public class DeviceReplacedLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 维修记录订单id
     */
    public long orderId;
    /**
     * 旧的充电设备编码
     */
    public String oldDeviceCode;
    /**
     * 新的充电设备编码
     */
    public String newDeviceCode;
    /**
     * 备注
     */
    public String remark;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceReplacedLogsEntity getInstance() {
        return new DeviceReplacedLogsEntity();
    }
}
