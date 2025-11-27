package com.evcharge.entity.order;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 充电订单v3版本额外费用;
 *
 * @author : JED
 * @date : 2024-3-14
 */
public class ChargeOrderV3ExtraFeeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电订单号
     */
    public String OrderSN;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 积分：预计扣费
     */
    public int esIntegral;
    /**
     * 积分：积分扣费
     */
    public int integral;
    /**
     * 积分：消耗的金额（用于统计计费）
     */
    public BigDecimal integralConsumeAmount;
    /**
     * 充电卡：卡号
     */
    public String cardNumber;
    /**
     * 充电卡：消耗时间,单位毫秒
     */
    public long chargeCardConsumeTime;
    /**
     * 充电卡：消耗时间的倍率
     */
    public BigDecimal chargeCardConsumeTimeRate;
    /**
     * 充电卡：消耗的金额（并不是用于实际扣费，只是用于统计或分账计算）
     */
    public BigDecimal chargeCardConsumeAmount;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeOrderV3ExtraFeeEntity getInstance() {
        return new ChargeOrderV3ExtraFeeEntity();
    }

    /**
     * 更新数据，不存在数据则新增
     *
     * @param orderSN  订单号
     * @param set_data 需要更新的数据
     * @return 操作成功
     */
    public boolean updateData(String orderSN, Map<String, Object> set_data) {
        if (this.where("OrderSN", orderSN).exist()) {
            return this.where("OrderSN", orderSN)
                    .update(set_data) > 0;
        }
        set_data.put("OrderSN", orderSN);
        return this.insert(set_data) > 0;
    }
}
