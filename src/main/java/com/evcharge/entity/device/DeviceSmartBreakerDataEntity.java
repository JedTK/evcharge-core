package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 设备-智能熔断器;
 *
 * @author : JED
 * @date : 2024-1-11
 */
public class DeviceSmartBreakerDataEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 设备编码，用于表示单独的一个实体
     */
    public String deviceCode;
    /**
     * 继电器状态：0-断开，1-吸合
     */
    public int relayStatus;
    /**
     * 电闸状态：0-分闸，1-合闸
     */
    public int gateStatus;
    /**
     * 电闸状态说明
     */
    public String gateStatusRemark;
    /**
     * 锁闸状态：0-解锁，1-锁上
     */
    public int gateLockStatus;
    /**
     * 当前累计电量
     */
    public int powerConsumption;
    /**
     * 电表当前累计电量
     */
    public BigDecimal meterPowerConsumption;
    /**
     * 当前漏电流（mA）
     */
    public BigDecimal lossCurrent;
    /**
     * 当前电流
     */
    public BigDecimal current;
    /**
     * 电表当前电流
     */
    public BigDecimal meterCurrent;
    /**
     * 当前电压
     */
    public float voltage;
    /**
     * 零线温度
     */
    public float zeroCurveTemperature;
    /**
     * 火线温度
     */
    public float firewireTemperature;
    /**
     * 环境温度
     */
    public float temperature;
    /**
     * 最近警报
     */
    public String lastAlarm;
    /**
     * 最近警报时间
     */
    public long lastAlarmTime;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceSmartBreakerDataEntity getInstance() {
        return new DeviceSmartBreakerDataEntity();
    }

    /**
     * 新增或更新数据
     *
     * @param deviceCode 设备编号
     * @param data       数据
     * @return
     */
    public boolean updateData(String deviceCode, Map<String, Object> data) {
        if (!StringUtils.hasLength(deviceCode)) return false;
        data.put("update_time", TimeUtil.getTimestamp());
        int noquery;
        if (!this.where("deviceCode", deviceCode).exist()) {
            data.put("deviceCode", deviceCode);
            noquery = this.insert(data);
        } else {
            noquery = this.where("deviceCode", deviceCode).update(data);
        }
        return noquery > 0;
    }
}
