package com.evcharge.entity.device;

import com.xyzs.entity.BaseEntity;

/**
 * 充电设备和通用设备视图实体类
 */
public class EGDeviceViewEntity extends BaseEntity {
    //region -- 实体类属性 --
    /**
     * 视图主键，对应 Device.id 或 GeneralDevice.id
     */
    public long id;

    /**
     * 设备来源类型：
     * CHARGE  - 来自充电设备表 Device
     * GENERAL - 来自通用设备表 GeneralDevice
     */
    public String deviceKind;

    /**
     * 充电站 ID，对应 Device.CSId / GeneralDevice.CSId
     */
    public String CSId;

    /**
     * 设备名称，对应 Device.deviceName / GeneralDevice.deviceName
     */
    public String deviceName;

    /**
     * 统一的“设备编码”：
     * - 对于充电设备：对应 Device.deviceCode
     * - 对于通用设备：对应 GeneralDevice.serialNumber
     */
    public String deviceCode;

    /**
     * 产品 SPU 代码，对应 Device.spuCode / GeneralDevice.spuCode
     */
    public String spuCode;

    /**
     * 品牌编码，对应 Device.brandCode / GeneralDevice.brandCode
     */
    public String brandCode;

    /**
     * 类型编码，对应 Device.typeCode / GeneralDevice.typeCode
     */
    public String typeCode;

    /**
     * 在线状态：
     * 0 - 离线
     * 1 - 在线
     * 其他值（例如 2、3）在通用设备中可能表示休眠、升级中等状态
     */
    public Integer onlineStatus;

    /**
     * 组织代码，对应 Device.organize_code / GeneralDevice.organize_code
     */
    public String organizeCode;

    /**
     * 平台代码，对应 Device.platform_code / GeneralDevice.platform_code
     */
    public String platformCode;

    /**
     * 应用通道编码，对应 Device.appChannelCode / GeneralDevice.appChannelCode
     */
    public String appChannelCode;

    /**
     * 创建时间戳（毫秒或秒视你的 BaseEntity 约定）
     */
    public long createTime;

    /**
     * 更新时间戳
     */
    public long updateTime;
    // endregion
}
