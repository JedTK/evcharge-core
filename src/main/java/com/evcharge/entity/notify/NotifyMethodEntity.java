package com.evcharge.entity.notify;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 通知方式：企业微信机器人、短信通知、电话通知等等;
 *
 * @author : JED
 * @date : 2024-9-25
 */
@TargetDB("evcharge_notify")
public class NotifyMethodEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 通知方式代码
     */
    public String method_code;
    /**
     * 通知方式名称
     */
    public String method_name;
    /**
     * 描述
     */
    public String description;

    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyMethodEntity getInstance() {
        return new NotifyMethodEntity();
    }
}
