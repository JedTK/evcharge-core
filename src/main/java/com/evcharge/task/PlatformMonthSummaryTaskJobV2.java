package com.evcharge.task;

import com.evcharge.entity.platform.PlatformMonthSummaryEntity;
import com.evcharge.entity.platform.PlatformMonthSummaryV2Entity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

/**
 * 平台 每月 数据统计：每日5:00触发
 */
public class PlatformMonthSummaryTaskJobV2 implements Job {
    protected final static String TAG = "平台月数据统计";
    private final static String mTaskName = PlatformMonthSummaryTaskJobV2.class.getSimpleName();
    private final static String mGroupName = "Platform";

    private static PlatformMonthSummaryTaskJobV2 _this;

    public static PlatformMonthSummaryTaskJobV2 getInstance() {
        if (_this == null) _this = new PlatformMonthSummaryTaskJobV2();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public SyncResult init() {
        String cron = "0 30 0/6 * * ? *";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , this.getClass()
                , null
                , TAG);

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
        return r;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        PlatformMonthSummaryV2Entity.getInstance().startSyncTask(TimeUtil.getMonthBegin00(-1), TimeUtil.getMonthBegin00(), true);
    }

    public static void execute(String organize_code, long start_time, long end_time) {
        if (start_time == 0) start_time = TimeUtil.getMonthBegin00(-1);
        if (end_time == 0) end_time = TimeUtil.getMonthBegin00();

        //触发v2版本统计
        PlatformMonthSummaryV2Entity.getInstance().syncData(organize_code, start_time, end_time);

        if (organize_code.equalsIgnoreCase(SysGlobalConfigEntity.getString("System:Organize:Code", "genkigo"))) {
            ThreadUtil.sleep(ECacheTime.SECOND * 10);
            //触发v1版本统计
            PlatformMonthSummaryEntity.getInstance().syncData(TimeUtil.getAddMonthTimestamp(start_time, -1));
            PlatformMonthSummaryEntity.getInstance().syncData(TimeUtil.getAddMonthTimestamp(start_time, 0));
        }
    }
}
