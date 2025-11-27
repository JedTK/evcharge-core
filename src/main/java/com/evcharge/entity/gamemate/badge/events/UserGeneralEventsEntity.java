package com.evcharge.entity.gamemate.badge.events;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 用户消费操作事件表;存储所有可能触发徽章或成就的用户行为记录
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class UserGeneralEventsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键ID,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 事件类型 (如: 'POST_CREATED', 'COMMENT_ADDED', 'DAILY_LOGIN', 'PRODUCT_PURCHASED'),;
     */
    public String event_type;
    /**
     * 发生事件的实体类型 (如: 'Post', 'Order', 'User'),;
     */
    public String entity_type;
    /**
     * 发生事件的实体ID (如: 帖子ID, 订单ID),;
     */
    public long entity_id;
    /**
     * 事件的附加数据 (用于复杂的规则判断，如：{"category_id": 5, "word_count": 500}),;
     */
    public String metadata;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static UserGeneralEventsEntity getInstance() {
        return new UserGeneralEventsEntity();
    }
}