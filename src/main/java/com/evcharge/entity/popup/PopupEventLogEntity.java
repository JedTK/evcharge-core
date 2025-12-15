package com.evcharge.entity.popup;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 弹窗日志 - 实体类 ;
 *
 * @date : 2025-12-12
 */
public class PopupEventLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 事件唯一ID（可用雪花/UUID），方便幂等写入,;
     */
    public String event_id;
    /**
     * 弹窗编码,;
     */
    public String popup_code;
    /**
     * 消息ID（来自 PopupMessage.message_id，可为空）,;
     */
    public String message_id;
    /**
     * 业务幂等主键（中奖记录ID/订单号等）,;
     */
    public String biz_key;
    /**
     * 用户ID，0表示全局类事件（一般不建议0，尽量都带uid）,;
     */
    public long uid;
    /**
     * 场景编码,;
     */
    public String scene_code;
    /**
     * 客户端（建议单值，不建议这里存逗号列表）,;
     */
    public String client_code;
    /**
     * 事件类型：DELIVER/IMPRESSION/CLICK_CONFIRM/CLOSE/CONSUMED/EXPIRE/CANCEL,;
     */
    public String event_type;
    /**
     * 事件日期(yyyyMMdd)，用于索引/分区,;
     */
    public String event_day;
    /**
     * 结果码（0=成功，其他=失败/拒绝/异常）,;
     */
    public int result_code;
    /**
     * 结果说明,;
     */
    public String result_msg;
    /**
     * 扩展JSON（渠道、灰度、版本号、城市、AB实验、队列信息等）,;
     */
    public String ext_json;
    /**
     * 创建时间戳,;
     */
    public long create_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static PopupEventLogEntity getInstance() {
        return new PopupEventLogEntity();
    }

}
