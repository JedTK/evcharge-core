package com.evcharge.entity.gamemate.badge.events;


import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户通用操作事件表;存储所有可能触发徽章或成就的用户行为记录
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class UserConsumeEventsEntity extends BaseEntity implements Serializable {
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
     * 订单编号,;
     */
    public String order_sn;
    /**
     * 消费类型 其实是产品类型,;
     */
    public String consume_type;
    /**
     * 消费金额,;
     */
    public BigDecimal consume_fee;
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
    public static UserConsumeEventsEntity getInstance() {
        return new UserConsumeEventsEntity();
    }


}