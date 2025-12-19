package com.evcharge.entity.popup;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 频率策略 - 实体类 ;表述一个弹窗弹出的频率策略
 *
 * @date : 2025-12-12
 */
@TargetDB("evcharge_notify")
public class PopupFrequencyStrategyEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * ;
     */
    public long id;
    /**
     * 频率策略编码，供 PopupConfig.frequency_strategy_code 引用,;
     */
    public String strategy_code;
    /**
     * 频率策略名称，如 WEAK_NOTICE、STRONG_FORCE,;
     */
    public String strategy_name;
    /**
     * 单用户每日最多弹出次数，0表示不限制,;
     */
    public int user_daily_max;
    /**
     * 单用户在活动期内最多弹出次数，0表示不限制,;
     */
    public int user_total_max;
    /**
     * 是否用户看过一次(曝光一次)后不再弹：0=否，1=是,;
     */
    public int user_seen_no_more;
    /**
     * 是否用户点击主按钮后不再弹：0=否，1=是,;
     */
    public int user_clicked_no_more;
    /**
     * 是否用户关闭一次后不再弹：0=否，1=是,;
     */
    public int user_closed_no_more;
    /**
     * 全局每日最多展示次数，0表示不限制,;
     */
    public int global_daily_max;
    /**
     * 是否在指定场景必弹一次：0=否，1=是,;
     */
    public int force_in_scene;
    /**
     * 扩展JSON，例如按渠道、城市维度做细化频控等,;
     */
    public String ext_config;
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
    public static PopupFrequencyStrategyEntity getInstance() {
        return new PopupFrequencyStrategyEntity();
    }

}
