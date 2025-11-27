package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电订单心跳包;
 *
 * @author : JED
 * @date : 2022-11-2
 */
public class ChargeOrderHeartbeatEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单ID
     */
    public long order_id;
    /**
     * 订单编号（以后作为唯一关联号）
     */
    public String OrderSN;
    /**
     * 状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
     */
    public int status;
    /**
     * 状态说明
     */
    public String status_msg;
    /**
     * 累积电量
     */
    public double chargeAmount;
    /**
     * 充电时长
     */
    public long chargeTime;
    /**
     * 心跳包期间平均功率
     */
    public double flatPower;
    /**
     * 心跳包期间最大功率
     */
    public double maxPower;
    /**
     * 心跳包期间最小功率
     */
    public double minPower;
    /**
     * 实时功率
     */
    public double thisPower;
    /**
     * 充电至今峰值功率
     */
    public double allMaxPower;
    /**
     * 电压
     */
    public double voltage;
    /**
     * 电流
     */
    public double current;
    /**
     * 环境温度
     */
    public double temperature;
    /**
     * 端口温度
     */
    public double portTemperature;
    /**
     * 时间戳
     */
    public long timeStamp;
    /**
     * 占位时长（单位为分，仅充电柜设备才有）
     */
    public long occupyTime;
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
    public static ChargeOrderHeartbeatEntity getInstance() {
        return new ChargeOrderHeartbeatEntity();
    }
}
