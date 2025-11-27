package com.evcharge.entity.order;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电订单v3版本额外数据;
 *
 * @author : JED
 * @date : 2024-3-12
 */
public class ChargeOrderV3ExDataEntity extends BaseEntity implements Serializable {
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
     * 心跳包：累计充电时长，单位：秒
     */
    public int totalChargeTime;
    /**
     * 心跳包：耗电量
     */
    public double powerConsumption;
    /**
     * 心跳包：当前电压
     */
    public double voltage;
    /**
     * 心跳包：当前电流
     */
    public double current;
    /**
     * 心跳包：当前环境温度
     */
    public double temperature;
    /**
     * 心跳包：当前端口温度
     */
    public double portTemperature;
    /**
     * 心跳包：当前功率
     */
    public double currentPower;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeOrderV3ExDataEntity getInstance() {
        return new ChargeOrderV3ExDataEntity();
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
