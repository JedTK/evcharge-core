package com.evcharge.entity.notify;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;

/**
 * 通知日志;
 *
 * @author : JED
 * @date : 2024-9-26
 */
@TargetDB("evcharge_notify")
public class NotifyLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 消息ID
     */
    public String message_id;
    /**
     * 唯一编码：{前缀}.{站点编码/设备序列号/其他}
     */
    public String unique_code;
    /**
     * 通知配置:{前缀}.{通知编码/站点编码/设备序列号}
     */
    public String config_code;
    /**
     * 通知方式
     */
    public String method_code;
    /**
     * 通知模板
     */
    public String template_code;
    /**
     * 状态：0-待发送，1-失败，2-成功，3-重试
     */
    public int status;
    /**
     * 失败次数
     */
    public int fail_count;
    /**
     * 重试次数
     */
    public int retry_count;
    /**
     * 重试最大次数
     */
    public int retry_max_count;
    /**
     * 重试间隔，毫秒级
     */
    public int retry_timeout_ms;
    /**
     * 节流超时时间,毫秒
     */
    public int throttling_timeout_ms;
    /**
     * 数据内容
     */
    public String data;
    /**
     * 接受者,[all_admin]/[auto]/手机号码1,手机号码2
     */
    public String accept_list;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyLogsEntity getInstance() {
        return new NotifyLogsEntity();
    }

    public NotifyLogsEntity add() {
        this.message_id = common.getUUID();
        this.retry_count = 0;
        this.create_time = TimeUtil.getTimestamp();
        this.update_time = TimeUtil.getTimestamp();
        this.id = this.insertGetId();
        return this;
    }
}
