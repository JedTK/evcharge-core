package com.evcharge.entity.device.meter;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 智能电表抄表记录;
 *
 * @author : JED
 * @date : 2025-4-16
 */
public class SmartMeterRecordEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 电表编码
     */
    public String meter_no;
    /**
     * 抄表时间戳（毫秒）
     */
    public long record_time;
    /**
     * 总有功电能（kWh）
     */
    public BigDecimal total_active_energy;
    /**
     * 期间用电量，当前值 - 上一值
     */
    public BigDecimal consumption_interval;
    /**
     * 上次抄表时间戳
     */
    public long last_record_time;
    /**
     * 备注信息
     */
    public String remark;
    /**
     * 额外数据JSON格式，用于记录一些可选参数
     */
    public String extra_data;
    /**
     * 创建时间戳（毫秒）
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static SmartMeterRecordEntity getInstance() {
        return new SmartMeterRecordEntity();
    }
}
