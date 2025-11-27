package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

/**
 * 设备离线监测任务类：
 * GeneralDeviceOfflineMonitorTaskJob用于定期检查每台设备的在线状态。
 * 当设备不再发送心跳数据并被认为离线时，本任务将触发通知服务，提醒相关人员设备离线。
 */
public class GeneralDeviceOfflineMonitorTaskJob implements Job {
    protected final static String TAG = "设备离线检测任务";
    private final String mGroupName = this.getClass().getSimpleName();
    private final static ExecutionThrottle mExecutionThrottle = new ExecutionThrottle(100000, ECacheTime.DAY);

    private static volatile GeneralDeviceOfflineMonitorTaskJob _this;

    /**
     * 获取 GeneralDeviceOfflineMonitorTaskJob 的单例实例
     *
     * @return GeneralDeviceOfflineMonitorTaskJob实例
     */
    public static GeneralDeviceOfflineMonitorTaskJob getInstance() {
        if (_this == null) {
            synchronized (GeneralDeviceOfflineMonitorTaskJob.class) {
                if (_this == null) {
                    _this = new GeneralDeviceOfflineMonitorTaskJob();
                }
            }
        }
        return _this;
    }

    /**
     * 添加监控任务
     * 为指定设备的序列号(serialNumber)添加离线监控任务，记录设备当前为在线状态。
     * 若该任务已存在，则避免重复添加。
     *
     * @param serialNumber 设备的唯一序列号，用于识别具体设备
     */
    public void add(String serialNumber) {
        add(serialNumber, ECacheTime.MINUTE * 5);
    }

    /**
     * 添加监控任务
     * 为指定设备的序列号(serialNumber)添加离线监控任务，记录设备当前为在线状态。
     * 若该任务已存在，则避免重复添加。
     *
     * @param serialNumber 设备的唯一序列号，用于识别具体设备
     * @param expire       过期时间(ms)
     */
    public void add(String serialNumber, long expire) {
        // 将设备在线状态记录到缓存，有效期5分钟
        DataService.getMainCache().set(String.format("GeneralDevice:Online:%s", serialNumber)
                , 1
                , expire
        );
        // 使用ExecutionThrottle限制执行频率，避免同一设备重复执行
        mExecutionThrottle.run(data -> {
            if (QuartzSchedulerManager.getInstance().checkExists(serialNumber, mGroupName)) {
                return new SyncResult(0, "");
            }

            // 创建一个简单触发器，每分钟触发一次，持续执行
            SimpleTrigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(1) // 每分钟触发一次
                            .withMisfireHandlingInstructionFireNow()
                            .repeatForever()) // 持续重复
                    .build();

            // 将任务添加到调度器中，并关联到GeneralDeviceOfflineMonitorTaskJob类
            QuartzSchedulerManager.getInstance().add(serialNumber
                    , mGroupName
                    , trigger
                    , GeneralDeviceOfflineMonitorTaskJob.class
                    , null);

            LogsUtil.info(TAG, "[%s] 添加任务", serialNumber);

            return new SyncResult(0, "");
        }, String.format("%sAdd_%s", mGroupName, serialNumber), ECacheTime.MINUTE * 10, null);

    }

    /**
     * 定期执行设备离线检查逻辑
     * 当设备在缓存中标记为不在线时，将执行离线通知逻辑并触发相应的离线处理。
     *
     * @param jobExecutionContext Quartz框架传递的任务上下文，包含任务执行时的信息
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try {
            // 从任务上下文中获取当前设备的序列号
            String serialNumber = jobExecutionContext.getJobDetail().getKey().getName();

            // 从缓存中获取设备在线状态，若设备为在线状态则不做处理
            int v = DataService.getMainCache().getInt(String.format("GeneralDevice:Online:%s", serialNumber), -1);
            if (v == 1) return;

            // 离线处理逻辑，限制8小时内仅发送一次通知
            mExecutionThrottle.run(data -> {
                LogsUtil.info(TAG, "[%s] - 已离线，正在推送通知...", serialNumber, data);

                GeneralDeviceService generalDeviceService = GeneralDeviceService.getInstance();
                // 根据序列号获取设备信息
                GeneralDeviceEntity deviceEntity = generalDeviceService.getWithSerialNumber(serialNumber, true);
                if (deviceEntity == null || deviceEntity.status == 0) {
                    // 若设备不存在或设备状态为0（表示已无效），则删除监控任务
                    QuartzSchedulerManager.getInstance().del(serialNumber, mGroupName);
                    return new SyncResult(0, "");
                }

                // 更新设备状态为离线（在线状态标识设为0）
                generalDeviceService.updateOnlineStatus(deviceEntity.serialNumber, 0);

                // 创建通知数据
                JSONObject notifyTransData = new JSONObject();
                notifyTransData.put("serialNumber", serialNumber);

                // 通过通知服务发送离线通知
                NotifyService.getInstance().asyncPush(serialNumber
                        , "SYSTEM.GENERAL.DEVICE.OFFLINE"
                        , ENotifyType.WECHATCORPBOT
                        , notifyTransData
                        , GeneralDeviceService.iNotifyServiceTransDataBuilder);

                return new SyncResult(0, "");
            }, String.format("%s_%s", mGroupName, serialNumber), ECacheTime.HOUR * 8, null);
        } catch (Exception e) {
            // 捕获并记录执行过程中发生的异常
            LogsUtil.error(e, TAG, "执行期间发生错误");
        }
    }
}