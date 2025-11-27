package com.evcharge.task;

import com.evcharge.entity.basedata.ParkingConfigEntity;
import com.evcharge.entity.basedata.ParkingItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ParkingOrderEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 占用费监控工作
 * 参考：
 * 充电自停后一小时后开始收取占用费
 * 晚间占用费0元，日间占用费0.25元/小时
 */
public class ParkingMonitorJob implements Job {
    private final static int mMaxErrorCount = 20;
    private String mGroup;
    private String mName;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        mGroup = jobExecutionContext.getJobDetail().getKey().getGroup();
        mName = jobExecutionContext.getJobDetail().getKey().getName();

        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        String OrderSN = MapUtil.getString(data, "OrderSN");

        LogsUtil.info(ParkingMonitorApp.mMonitorText, "\033[1;91m 执行中... OrderSN=%s 下次执行：%s\033[0m", OrderSN, TimeUtil.toTimeString(jobExecutionContext.getNextFireTime()));

        if (!StringUtils.hasLength(OrderSN)) {
            ChargeOrderTaskJob.getInstance().remove(mName, String.format("JobData中订单号为空 [%s.%s]", mGroup, mName));
            return;
        }

        SyncResult r = run(OrderSN);
        if (r.code != 0) {
            int errorCount = DataService.getMainCache().getInt(String.format("Task:%s:ErrorCount:%s:%s", common.getLocalIPv4(), mGroup, mName), 0);
            if (errorCount > mMaxErrorCount) {
                ParkingMonitorApp.remove(OrderSN, "任务执行期间发生错误次数过多");
            } else {
                errorCount++;
                DataService.getMainCache().set(String.format("Task:%s:ErrorCount:%s:%s", common.getLocalIPv4(), mGroup, mName), errorCount, 86400000);
            }
            return;
        }
    }

    /**
     * 运行
     * <p>
     * 1、检查订单是否已经结束充电
     * 2、检查设备端口状态是否为 3=已充满电
     * 3、检查是否开始收费
     * 4、检查是否有占用费收费订单，不存在则新增，存在则更新占用费收费订单的数据
     *
     * @param OrderSN 订单号
     * @return
     */
    private SyncResult run(String OrderSN) {
        try {
            //region 检查订单是否已经结束充电
            ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findEntity();
            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到充电订单");
            //状态,-1=错误，0=待启动，1=充电中，2=已完成
            if (orderEntity.status != 2) return new SyncResult(11, "充电订单必须完成才能开始任务");
            //endregion

            //region 读取设备信息
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(12, "查询不到设备信息");
            if (deviceEntity.parkingConfigId == 0) {
                ParkingMonitorApp.remove(OrderSN, "此设备没有配置占用费收费标准");
                return new SyncResult(13, "此设备没有配置占用费收费标准");
            }
            //endregion

            //region 读取插座信息
            DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                    .where("deviceId", deviceEntity.id)
                    .where("port", orderEntity.port)
                    .findEntity();
            if (deviceSocketEntity == null || deviceSocketEntity.id == 0) {
                return new SyncResult(13, "查询不到设备插座数据");
            }
            //endregion

            //region 读取占用费收费配置以及检查占用费收费配置
            ParkingConfigEntity parkingConfigEntity = ParkingConfigEntity.getInstance()
                    .cache(String.format("ParkingConfig:%s:Details", deviceEntity.parkingConfigId))
                    .findEntity(deviceEntity.parkingConfigId);
            if (parkingConfigEntity == null || parkingConfigEntity.id == 0) {
                return new SyncResult(14, "查询不到占用费收费标准配置，请检查配置是否存在");
            }
            //endregion

            long nowTime = TimeUtil.getTimestamp();
            ParkingItemEntity parkingItemEntity = null;

            //检查一下是否生成了占用费收费订单，如果没有生成，则检查插座的状态进行处理
            ParkingOrderEntity parkingOrderEntity = ParkingOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findEntity();
            //没有产生占用费收费订单
            if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
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
                        return new SyncResult(14, "插座发生故障，不产生占用费收费");
                }
            }

            //region 检查是否开始收费

            //计算开始占用费收费时间戳
            long startTime = orderEntity.stopTime + parkingConfigEntity.delayTime * 1000;
            if (startTime > nowTime) return new SyncResult(0, "还没进入收费时段");

            //region 读取占用费收费设置
            List<ParkingItemEntity> parkingItemEntityList = ParkingItemEntity.getInstance()
                    .cache(String.format("ParkingConfig:%s:Items", deviceEntity.parkingConfigId))
                    .where("configId", deviceEntity.parkingConfigId)
                    .page(1, 10)
                    .order("id")
                    .selectList();
            Iterator it = parkingItemEntityList.iterator();
            while (it.hasNext()) {
                ParkingItemEntity nd = (ParkingItemEntity) it.next();
                if (TimeUtil.isBelongPeriodTime(nowTime, nd.startDate, nd.endDate)) {
                    parkingItemEntity = nd;
                    break;
                }
            }

            if (parkingItemEntity == null) return new SyncResult(0, "该时段不收费");
            if (parkingItemEntity.price == 0) return new SyncResult(0, "该时段不收费");

            //endregion

            //endregion

            boolean isNew = false;//用来表示是否存在了新的订单
            //region 根据设备码和端口查询此设备最后充电的一笔订单
            ChargeOrderEntity chargeOrderEntity = ChargeOrderEntity.getInstance()
                    .where("deviceCode", orderEntity.deviceCode)
                    .where("port", orderEntity.port)
                    .where("create_time", ">=", TimeUtil.getTime00(-1))
                    .where("create_time", "<=", TimeUtil.getTime24())
                    .order("id DESC")
                    .findEntity();
            if (chargeOrderEntity != null && chargeOrderEntity.id > 0) {
                //判断订单号是否相同,如果不同则表示有新的订单存在
                if (!chargeOrderEntity.OrderSN.equals(OrderSN)) isNew = true;
            }
            //endregion

            //计算收费
            double totalAmount = parkingConfigEntity.getFee(parkingConfigEntity.id, startTime, nowTime);

            //如果是新的订单，并且还没产生占用费收费订单，则移除监控
            if (isNew) {
                //如果是新的订单，并且还没产生占用费收费，则移除占用费监控
                if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
                    ParkingMonitorApp.remove(OrderSN, "已有新的订单正在充电，上一个用户已经移走");
                    return new SyncResult(15, "已有新的订单正在充电，上一个用户已经移走");
                } else {
                    //如果是新的订单，但是已经有了占用费收费订单，则更新数据，等待用户支付，并且移除占用费监控
                    Map<String, Object> set_data = new LinkedHashMap<>();
                    set_data.put("totalAmount", totalAmount);
                    set_data.put("end_time", nowTime);
                    parkingOrderEntity.where("OrderSN", OrderSN).update(set_data);

                    ParkingMonitorApp.remove(OrderSN, "已有新的订单正在充电，上一个用户已经移走");
                    return new SyncResult(15, "已有新的订单正在充电，上一个用户已经移走");
                }
            }

            //状态：0=空闲，1=充电中，2=未启动充电，3=已充满电（用户还没有拔掉充电器），4=故障，5=浮充
            //不存在订单则新增占用费订单：这里也不需要判断状态是否为3，因为不存在订单状态为其他的，上面已经结束监控了
            if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
                //情景1：用户充电完成，但是还没拔掉充电头
                parkingOrderEntity = new ParkingOrderEntity();
                parkingOrderEntity.OrderSN = OrderSN;
                parkingOrderEntity.deviceCode = orderEntity.deviceCode;
                parkingOrderEntity.port = orderEntity.port;
                parkingOrderEntity.uid = orderEntity.uid;
                parkingOrderEntity.payment_status = 0;//状态：0=未结算，1=已结算
                parkingOrderEntity.delayTime = parkingConfigEntity.delayTime;
                parkingOrderEntity.price = parkingItemEntity.price;
                parkingOrderEntity.totalAmount = totalAmount;
                parkingOrderEntity.start_time = startTime;
                parkingOrderEntity.create_time = nowTime;
                parkingOrderEntity.update_time = nowTime;
                parkingOrderEntity.chargeStopTime = orderEntity.stopTime;
                parkingOrderEntity.id = parkingOrderEntity.insertGetId();
                if (parkingOrderEntity.id == 0) return new SyncResult(30, "新增占用费收费订单失败");
                else return new SyncResult(0, "");//继续监控
            } else {
                //检查是否已经结账，如果已经结账，则移除监控
                if (parkingOrderEntity.payment_status == 1) {
                    // 这里产生一个情景：用户产生了占用费订单，但是缴费了不拔充电头不移车。所以这里可能需要检查一下用户是否已经拔掉了插头
                    // 答：在缴费的时候检查一下插头是否拔掉了
                    ParkingMonitorApp.remove(OrderSN, "用户已经结账");
                    return new SyncResult(16, "用户已经结账");
                }

                //能进行到这一步，基本上是已经产生订单的了,所以只需更新占用费订单的收费金额
                Map<String, Object> set_data = new LinkedHashMap<>();
                set_data.put("totalAmount", totalAmount);
                set_data.put("end_time", nowTime);
                set_data.put("update_time", nowTime);
                parkingOrderEntity.update(parkingOrderEntity.id, set_data);
            }

            switch (deviceSocketEntity.status) {
                case 3://已充满电（用户还没有拔掉充电器）
                    return new SyncResult(0, "");//继续监控
                case 0://空闲：
                    //情景1：用户拔掉走人 进行结算
                case 1://充电中
                    //情景1：用户已经拔掉走人，新的用户使用了此端口
                case 5: //浮充
                    //情景1：用户已经拔掉走人了，新的用户使用了此端口，并且已经快充电完成
                case 2://未启动充电
                    //情景1：用户拔掉走人了，新的用户正准备充电
                    ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头1 状态：" + deviceSocketEntity.status);
                    return new SyncResult(1, "用户已经拔掉充电头1");
                default://其他状态：
                    ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头1 状态：" + deviceSocketEntity.status);
                    return new SyncResult(14, "插座发生故障，不产生占用费收费1");
            }
        } catch (Exception e) {
            LogsUtil.error(e, "", "充电监控发生错误");
        }
        return new SyncResult(1, "发生错误");
    }

    /**
     * 运行：备份待删除
     * <p>
     * 1、检查订单是否已经结束充电
     * 2、检查设备端口状态是否为 3=已充满电
     * 3、检查是否开始收费
     * 4、检查是否有占用费收费订单，不存在则新增，存在则更新占用费收费订单的数据
     *
     * @param OrderSN 订单号
     * @return
     */
    private SyncResult run1(String OrderSN) {
        try {
            //region 检查订单是否已经结束充电
            ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findEntity();
            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到充电订单");
            //状态,-1=错误，0=待启动，1=充电中，2=已完成
            if (orderEntity.status != 2) return new SyncResult(11, "充电订单必须完成才能开始任务");
            //endregion

            //region 读取设备信息
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(12, "查询不到设备信息");
            if (deviceEntity.parkingConfigId == 0) {
                ParkingMonitorApp.remove(OrderSN, "此设备没有配置占用费收费标准");
                return new SyncResult(13, "此设备没有配置占用费收费标准");
            }
            //endregion

            //region 读取插座信息
            DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                    .where("deviceId", deviceEntity.id)
                    .where("port", orderEntity.port)
                    .findEntity();
            if (deviceSocketEntity == null || deviceSocketEntity.id == 0) {
                return new SyncResult(13, "查询不到设备插座数据");
            }
            //endregion

            //region 读取占用费收费配置以及检查占用费收费配置
            ParkingConfigEntity parkingConfigEntity = ParkingConfigEntity.getInstance()
                    .cache(String.format("ParkingConfig:%s:Details", deviceEntity.parkingConfigId))
                    .findEntity(deviceEntity.parkingConfigId);
            if (parkingConfigEntity == null || parkingConfigEntity.id == 0) {
                return new SyncResult(14, "查询不到占用费收费标准配置，请检查配置是否存在");
            }
            //endregion

            long nowTime = TimeUtil.getTimestamp();
            ParkingItemEntity parkingItemEntity = null;

            //检查一下是否生成了占用费收费订单，如果没有生成，则检查插座的状态进行处理
            ParkingOrderEntity parkingOrderEntity = ParkingOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findEntity();
            //没有产生占用费收费订单
            if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
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
                        return new SyncResult(14, "插座发生故障，不产生占用费收费");
                }
            }

            //region 检查是否开始收费

            //计算开始占用费收费时间戳
            long startTime = orderEntity.stopTime + parkingConfigEntity.delayTime * 1000;
            if (startTime > nowTime) return new SyncResult(0, "还没进入收费时段");

            //region 读取占用费收费设置
            List<ParkingItemEntity> parkingItemEntityList = ParkingItemEntity.getInstance()
                    .cache(String.format("ParkingConfig:%s:Items", deviceEntity.parkingConfigId))
                    .where("configId", deviceEntity.parkingConfigId)
                    .page(1, 10)
                    .order("id")
                    .selectList();
            Iterator it = parkingItemEntityList.iterator();
            while (it.hasNext()) {
                ParkingItemEntity nd = (ParkingItemEntity) it.next();
                if (TimeUtil.isBelongPeriodTime(nowTime, nd.startDate, nd.endDate)) {
                    parkingItemEntity = nd;
                    break;
                }
            }

            if (parkingItemEntity == null) return new SyncResult(0, "该时段不收费");
            if (parkingItemEntity.price == 0) return new SyncResult(0, "该时段不收费");

            //endregion

            //endregion

            boolean isNew = false;//用来表示是否存在了新的订单
            //region 根据设备码和端口查询此设备最后充电的一笔订单
            ChargeOrderEntity chargeOrderEntity = ChargeOrderEntity.getInstance()
                    .where("deviceCode", orderEntity.deviceCode)
                    .where("port", orderEntity.port)
                    .where("create_time", ">=", TimeUtil.getTime00())
                    .where("create_time", "<=", TimeUtil.getTime24())
                    .order("id DESC")
                    .findEntity();
            if (chargeOrderEntity != null && chargeOrderEntity.id > 0) {
                //判断订单号是否相同,如果不同则表示有新的订单存在
                if (!chargeOrderEntity.OrderSN.equals(OrderSN)) isNew = true;
            }
            //endregion

            long parkingTime = (nowTime - startTime) / 1000;
            int parkingHour = TimeUtil.convertToHourFull(parkingTime);
            double totalAmount = parkingHour * parkingItemEntity.price;

            //如果是新的订单，并且还没产生占用费收费订单，则移除监控
            if (isNew) {
                //如果是新的订单，并且还没产生占用费收费，则移除占用费监控
                if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
                    ParkingMonitorApp.remove(OrderSN, "已有新的订单正在充电，上一个用户已经移走");
                    return new SyncResult(15, "已有新的订单正在充电，上一个用户已经移走");
                } else {
                    //如果是新的订单，但是已经有了占用费收费订单，则更新数据，等待用户支付，并且移除占用费监控
                    Map<String, Object> set_data = new LinkedHashMap<>();
                    set_data.put("totalAmount", totalAmount);
                    set_data.put("end_time", nowTime);
                    parkingOrderEntity.where("OrderSN", OrderSN).update(set_data);

                    ParkingMonitorApp.remove(OrderSN, "已有新的订单正在充电，上一个用户已经移走");
                    return new SyncResult(15, "已有新的订单正在充电，上一个用户已经移走");
                }
            }

            //状态：0=空闲，1=充电中，2=未启动充电，3=已充满电（用户还没有拔掉充电器），4=故障，5=浮充
            //不存在订单则新增占用费订单：这里也不需要判断状态是否为3，因为不存在订单状态为其他的，上面已经结束监控了
            if (parkingOrderEntity == null || parkingOrderEntity.id == 0) {
                //情景1：用户充电完成，但是还没拔掉充电头
                parkingOrderEntity = new ParkingOrderEntity();
                parkingOrderEntity.OrderSN = OrderSN;
                parkingOrderEntity.deviceCode = orderEntity.deviceCode;
                parkingOrderEntity.port = orderEntity.port;
                parkingOrderEntity.uid = orderEntity.uid;
                parkingOrderEntity.payment_status = 0;//状态：0=未结算，1=已结算
                parkingOrderEntity.delayTime = parkingConfigEntity.delayTime;
                parkingOrderEntity.price = parkingItemEntity.price;
                parkingOrderEntity.totalAmount = totalAmount;
                parkingOrderEntity.start_time = startTime;
                parkingOrderEntity.create_time = nowTime;
                parkingOrderEntity.update_time = nowTime;
                parkingOrderEntity.chargeStopTime = orderEntity.stopTime;
                parkingOrderEntity.id = parkingOrderEntity.insertGetId();
                if (parkingOrderEntity.id == 0) return new SyncResult(30, "新增占用费收费订单失败");
                else return new SyncResult(0, "");//继续监控
            } else {
                //检查是否已经结账，如果已经结账，则移除监控
                if (parkingOrderEntity.payment_status == 1) {
                    // 这里产生一个情景：用户产生了占用费订单，但是缴费了不拔充电头不移车。所以这里可能需要检查一下用户是否已经拔掉了插头
                    // 答：在缴费的时候检查一下插头是否拔掉了
                    ParkingMonitorApp.remove(OrderSN, "用户已经结账");
                    return new SyncResult(16, "用户已经结账");
                }

                //能进行到这一步，基本上是已经产生订单的了,所以只需更新占用费订单的收费金额
                Map<String, Object> set_data = new LinkedHashMap<>();
                set_data.put("totalAmount", totalAmount);
                set_data.put("end_time", nowTime);
                parkingOrderEntity.where("OrderSN", OrderSN).update(set_data);
            }

            switch (deviceSocketEntity.status) {
                case 3://已充满电（用户还没有拔掉充电器）
                    return new SyncResult(0, "");//继续监控
                case 0://空闲：
                    //情景1：用户拔掉走人 进行结算
                case 1://充电中
                    //情景1：用户已经拔掉走人，新的用户使用了此端口
                case 5: //浮充
                    //情景1：用户已经拔掉走人了，新的用户使用了此端口，并且已经快充电完成
                case 2://未启动充电
                    //情景1：用户拔掉走人了，新的用户正准备充电
                    ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头1 状态：" + deviceSocketEntity.status);
                    return new SyncResult(1, "用户已经拔掉充电头1");
                default://其他状态：
                    ParkingMonitorApp.remove(OrderSN, "用户已经拔掉充电头1 状态：" + deviceSocketEntity.status);
                    return new SyncResult(14, "插座发生故障，不产生占用费收费1");
            }
        } catch (Exception e) {
            LogsUtil.error(e, "[占用费监控任务] %s 执行期间发生错误", OrderSN);
        }
        return new SyncResult(1, "执行错误");
    }
}
