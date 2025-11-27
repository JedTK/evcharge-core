package com.evcharge.task;

import com.evcharge.entity.basedata.ParkingConfigEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.TriggerBuilder;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 占用费监控任务
 */
public class ParkingMonitorApp {
    protected final static String mMonitorText = "占用监控任务";
    private final static String mGroupName = "Parking";
    private final static String mServerIPv4 = common.getLocalIPv4();

    /**
     * 开始监控
     */
    public static SyncResult add(String OrderSN) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(1, "");

        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        return add(orderEntity);
    }

    /**
     * 开始监控
     */
    public static SyncResult add(ChargeOrderEntity orderEntity) {
        try {
            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到充电订单");
            //状态,-1=错误，0=待启动，1=充电中，2=已完成
            if (orderEntity.status != 2) return new SyncResult(11, "充电订单必须完成才能开始任务");

            String OrderSN = orderEntity.OrderSN;

            //读取设备信息
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(12, "查询不到设备信息");

            //检查占用费收费配置
            if (deviceEntity.parkingConfigId == 0) return new SyncResult(13, "此设备没有配置占用费收费标准");
            //读取占用费收费配置
            ParkingConfigEntity parkingConfigEntity = ParkingConfigEntity.getInstance()
                    .cache(String.format("ParkingConfig:%s:Details", deviceEntity.parkingConfigId))
                    .findEntity(deviceEntity.parkingConfigId);
            if (parkingConfigEntity == null || parkingConfigEntity.id == 0) {
                return new SyncResult(14, "查询不到占用费收费标准配置，请检查配置是否存在");
            }

            //计算开始占用费收费时间戳
            long startTime = orderEntity.stopTime + parkingConfigEntity.delayTime * 1000;

            //设置好触发器
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
            CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(OrderSN, mGroupName)
                    .startAt(TimeUtil.toDate(startTime))//设置第一次开始时间
                    .withSchedule(cronScheduleBuilder
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();

            //添加任务到调度器
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("OrderSN", OrderSN);
            SyncResult r = QuartzSchedulerManager.getInstance().add(OrderSN
                    , mGroupName
                    , cronTrigger
                    , ParkingMonitorJob.class
                    , jobDataMap
                    , "占用费监控任务"
            );
            if (r.code == 0) {
                DataService.getMainCache().setMap(String.format("Task:%s:%s:%s", mServerIPv4, mGroupName, OrderSN), jobDataMap, true);
                LogsUtil.info("", "\033[1;94m [%s] - 开始监控：OrderSN=%s 【%s】触发 \033[0m", mMonitorText, OrderSN, TimeUtil.toTimeString(startTime));
            } else {
                LogsUtil.info("", "\033[1;94m [%s] - 开始监控发生错误：OrderSN=%s 原因=%s \033[0m", mMonitorText, OrderSN, r.msg);
            }
            return r;
        } catch (Exception e) {
            LogsUtil.error(e, "", "添加充电监控任务发生错误");
        }
        return new SyncResult(1, "添加失败");
    }

    /**
     * 移除监控任务
     *
     * @param OrderSN
     * @param delReason
     */
    public static void remove(String OrderSN, String delReason) {
        try {
            QuartzSchedulerManager.getInstance().del(OrderSN, mGroupName);
            DataService.getMainCache().del(String.format("Task:%s:%s:%s", mServerIPv4, mGroupName, OrderSN));
            DataService.getMainCache().del(String.format("Task:%s:ErrorCount:%s:%s", mServerIPv4, mGroupName, OrderSN));
            LogsUtil.info("", "\033[1;94m [%s] - 移除监控：OrderSN=%s 原因=%s \033[0m", mMonitorText, OrderSN, delReason);
        } catch (Exception e) {
            LogsUtil.error(e, "", "删除充电监控任务发生错误");
        }
    }

    /**
     * 恢复监控任务
     */
    public static void resume() {
        ThreadUtil.getInstance().execute(String.format("恢复[%s]", mMonitorText), () -> {
            LogsUtil.info("", "\033[1;94m [%s] - 恢复监控 %s \033[0m", mMonitorText, mServerIPv4);

            Set<String> keyset = DataService.getMainCache().keys(String.format("Task:%s:%s:*", mServerIPv4, mGroupName));
            if (keyset.size() == 0) {
                LogsUtil.info("", "\033[1;94m [%s] - 恢复监控 %s - 找不到占用费订单缓存数据 \033[0m", mMonitorText, mServerIPv4);
                return;
            }

            Iterator it = keyset.iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                Map<String, Object> data = DataService.getMainCache().getMap(key);
                String OrderSN = MapUtil.getString(data, "OrderSN");
                add(OrderSN);
            }
        });
    }
}
