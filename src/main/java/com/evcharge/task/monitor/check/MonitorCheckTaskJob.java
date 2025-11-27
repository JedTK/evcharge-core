package com.evcharge.task.monitor.check;

import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

public class MonitorCheckTaskJob implements Job {

    protected final static String TAG = "元气充通用设备-监控检查日志";
    private final static String mTaskName = MonitorCheckTaskJob.class.getSimpleName();
    private final static String mGroupName = "MonitorCheckTaskV1";


    private static volatile MonitorCheckTaskJob _this;

    public static MonitorCheckTaskJob getInstance() {
        if (_this == null) {
            synchronized (MonitorCheckTaskJob.class) {
                if (_this == null) _this = new MonitorCheckTaskJob();
            }
        }
        return _this;
    }

    public SyncResult init() {
        String cron = "0 0 */4 * * ?";
        CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger().startNow().withSchedule(CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionDoNothing()).build();
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName, "MonitorCheckTaskV1", trigger, this.getClass(), (JobDataMap) null, "元气充通用设备-监控检查日志");
        LogsUtil.info("元气充通用设备-监控检查日志", "\u001b[1;91m %s 预计执行时间：%s \u001b[0m", new Object[]{r.msg, TimeUtil.toTimeString(trigger.getNextFireTime())});
        return r;

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);
        MonitorCheckApp.getInstance().run();
    }

    /**
     * 运行核心
     *
     * @param organizeCode String
     */
    public static void execute(String organizeCode) {
        //触发v2版本统计
        MonitorCheckApp.getInstance().run();
    }
}
