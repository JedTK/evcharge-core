package com.evcharge.entity.megadata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电日志，由第三方数据埋点主动上传数据;
 *
 * @author : JED
 * @date : 2023-8-21
 */
public class MDChargingLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 分配的充电桩唯一编码
     */
    public String cs_code;
    /**
     * 状态,1=启动充电，2=完成充电，3=充电错误
     */
    public int status;
    /**
     * 开始充电时间戳
     */
    public long startTime;
    /**
     * 预计结束充电时间戳
     */
    public long endTime;
    /**
     * 实际停止充电时间戳
     */
    public long stopTime;
    /**
     * 充电时长,单位：秒
     */
    public int chargeTime;
    /**
     * 充电期间最大功率
     */
    public double maxPower;
    /**
     * 充电期间消耗的电量
     */
    public double powerConsumption;
    /**
     * 自定义订单号
     */
    public String custom_OrderSN;
    /**
     * 自定义用户唯一id
     */
    public String custom_uid;
    /**
     * 自定义设备编码
     */
    public String custom_device_code;
    /**
     * 自定义设备端口
     */
    public String custom_device_port;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static MDChargingLogsEntity getInstance() {
        return new MDChargingLogsEntity();
    }
}