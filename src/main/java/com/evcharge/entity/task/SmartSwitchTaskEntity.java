package com.evcharge.entity.task;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 智能开关任务;
 *
 * @author : JED
 * @date : 2024-11-29
 */
public class SmartSwitchTaskEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 序列号
     */
    public String serialNumber;
    /**
     * 打开cron表达式
     */
    public String cron_on;
    /**
     * 关闭cron表达式
     */
    public String cron_off;
    /**
     * 开关索引
     */
    public int switch_index;
    /**
     * 状态：0-关闭，1-开启
     */
    public int status;
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
    public static SmartSwitchTaskEntity getInstance() {
        return new SmartSwitchTaskEntity();
    }
}