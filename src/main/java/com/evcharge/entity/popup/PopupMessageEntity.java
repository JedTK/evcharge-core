package com.evcharge.entity.popup;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 弹窗消息 - 实体类 ;待弹消息表,业务主动写入
 *
 * @date : 2025-12-12
 */
@TargetDB("evcharge_notify")
public class PopupMessageEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 消息id，唯一标识,;
     */
    public String message_id;
    /**
     * 弹窗编码，关联 PopupConfig.popup_code,;
     */
    public String popup_code;
    /**
     * 目标用户ID，0表示全局/按人群规则匹配,;
     */
    public long uid;
    /**
     * 触发场景编码，如 app_launch/home_enter/order_finish,;
     */
    public String scene_code;
    /**
     * 客户端列表，逗号分隔，如: MINI_PROGRAM,IOS,;
     */
    public String client_code;
    /**
     * 状态：任务状态：0=待处理，1=已下发(已返回给客户端)，2=已消费完成，3=已取消，4=已过期,;
     */
    public byte status;
    /**
     * 业务主键，如中奖记录ID、订单号，用于幂等/排重,;
     */
    public String biz_key;
    /**
     * 业务变量JSON，用于渲染模板中的 ${var},;
     */
    public String vars_json;
    /**
     * 任务过期时间，0表示按 PopupConfig.end_time 或长期有效,;
     */
    public long expire_time;
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
    public static PopupMessageEntity getInstance() {
        return new PopupMessageEntity();
    }

}
