package com.evcharge.entity.station;


import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.task.ParkingMonitorApp;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;

/**
 * 占用费收费订单;
 *
 * @author : JED
 * @date : 2022-10-19
 */
public class ParkingOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 设备码
     */
    public String deviceCode;
    /**
     * 端口
     */
    public int port;
    /**
     * 状态：0=未结算，1=已结算
     */
    public int payment_status;
    /**
     * 占用费单价，元/小时
     */
    public double price;
    /**
     * 总收费
     */
    public double totalAmount;
    /**
     * 充电停止N秒后开始收费，单位：秒
     */
    public long delayTime;
    /**
     * 开始占用费时间
     */
    public long start_time;
    /**
     * 最后占用费时间戳
     */
    public long end_time;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 充电停止时间
     */
    public long chargeStopTime;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ParkingOrderEntity getInstance() {
        return new ParkingOrderEntity();
    }

    /**
     * 检查用户是否拔掉了插座
     *
     * @param lastParkingOrder
     * @return
     */
    public SyncResult checkDeviceSocketStatus(ParkingOrderEntity lastParkingOrder) {
        //查询到充电订单
        ChargeOrderEntity chargeOrderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", lastParkingOrder.OrderSN)
                .findEntity();
        if (chargeOrderEntity == null || chargeOrderEntity.id == 0) return new SyncResult(2, "查询不到充电订单信息");

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(chargeOrderEntity.deviceCode);
        if (deviceEntity == null) return new SyncResult(2, "未知设备");

        //region 读取插座信息
        DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                .where("deviceId", deviceEntity.id)
                .where("port", chargeOrderEntity.port)
                .findEntity();
        if (deviceSocketEntity == null || deviceSocketEntity.id == 0) return new SyncResult(4, "查询不到设备插座数据");
        //endregion

        //状态：0=空闲，1=充电中，2=未启动充电，3=已充满电（用户还没有拔掉充电器），4=故障，5=浮充
        switch (deviceSocketEntity.status) {
            case 3://已充满电
                //情景1：用户充电完成，但是还没拔掉充电头
                break;
            case 0://空闲：
                //情景1：用户拔掉走人
            case 1://充电中
                //情景1：用户已经拔掉走人，新的用户使用了此端口
            case 5: //浮充
                //情景1：用户已经拔掉走人了，新的用户使用了此端口，并且已经快充电完成
            case 2://未启动充电
                //情景1：用户拔掉走人了，新的用户正准备充电
                ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头 插座状态：" + deviceSocketEntity.status);
                return new SyncResult(1, "用户已经拔掉充电头");
            default://其他状态：
                ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头 插座状态：" + deviceSocketEntity.status);
                return new SyncResult(14, "插座发生故障，不产生占用费");
        }
        //region 根据设备码和端口查询此设备最后充电的一笔订单
        ChargeOrderEntity newChargeOrderEntity = ChargeOrderEntity.getInstance()
                .where("deviceCode", chargeOrderEntity.deviceCode)
                .where("port", chargeOrderEntity.port)
                .where("create_time", ">=", TimeUtil.getTime00())
                .where("create_time", "<=", TimeUtil.getTime24())
                .order("id DESC")
                .findEntity();
        if (chargeOrderEntity != null && chargeOrderEntity.id > 0) {
            //判断订单号是否相同,如果不同则表示有新的订单存在
            if (!chargeOrderEntity.OrderSN.equals(OrderSN)) return new SyncResult(1, "用户已经拔掉充电头");
        }
        //endregion

        return new SyncResult(0, "用户还没拔掉充电头");
    }
}
