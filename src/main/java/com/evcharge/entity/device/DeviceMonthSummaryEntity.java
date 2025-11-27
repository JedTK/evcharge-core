package com.evcharge.entity.device;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备月数据统计;
 *
 * @author : JED
 * @date : 2025-6-30
 */
public class DeviceMonthSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 设备编码
     */
    public String device_code;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 消费金额
     */
    public BigDecimal consume_amount;
    /**
     * 计次消费金额
     */
    public BigDecimal per_consume_amount;
    /**
     * 计次消费调整金额
     */
    public BigDecimal per_adj_consume_amount;
    /**
     * 充电卡消费金额
     */
    public BigDecimal card_consume_amount;
    /**
     * 充电卡消费调整金额
     */
    public BigDecimal card_adj_consume_amount;
    /**
     * 平均日消费金额
     */
    public BigDecimal avg_day_consume_amount;
    /**
     * 运行天数
     */
    public int run_days;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static DeviceMonthSummaryEntity getInstance() {
        return new DeviceMonthSummaryEntity();
    }
}
