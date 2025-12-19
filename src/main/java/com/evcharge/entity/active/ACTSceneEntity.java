package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 场景映射表：一个活动绑定多个场景 - 实体类 ;
 *
 * @date : 2025-12-19
 */
public class ACTSceneEntity extends BaseEntity implements Serializable {
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
     * 触发场景：CHARGE_FINISH/RECHARGE_CALLBACK/HOME_ENTER等,;
     */
    public String scene_code;
    /**
     * 状态：0=停用，1=启用,;
     */
    public byte status;
    /**
     * 创建时间戳,;
     */
    public long create_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static ACTSceneEntity getInstance() {
        return new ACTSceneEntity();
    }
}
