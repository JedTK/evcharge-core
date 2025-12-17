package com.evcharge.entity.popup;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 用户-弹窗维度的状态快照 - 实体类 ;
 *
 * @date : 2025-12-12
 */
@TargetDB("evcharge_notify")
public class PopupUserStateEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 弹窗编码，唯一标识，如 ACT_2025_XMAS,;
     */
    public String popup_code;
    /**
     * 是否已曝光(IMPRESSION),;
     */
    public byte seen;
    /**
     * 是否已点击主按钮,;
     */
    public byte clicked;
    /**
     * 是否已关闭,;
     */
    public byte closed;
    /**
     * 活动期累计展示次数（可选：按IMPRESSION累计）,;
     */
    public int total_count;
    /**
     * 最后一次事件时间,;
     */
    public long last_event_time;
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
    public static PopupUserStateEntity getInstance() {
        return new PopupUserStateEntity();
    }
}
