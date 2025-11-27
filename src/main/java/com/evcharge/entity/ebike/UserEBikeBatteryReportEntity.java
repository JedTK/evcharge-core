package com.evcharge.entity.ebike;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 电池健康AI助手报告;
 *
 * @author : JED
 * @date : 2025-2-19
 */
public class UserEBikeBatteryReportEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 输入的内容
     */
    public String input_text;
    /**
     * 报告内容
     */
    public String content;
    /**
     * 车辆关联ID
     */
    public long e_bike_id;
    /**
     * 最后充电时间，用于查询充电记录
     */
    public long last_charging_date;
    /**
     * 状态：0-删除，1-生成中，2-生成完毕
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static UserEBikeBatteryReportEntity getInstance() {
        return new UserEBikeBatteryReportEntity();
    }
}
