package com.evcharge.entity.LogTrack;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 消防事件;
 *
 * @author : JED
 * @date : 2024-12-3
 */
public class FireSafetyEventTrackEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 事件id
     */
    public String event_id;
    /**
     * 事件类型
     */
    public String event_type;
    /**
     * 事件标题
     */
    public String event_title;
    /**
     * 事件等级：1-信息, 2-低, 3-中, 4-高, 5-严重
     */
    public int event_level;
    /**
     * 事件数据，JSON结构
     */
    public String event_data;
    /**
     * 状态：0-忽略，1-进行中，2-结束
     */
    public int status ;
    /**
     * 设备编码/序列号
     */
    public String device_code;
    /**
     * 设备信息，JSON结构
     */
    public String device_info;
    /**
     * 站点唯一编码
     */
    public String cs_id;
    /**
     * 站点信息，JSON结构
     */
    public String cs_info;
    /**
     * 主事件id
     */
    public String main_event_id;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static FireSafetyEventTrackEntity getInstance() {
        return new FireSafetyEventTrackEntity();
    }
}
