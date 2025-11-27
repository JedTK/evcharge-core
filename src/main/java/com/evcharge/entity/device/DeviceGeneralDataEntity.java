package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备一般数据;
 *
 * @author : JED
 * @date : 2024-1-11
 */
public class DeviceGeneralDataEntity extends BaseEntity implements Serializable {
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
     * 当前信号强度，心跳包更新
     */
    public Double strength;
    /**
     * 当前环境温度，心跳包更新
     */
    public Double temperature;
    /**
     * 当前电压，心跳包更新
     */
    public Double voltage;
    /**
     * IMEI编码
     */
    public String IMEI;
    /**
     * SIM卡号
     */
    public String simcode;
    /**
     * 最后警告消息
     */
    public String lastWarnMessage;
    /**
     * 模块类型
     */
    public String module;
    /**
     * 模块版本号
     */
    public String moduleVersion;
    /**
     * 设备版本号
     */
    public String version;
    /**
     * 频率
     */
    public Double frequency;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * 额外数据
     */
    public String extra;
    //endregion

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static DeviceGeneralDataEntity getInstance() {
        return new DeviceGeneralDataEntity();
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
