package com.evcharge.entity.active;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 活动配置表 - 实体类 ;
 *
 * @date : 2025-12-19
 */
public class ACTConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 活动编码(唯一),;
     */
    public String activity_code;
    /**
     * 活动标题,;
     */
    public String title;
    /**
     * 活动描述,;
     */
    public String description;
    /**
     * 策略编码(决定执行哪个策略类),;
     */
    public String strategy_code;
    /**
     * 策略参数(JSON)，如抽奖概率/奖励配置/弹窗变量等,;
     */
    public String params_json;
    /**
     * 场景匹配模式：1=按ActivityScene表，2=全局(谨慎),;
     */
    public byte scene_mode;
    /**
     * 优先级(越小越先执行),;
     */
    public int priority;
    /**
     * 状态：0=停用，1=启用,;
     */
    public byte status;
    /**
     * 开始时间(毫秒时间戳),;
     */
    public long start_time;
    /**
     * 结束时间(毫秒时间戳),;
     */
    public long end_time;
    /**
     * 平台代码,;
     */
    public String platform_code;
    /**
     * 组织代码,;
     */
    public String organize_code;
    /**
     * 备注,;
     */
    public String remark;
    /**
     * 创建时间戳,;
     */
    public long create_time;
    /**
     * 更新时间戳,;
     */
    public long update_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static ACTConfigEntity getInstance() {
        return new ACTConfigEntity();
    }
}
