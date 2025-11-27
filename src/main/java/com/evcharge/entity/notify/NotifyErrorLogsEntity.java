package com.evcharge.entity.notify;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 通知错误日志 - 实体类 ;
 *
 * @date : 2025-9-11
 */
public class NotifyErrorLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id,;
     */
    public long id;
    /**
     * 消息ID,;
     */
    public String message_id;
    /**
     * 错误日志内容,;
     */
    public String content;
    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyErrorLogsEntity getInstance() {
        return new NotifyErrorLogsEntity();
    }

}
