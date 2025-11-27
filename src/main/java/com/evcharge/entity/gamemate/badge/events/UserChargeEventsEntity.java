package com.evcharge.entity.gamemate.badge.events;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充电记录表;
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class UserChargeEventsEntity extends BaseEntity implements Serializable {
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
     * 站点id,;
     */
    public String station_id;
    /**
     * 支付方式 余额支付-Balance 充电卡-chargeCard 积分-integral,;
     */
    public String payment_type;
    /**
     * 开始时间,;
     */
    public long start_time;
    /**
     * 结束时间,;
     */
    public long end_time;
    /**
     * 充电时长,;
     */
    public long charge_time;
    /**
     * 充电费用,;
     */
    public BigDecimal total_fee;
    /**
     * 是否在法定假日完成充电,;
     */
    public int is_holiday;
    /**
     * 状态,;
     */
    public int status;
    /**
     * 仅存储非查询依赖的附加信息 (如：优惠券ID、桩体型号等，不用于徽章统计),;
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
    public static UserChargeEventsEntity getInstance() {
        return new UserChargeEventsEntity();
    }
}