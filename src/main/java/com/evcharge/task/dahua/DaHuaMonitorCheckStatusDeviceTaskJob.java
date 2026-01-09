package com.evcharge.task.dahua;

import com.evcharge.libsdk.dahua.DaHuaDeviceSDK;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

public class DaHuaMonitorCheckStatusDeviceTaskJob implements Job {
    protected final static String TAG = "大华平台监控设备检查在线状态";
    private final static String mTaskName = DaHuaMonitorCheckStatusDeviceTaskJob.class.getSimpleName();
    private final static String mGroupName = "DaHua_NVR_Status";

    private static DaHuaMonitorCheckStatusDeviceTaskJob _this;

    public static DaHuaMonitorCheckStatusDeviceTaskJob getInstance() {
        if (_this == null) _this = new DaHuaMonitorCheckStatusDeviceTaskJob();
        return _this;
    }


    /**
     * 初始化任务
     */
    public SyncResult init() {
        String cron = "0 0/30 * * * ? *"; //每半小时执行一次
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , this.getClass()
                , null);

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
        return r;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        execute();
    }

    public void execute() {
        LogsUtil.info(TAG, " - 开始执行 - ");
        DaHuaDeviceSDK daHuaDeviceSDK = new DaHuaDeviceSDK();
        daHuaDeviceSDK.syncCheckDeviceStatus();
        LogsUtil.info(TAG, " - 执行结束 - ");
    }

}