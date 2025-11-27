package com.evcharge.entity.station;

import com.xyzs.entity.BaseEntity;
import org.apache.poi.hpsf.Decimal;

import java.io.Serializable;
import java.math.BigDecimal;

public class ChargeOrderExDataEntity extends BaseEntity implements Serializable {


    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 充电订单号
     */
    public String orderSN;

    /**
     * 用户会员等级 无会员无等级为0
     */
    public long user_member_level_id;

    /**
     * 安全充电保险，0=不启用，1=启用
     */
    public int is_safe_charge;

    /**
     * 安全充电保险费用
     */
    public BigDecimal safe_charge_fee;

    /**
     * 创建时间
     */
    public long create_time;

    /**
     * 更新时间
     */
    public long update_time;


    public static ChargeOrderExDataEntity getInstance() {
        return new ChargeOrderExDataEntity();
    }

}
