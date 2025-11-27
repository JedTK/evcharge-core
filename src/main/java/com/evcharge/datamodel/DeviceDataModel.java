package com.evcharge.datamodel;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceUnitEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 设备信息 数据模型，用于快速查询数据模型，特别是多表的数据模型
 */
@Getter
public class DeviceDataModel {
    /**
     * 设备基础信息
     */
    public DeviceEntity device;
    /**
     * 充电桩信息
     */
    public ChargeStationEntity cs;
    /**
     * 设备单元信息
     */
    public DeviceUnitEntity unit;

    /**
     * 实例化
     *
     * @return 数据模型
     */
    public static DeviceDataModel getInstance() {
        return new DeviceDataModel();
    }

    /**
     * 通过设备编码获取设备信息
     *
     * @param deviceCode 设备编码
     * @return 设备信息数据模型
     */
    public DeviceDataModel getWithDeviceCode(String deviceCode) {
        this.device = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (this.device == null) return null;

        this.cs = ChargeStationEntity.getInstance().getWithDeviceCode(deviceCode);
        if (this.cs == null) {
            this.cs = new ChargeStationEntity();
        }

        this.unit = DeviceUnitEntity.getInstance().getWithSpuCode(this.device.getSpuCode());
        if (this.unit == null) {
            this.unit = new DeviceUnitEntity();
        }

        return this;
    }

    /**
     * 获取组织代码
     *
     * @return 组织代码
     */
    public String getOrganizCode() {
        return getOrganizCode("");
    }

    /**
     * 获取组织代码
     *
     * @param defaultValue 默认值
     * @return 组织代码
     */
    public String getOrganizCode(String defaultValue) {
        if (this.cs == null || this.cs.id == 0) return defaultValue;
        if (!StringUtils.hasLength(this.cs.organize_code)) return defaultValue;
        return this.cs.organize_code;
    }

    /**
     * 获取省市组合
     *
     * @return 省市组合字符串
     */
    public String getProvinceCity() {
        if (this.cs == null || this.cs.id == 0) return "";
        StringBuilder v = new StringBuilder();
        if (StringUtils.hasLength(this.cs.province)) {
            v.append(this.cs.province);
        }
        if (StringUtils.hasLength(this.cs.city)) {
            v.append(" ").append(this.cs.city);
        }
        return v.toString().trim();
    }

    /**
     * 获取区、街道、社区组合
     *
     * @return 区、街道、社区组合字符串
     */
    public String getDistrictStreetCommunities() {
        if (this.cs == null || this.cs.id == 0) return "";
        StringBuilder v = new StringBuilder();
        if (StringUtils.hasLength(this.cs.district)) {
            v.append(this.cs.district);
        }
        if (StringUtils.hasLength(this.cs.street)) {
            v.append(" ").append(this.cs.street);
        }
        if (StringUtils.hasLength(this.cs.communities)) {
            v.append(" ").append(this.cs.communities);
        }
        return v.toString().trim();
    }

    /**
     * 获取路、详细地址（门牌）组合
     *
     * @return 路、详细地址（门牌）组合字符串
     */
    public String getRoadsAddress() {
        if (this.cs == null || this.cs.id == 0) return "";
        StringBuilder v = new StringBuilder();
        if (StringUtils.hasLength(this.cs.roads)) {
            v.append(this.cs.roads);
        }
        if (StringUtils.hasLength(this.cs.address)) {
            v.append(" ").append(this.cs.address);
        }
        return v.toString().trim();
    }
}
