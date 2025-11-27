package com.evcharge.entity.device;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控设备;
 *
 * @author : JED
 * @date : 2023-6-13
 */
public class MonitorDeviceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩id
     */
    public long CSId;
    /**
     * 品牌
     */
    public String brand;
    /**
     * 品牌代码
     */
    public String brandCode;
    /**
     * 设备型号
     */
    public String deviceModel;
    /**
     * 设备名
     */
    public String deviceName;
    /**
     * 序列号
     */
    public String serialNumber;
    /**
     * 用户名
     */
    public String username;
    /**
     * 密码
     */
    public String password;
    /**
     * 验证码
     */
    public String verifyCode;
    /**
     * 配置
     */
    public String config;
    /**
     * SIM卡号
     */
    public String simCode;
    /**
     * 0-出库，1-入库
     */
    public int inventory_status;
    /**
     * 0-离线，1-在线
     */
    public int online_status;
    /**
     * 状态：0-离线，1-在线
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static MonitorDeviceEntity getInstance() {
        return new MonitorDeviceEntity();
    }

    /**
     * 读取设备信息
     *
     * @param serialNumber 序列号
     * @return 监控设备实体类
     */
    public MonitorDeviceEntity getWithSerialNumber(String serialNumber) {
        return getWithSerialNumber(serialNumber, true);
    }

    /**
     * 读取设备信息
     *
     * @param serialNumber 序列号
     * @param inCache      是否优先读取缓存
     * @return 监控设备实体类
     */
    public MonitorDeviceEntity getWithSerialNumber(String serialNumber, boolean inCache) {
        if (inCache) this.cache(String.format("MonitorDevice:%s:Details", serialNumber));
        return this.where("serialNumber", serialNumber)
                .findEntity();
    }

    /**
     * 获取绑定的充电桩信息
     *
     * @param serialNumber 序列号
     * @return 充电桩实体类信息
     */
    public ChargeStationEntity getChargeStation(String serialNumber) {
        return this.field("a.*")
                .cache(String.format("ChargeStation:MonitorDevice:%s", serialNumber))
                .alias("a")
                .join(MonitorDeviceEntity.getInstance().theTableName(), "b", "a.CSId = b.CSId")
                .where("b.serialNumber", serialNumber)
                .findEntity();
    }

    /**
     * 获取设备配置
     *
     * @return JSON格式配置
     */
    public JSONObject getDeviceConfig() {
        if (!StringUtils.hasLength(this.config)) return null;
        return JSONObject.parse(this.config);
    }

    /**
     * 修复simCode
     *
     * @param serialNumber 序列号
     * @param simCode      sim编码
     * @return
     */
    public boolean updateSimCode(String serialNumber, String simCode) {
        // 参数有效性校验
        if (!StringUtils.hasLength(serialNumber) || !StringUtils.hasLength(simCode)) {
            return false;
        }

        MonitorDeviceEntity deviceEntity = getWithSerialNumber(serialNumber);
        // 设备不存在，直接返回false
        if (deviceEntity == null) return false;
        // 如果sim已经更新并且设备已是主机或已有有效的主机设备ID，无需进一步更新
        if (deviceEntity.simCode.equalsIgnoreCase(simCode)) return true;

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("simCode", simCode);
        // 更新设备信息
        deviceEntity.where("serialNumber", serialNumber).update(set_data);
        LogsUtil.info(this.getClass().getSimpleName(), "[%s] 修复监控设备simCode", serialNumber);

        DataService.getMainCache().del(String.format("MonitorDevice:%s:Details", serialNumber));
        return true;
    }

    //region 设备监控

    /**
     * 设备监控任务
     *
     * @param serialNumber 序列号
     * @return
     */
    public SyncResult monitorTask(String serialNumber) {
        // 获取设备状态
        int status = DataService.getMainCache().getInt(String.format("MonitorDevice:%s:status", serialNumber), -1);
        if (status != -1) return new SyncResult(0, "");

        // 获取设备实体
        MonitorDeviceEntity deviceEntity = MonitorDeviceEntity.getInstance().getWithSerialNumber(serialNumber, false);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备信息不完整。");

        // 检查设备是否被标记为已拆除
        if (deviceEntity.online_status == 99) return new SyncResult(99, "设备已拆除。");

        // 更新设备的离线状态
        deviceEntity.where("serialNumber", serialNumber).update(new LinkedHashMap<>() {{
            put("online_status", 0);// 在线状态：0=离线，1=在线
        }});
        return new SyncResult(1, "设备已离线。");
    }

    //endregion
}
