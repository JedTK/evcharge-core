package com.evcharge.task;

import com.evcharge.entity.megadata.MDStreetDailySummaryEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

/**
 * 街道/城镇每日数据汇总
 */
public class MDStreetDaySummaryTaskJob implements Job {
    protected final static String TAG = "街道/城镇每日数据汇总";
    private final static String mTaskName = MDStreetDaySummaryTaskJob.class.getSimpleName();
    private final static String mGroupName = "Area";

    private static MDStreetDaySummaryTaskJob _this;

    public static MDStreetDaySummaryTaskJob getInstance() {
        if (_this == null) _this = new MDStreetDaySummaryTaskJob();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public void init() {
        String cron = "0 30 0/2 * * ?";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , ChargeStationDaySummaryTaskJobV2.class
                , null
                , TAG);

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        MDStreetDailySummaryEntity.getInstance().startSyncTask(TimeUtil.getTime00(-1), TimeUtil.getTime00(), true);
    }
}
