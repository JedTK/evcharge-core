package com.evcharge.task;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;

import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;

/**
 * 设备监控任务服务类：用于周期性地监控设备的在线状态，并在设备下线或拆除时做相应处理
 * <p>
 * 主要职责：
 * 1. 添加/删除设备的监控任务
 * 2. 定期执行监控任务，检查设备是否在线
 * 3. 在设备下线时执行对应操作
 */
public class DeviceMonitorTaskJob implements Job {
    // 设备监控任务的描述文本，方便在日志中进行识别
    private final static String TAG = "设备监控任务";

    // Quartz 任务组名称，使用类名作为唯一标识
    private final static String mGroupName = DeviceMonitorTaskJob.class.getSimpleName();

    // 异步监听器接口，用于在任务执行后传递执行结果
    private IAsyncListener mIAsyncListener;

    // 单例模式实例，确保每个设备只存在一个监控任务实例
    private static volatile DeviceMonitorTaskJob _this;

    /**
     * 获取单例对象
     * 如果对象未实例化，则创建一个新的实例；否则返回现有的实例
     */
    public static DeviceMonitorTaskJob getInstance() {
        if (_this == null) {
            synchronized (DeviceMonitorTaskJob.class) {
                if (_this == null) _this = new DeviceMonitorTaskJob();
            }
        }
        return _this;
    }

    /**
     * 添加一个设备的监控任务
     *
     * @param deviceCode     设备编码，作为任务的唯一标识
     * @param expire         缓存时间
     * @param iAsyncListener 异步监听器，任务执行结束后调用此监听器返回结果
     * @return SyncResult 同步操作的结果，返回成功或错误信息
     */
    public void add(String deviceCode, long expire, IAsyncListener iAsyncListener) {
        // 校验设备编码不能为空
        if (!StringUtils.hasLength(deviceCode)) return;

        // 插入缓存
        DataService.getMainCache().set(String.format("Device:%s:status", deviceCode)
                , 1
                , expire);

        // 使用ExecutionThrottle限制执行频率，避免同一设备重复执行
        ExecutionThrottle.getInstance().run(data -> {
            // 设置任务的触发时间，任务将在当前时间 5 分钟后开始执行，每 5 分钟执行一次
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(TimeUtil.toDate(TimeUtil.getTimestamp() + ECacheTime.MINUTE * 5))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(5)
                            .repeatForever()
                            .withMisfireHandlingInstructionFireNow()
                    )
                    .build();

            // 将任务添加到 Quartz 调度器中，任务会以指定的触发时间开始执行
            SyncResult r = QuartzSchedulerManager.getInstance().add(deviceCode
                    , mGroupName
                    , trigger
                    , this.getClass()
                    , null
                    , TAG
            );

            // 如果任务添加成功，保存异步监听器用于后续回调
            if (r.code == 0) this.mIAsyncListener = iAsyncListener;
            return r;
        }, String.format("%sAdd_%s", mGroupName, deviceCode), ECacheTime.MINUTE * 10, null);
    }

    /**
     * 移除设备的监控任务
     *
     * @param deviceCode 设备编码，用于唯一标识需要删除的任务
     */
    public void remove(String deviceCode) {
        QuartzSchedulerManager.getInstance().del(deviceCode, mGroupName);
    }

    /**
     * 任务执行逻辑
     * 这是 Quartz 任务调度器周期性触发执行的方法
     *
     * @param jobExecutionContext Quartz 提供的任务执行上下文，包含任务执行相关的信息
     * @throws JobExecutionException 如果任务执行时发生错误，将抛出此异常
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 获取任务执行时的参数（设备编码）
        String deviceCode = jobExecutionContext.getJobDetail().getKey().getName();

        // 记录任务执行的日志，并显示下次执行时间
        LogsUtil.info(TAG, "\033[1;92m [%s] 执行中... 下次执行：%s \033[0m"
                , deviceCode
                , TimeUtil.toTimeString(jobExecutionContext.getNextFireTime()));

        // 调用监控任务，监测设备状态
        SyncResult r = run(deviceCode);

        // 如果异步监听器不为空，则将任务执行结果通过监听器传递出去
        if (mIAsyncListener != null) mIAsyncListener.onResult(r.code, new LinkedHashMap<>() {{
            put("deviceCode", deviceCode);
        }});

        // 如果设备已被标记为拆除，移除该设备的监控任务
        if (r.code == 99) remove(deviceCode);
    }

    /**
     * 设备监控任务
     *
     * @param deviceCode 设备编码，用于唯一标识需要监控的设备
     * @return SyncResult 同步操作结果，包含任务执行状态和结果描述
     */
    private SyncResult run(String deviceCode) {
        // 从缓存中获取设备状态（缓存的设备状态通过设备编码保存）
        int status = DataService.getMainCache().getInt(String.format("Device:%s:status", deviceCode), -1);
        // 从缓存中获取设备在线状态，若设备为在线状态则不做处理
        if (status == 1) return new SyncResult(0, "");

        // 如果设备未拆除但状态不在线，则更新设备的状态为离线
        DeviceEntity.getInstance()
                .where("deviceCode", deviceCode)
                .update(new LinkedHashMap<>() {{
                    put("online_status", 0); // 在线状态：0=离线，1=在线
                }});

        ExecutionThrottle.getInstance().run(data -> {
            // 从数据库或其他存储中获取设备的实体对象
            DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode, true);
            // 如果设备实体不存在或设备 ID 为 0，表示设备信息不完整，返回错误结果
            if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(99, "设备信息不完整。");
            // 如果设备被标记为已拆除（在线状态为 99），返回特殊状态码 99，表示设备已拆除
            if (deviceEntity.online_status == 99) return new SyncResult(99, "设备已拆除。");
            return new SyncResult(0, "");
        }, String.format("%sRemove_%s", mGroupName, deviceCode), ECacheTime.HOUR, null);

        // 返回设备离线的同步结果
        return new SyncResult(1, "设备已离线。");
    }
}