package com.evcharge.entity.device;


import com.evcharge.libsdk.windy.WindySDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备列表,一行数据表示一个具体的设备实体
 *
 * @author : JED
 * @date : 2022-9-15
 */
@Getter
@Setter
public class DeviceEntity extends BaseEntity implements Serializable {
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
     * 设备编码，用于表示单独的一个实体
     */
    public String deviceCode;
    /**
     * 物理编号，设备上可以看到的编号
     */
    public String deviceNumber;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 关联的设备单元ID
     */
    public long deviceUnitId;
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
     * 在线状态：0=离线，1=在线
     */
    public int online_status;
    /**
     * 备注
     */
    public String remark;
    /**
     * 当前信号强度，心跳包更新
     */
    public double strength;
    /**
     * 当前环境温度，心跳包更新
     */
    public double temperature;
    /**
     * 当前电压，心跳包更新
     */
    public double voltage;
    /**
     * SIM卡号
     */
    public String simCode;
    /**
     * 主机：0=否，1=是
     */
    public int isHost;
    /**
     * 主机ID
     */
    public long hostDeviceId;
    /**
     * 收费标准配置
     */
    public long chargeStandardConfigId;
    /**
     * 充电时长配置
     */
    public long chargeTimeConfigId;
    /**
     * 停车收费配置ID
     */
    public long parkingConfigId;
    /**
     * 安全充电保险，0=不启用，1=启用
     */
    public int safeCharge;
    /**
     * 安全充电保险费用
     */
    public double safeChargeFee;
    /**
     * 0=出库，1=入库
     */
    public int inventory_status;
    /**
     * 电表ID
     */
    public long meterId;
    /**
     * 是否为测试，0=否，1=是
     */
    public int isTest;
    /**
     * 最后警告消息
     */
    public String last_warn_msg;
    /**
     * 心跳包更新时间
     */
    public long heartbeat_update_time;
    /**
     * 充电柜标识：0-否，1-是
     */
    public int is_cabinet;
    /**
     * 充电平台代码
     */
    public String platform_code;
    /**
     * 充电平台充电桩代码，表示此充电桩所在平台的唯一编码
     */
    public String platform_cs_id;
    /**
     * 应用通道编码
     */
    public String appChannelCode;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 显示状态：0=不显示，1=显示
     */
    public int display_status;
    /**
     * 通信的服务器IPv4地址
     */
    public String server_IPv4;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static DeviceEntity getInstance() {
        return new DeviceEntity();
    }

    /**
     * 通过id获取真正的设备号
     */
    public String getDeviceCodeWithId(long id) {
        Map<String, Object> data = this.field("id,deviceCode")
                .cache(String.format("Device:%s:DeviceCode", id), 86400000 * 7)
                .where("id", id)
                .find();
        if (data == null || data.isEmpty()) return "";
        return MapUtil.getString(data, "deviceCode");
    }

    /**
     * 通过设备码获取数据id
     *
     * @param deviceCode
     * @return
     */
    public long getIdWithDeviceCode(String deviceCode) {
        Map<String, Object> data = this.field("id")
                .cache(String.format("Device:%s:id", deviceCode), 86400000 * 7)
                .where("deviceCode", deviceCode)
                .find();
        if (data == null || data.isEmpty()) return 0;
        return MapUtil.getLong(data, "id");
    }

    /**
     * 读取设备信息（优先从缓存中读取）
     *
     * @param deviceCode 设备码
     * @return
     */
    public DeviceEntity getWithDeviceCode(String deviceCode) {
        return getWithDeviceCode(deviceCode, true);
    }

    /**
     * 读取设备信息
     *
     * @param deviceCode 设备码
     * @param inCache    是否优先读取缓存
     * @return
     */
    public DeviceEntity getWithDeviceCode(String deviceCode, boolean inCache) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance();
        if (inCache) deviceEntity.cache(String.format("Device:%s:Details", deviceCode));
        deviceEntity = deviceEntity.where("deviceCode", deviceCode).findEntity();
        return deviceEntity;
    }

    /**
     * 读取主机设备信息
     *
     * @param simCode SIM编码
     * @return
     */
    public DeviceEntity getHostWithSimCode(String simCode) {
        return getWithDeviceCode(simCode, true);
    }

    /**
     * 读取主机设备信息
     *
     * @param simCode SIM编码
     * @param inCache 是否优先读取缓存
     * @return
     */
    public DeviceEntity getHostWithSimCode(String simCode, boolean inCache) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance();
        if (inCache) deviceEntity.cache(String.format("Device:Host:%s", simCode));
        deviceEntity = deviceEntity
                .where("isHost", 1)
                .where("simCode", simCode)
                .findEntity();
        return deviceEntity;
    }

    /**
     * 读取设备信息
     *
     * @param deviceNumber 设备物理号
     * @return
     */
    public DeviceEntity getWithDeviceNumber(String deviceNumber) {
        return getWithDeviceNumber(deviceNumber, true);
    }

    /**
     * 读取设备信息
     *
     * @param deviceNumber 设备物理号
     * @param inCache      是否优先读取缓存
     * @return
     */
    public DeviceEntity getWithDeviceNumber(String deviceNumber, boolean inCache) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance();
        if (inCache) deviceEntity.cache(String.format("Device:%s:Details", deviceNumber));
        deviceEntity = deviceEntity.where("deviceNumber", deviceNumber)
                .findEntity();
        return deviceEntity;
    }

    /**
     * 通过设备码获取设备的所有信息
     *
     * @param deviceCode 设备码
     * @return
     */
    public Map<String, Object> getAllParamsWithDeviceCode(String deviceCode) {
        SyncResult r = this.beginTransaction(connection -> {
            Map<String, Object> params = new LinkedHashMap<>();

            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode, false);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(1, "no data");

            params.put("id", deviceEntity.id);
            params.put("deviceName", deviceEntity.deviceName);
            params.put("deviceCode", deviceEntity.deviceCode);
            params.put("deviceNumber", deviceEntity.deviceNumber);
            params.put("isHost", deviceEntity.isHost);

            Map<String, Object> deviceUnit = DeviceUnitEntity.getInstance().getWithUnitId(deviceEntity.deviceUnitId);
            if (deviceUnit != null && !deviceUnit.isEmpty()) {
                params.put("deviceUnitName", deviceUnit.get("name"));
                params.put("previewImage", deviceUnit.get("previewImage"));
                params.put("maxPower", deviceUnit.get("maxPower"));
                params.put("brandName", deviceUnit.get("brandName"));
                params.put("TypeName", deviceUnit.get("TypeName"));
            }

            return new SyncResult(0, params);
        });
        if (r.code != 0) return new LinkedHashMap<>();
        return (Map<String, Object>) r.data;
    }

    /**
     * 识别二维码内容，并返回设备号端口等信息
     *
     * @param content
     * @return
     */
    public Map<String, Object> analysisQRCode(String content) {
        content = content.trim();

        Map<String, Object> data = new LinkedHashMap<>();
        String deviceCode = "";
        String deviceNumber = "";
        int port = -1;
        try {
            //https://centerapi.genkigo.net/windy/8FA3B704/00
            //https://centerapi.genkigo.net/windy/206BE691
            String[] arr = content.split("/");
            if (arr.length == 6) {
                deviceCode = arr[arr.length - 2];
                deviceNumber = WindySDK.getInstance().convertDeviceNumber(deviceCode);
                port = Integer.parseInt(arr[arr.length - 1], 16);
            } else if (arr.length == 5) {
                deviceCode = arr[arr.length - 1];
                deviceNumber = WindySDK.getInstance().convertDeviceNumber(deviceCode);
            }

            data.put("deviceCode", deviceCode.toUpperCase());
            data.put("deviceNumber", deviceNumber);
            data.put("port", port);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "识别二维码内容发生错误：%s", content);
        }
        return data;
    }
}
