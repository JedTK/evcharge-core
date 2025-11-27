package com.evcharge.entity.device;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 一般设备定义;
 *
 * @author : JED
 * @date : 2024-5-22
 */
public class GeneralDeviceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 设备名称
     */
    public String deviceName;
    /**
     * 序列号
     */
    public String serialNumber;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 产品SPU代码
     */
    public String spuCode;
    /**
     * 品牌编码，这里用于快速查询
     */
    public String brandCode;
    /**
     * 类型编码，这里用于快速查询
     */
    public String typeCode;
    /**
     * 状态：0-离线，1-在线
     */
    public int online_status;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * SIM编码
     */
    public String simCode;
    /**
     * 批次号
     */
    public String batchNumber;
    /**
     * 规格，JSON数据
     */
    public String spec;
    /**
     * 动态信息，实时更新，JSON数据
     */
    public String dynamic_info;
    /**
     * 主设备序列号，用于联合设备使用
     */
    public String mainSerialNumber;
    /**
     * 备注
     */
    public String remark;
    /**
     * 组织代码，表示此设备属于哪个组织，一般用于后台查询
     */
    public String organize_code;
    /**
     * 充电平台代码，表示此设备所属平台，一般用于MQTT通信
     */
    public String platform_code;
    /**
     * 应用通道编码
     */
    public String appChannelCode;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 删除时间戳
     */
    public long delete_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static GeneralDeviceEntity getInstance() {
        return new GeneralDeviceEntity();
    }

    /**
     * 查询设备详情
     *
     * @param serialNumber
     * @param inCache
     * @return
     */
    public GeneralDeviceEntity getBySerialNumber(String serialNumber, boolean inCache) {
        GeneralDeviceEntity entity = new GeneralDeviceEntity();
        if (inCache) {
            entity.cache(String.format("BaseData:GeneralDevice:%s", serialNumber));
        }
        return entity
                .where("serialNumber", serialNumber)
                .where("status", 1)
                .findEntity();
    }
}