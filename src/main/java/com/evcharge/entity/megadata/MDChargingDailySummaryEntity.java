package com.evcharge.entity.megadata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电每日数据汇总，由平台配置，主动请求数据来更新;
 *
 * @author : JED
 * @date : 2023-8-21
 */
public class MDChargingDailySummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 组织代码，所属组织
     */
    public String organize_code;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 累计充电时长
     */
    public long chargingTotalTime;
    /**
     * 总充电次数
     */
    public int chargingTotalCount;
    /**
     * 累计耗电量（度）
     */
    public double totalPowerConsumption;
    /**
     * 新增的用户数
     */
    public int newUserCount;
    /**
     * 新增的车辆数
     */
    public int newVehicleCount;
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
    public static MDChargingDailySummaryEntity getInstance() {
        return new MDChargingDailySummaryEntity();
    }
}