package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 通知配置;
 *
 * @author : JED
 * @date : 2023-11-20
 */
public class SysNotificationConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 唯一标识
     */
    public String uniqueId;
    /**
     * 离线通知：0-关闭，1-开启
     */
    public int offline;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysNotificationConfigEntity getInstance() {
        return new SysNotificationConfigEntity();
    }

    /**
     * 获取设备或充电桩的通知配置
     *
     * @param uniqueId 唯一设备码
     * @param add      不存在是否新增？
     * @return
     */
    public SysNotificationConfigEntity getWithUniqueId(String uniqueId, boolean add) {
        SysNotificationConfigEntity configEntity = this
                .cache(String.format("BaseData:NotificationConfig:%s", uniqueId))
                .where("uniqueId", uniqueId)
                .findEntity();
        if (configEntity == null || configEntity.id == 0) {
            if (!add) return null;
            configEntity = new SysNotificationConfigEntity();
            configEntity.uniqueId = uniqueId;
            configEntity.offline = 1;//离线通知：0-关闭，1-开启
//            configEntity.id = configEntity.insertGetId();
        }
        return configEntity;
    }
}
