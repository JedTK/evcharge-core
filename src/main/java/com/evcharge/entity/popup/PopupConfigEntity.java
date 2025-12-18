package com.evcharge.entity.popup;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 弹出窗口配置 - 实体类 ;弹窗核心配置，调用者根据popup_code表述：要不要弹？弹什么？弹几次？对谁弹
 *
 * @date : 2025-12-12
 */
@TargetDB("evcharge_notify")
public class PopupConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * ID;
     */
    public long id;
    /**
     * 弹窗编码，唯一标识，如 ACT_2025_XMAS,;
     */
    public String popup_code;
    /**
     * 弹窗名称，运营可读名称,;
     */
    public String popup_name;
    /**
     * 弹窗类型：ACTIVITY/NOTICE/PRIZE/VERSION/AGREEMENT 等,;
     */
    public String popup_type;
    /**
     * 状态：0=停用，1=启用,;
     */
    public int status;
    /**
     * 关联内容模板 PopupTemplate 的编码,;
     */
    public String template_code;
    /**
     * 触发场景编码，如 app_launch/home_enter/order_finish,;
     */
    public String scene_code;
    /**
     * 客户端列表，逗号分隔，如: MINI_PROGRAM,IOS,;
     */
    public String client_code;
    /**
     * 生效开始时间，毫秒时间戳,;
     */
    public long start_time;
    /**
     * 生效结束时间，毫秒时间戳，0表示永久,;
     */
    public long end_time;
    /**
     * 人群类型：0=全体，1=特定用户,;
     */
    public int target_type;
    /**
     * 弹窗优先级，1-100，数字越小优先级越高,;
     */
    public int priority;
    /**
     * 频率策略编码，关联 PopupFrequencyStrategy.strategy_code,;
     */
    public String frequency_strategy_code;
    /**
     * 是否允许与其他弹窗串行展示：0=只弹本条，1=可与其他一起排队,;
     */
    public int allow_multi_chain;
    /**
     * 是否强制弹窗：0=否，1=是（不处理不能继续下一步）,;
     */
    public int is_force;
    /**
     * 扩展配置JSON，预留字段，如业务定向条件、灰度规则等,;
     */
    public String ext_config;
    /**
     * 组织代码,;
     */
    public String organize_code;
    /**
     * 平台代码,;
     */
    public String platform_code;
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
    public static PopupConfigEntity getInstance() {
        return new PopupConfigEntity();
    }
}
