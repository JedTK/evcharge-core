package com.evcharge.entity.order;

import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.math.BigDecimal;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电订单V3-电量计费记录;
 *
 * @author : JED
 * @date : 2024-4-7
 */
public class ChargeOrderV3ElectricityFeeEntity extends BaseEntity implements Serializable {
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
     * 计费时段：开始时间戳，单位：毫秒
     */
    public long startTime;
    /**
     * 计费时段：结束时间戳，单位：毫秒
     */
    public long endTime;
    /**
     * 耗电量，心跳包更新
     */
    public double powerConsumption;
    /**
     * 总耗电量，度
     */
    public BigDecimal totalElectricity;
    /**
     * 心跳电量，度
     */
    public BigDecimal electricity;
    /**
     * 总费用金额
     */
    public BigDecimal totalAmount;
    /**
     * 心跳费用金额
     */
    public BigDecimal tickAmount;
    /**
     * 总电费，元
     */
    public BigDecimal totalElectricityFeeAmount;
    /**
     * 心跳电费，元
     */
    public BigDecimal electricityFeeAmount;
    /**
     * 总服务费，元
     */
    public BigDecimal totalServiceFeeAmount;
    /**
     * 心跳服务费，元
     */
    public BigDecimal serviceFeeAmount;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeOrderV3ElectricityFeeEntity getInstance() {
        return new ChargeOrderV3ElectricityFeeEntity();
    }

    /**
     * 充电心跳包计费
     *
     * @param orderV3Entity 充电订单
     * @return 是否新增记录成功
     */
    public boolean heartbeatBilling(ChargeOrderV3Entity orderV3Entity) {
        this.OrderSN = orderV3Entity.OrderSN;
        this.uid = orderV3Entity.uid;
        this.create_time = TimeUtil.getTimestamp();

        if (this.startTime == 0 || this.endTime == 0) {
            Map<String, Object> lastData = ChargeOrderV3ElectricityFeeEntity.getInstance()
                    .field("id,endTime")
                    .where("OrderSN", this.OrderSN)
                    .order("id DESC")
                    .find();
            if (lastData == null || lastData.isEmpty()) {
                //表示首次充电心跳包
                this.startTime = orderV3Entity.startTime;
            } else {
                this.startTime = MapUtil.getLong(lastData, "endTime");
            }
            this.endTime = TimeUtil.getTimestamp();
        }
        return this.insert() > 0;
    }
}
