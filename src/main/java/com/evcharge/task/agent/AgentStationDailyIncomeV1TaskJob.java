package com.evcharge.task.agent;

import com.evcharge.entity.agent.summary.AgentStationDailyIncomeV1Entity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

public class AgentStationDailyIncomeV1TaskJob implements Job {

    protected final static String TAG = "元气充代理-充电桩月数据统计";
    private final static String mTaskName = AgentStationDailyIncomeV1TaskJob.class.getSimpleName();
    private final static String mGroupName = "AgentStationDaily";

    private static volatile AgentStationDailyIncomeV1TaskJob _this;

    public static AgentStationDailyIncomeV1TaskJob getInstance() {
        if (_this == null) {
            synchronized (AgentStationDailyIncomeV1TaskJob.class) {
                if (_this == null) _this = new AgentStationDailyIncomeV1TaskJob();
            }
        }
        return _this;
    }

    public SyncResult init() {
        String cron = "0 0,30 * * * ?";
//        String cron = "0 0/5 * * * ?";
        CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger().startNow().withSchedule(CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionDoNothing()).build();
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName, "AgentStationDaily", trigger, this.getClass(), (JobDataMap) null, "元气充代理-站点-日数据统计");
        LogsUtil.info("元气充代理-站点-日数据统计", "\u001b[1;91m %s 预计执行时间：%s \u001b[0m", new Object[]{r.msg, TimeUtil.toTimeString(trigger.getNextFireTime())});
        return r;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);
//        ChargeStationDaySummaryV2Entity.getInstance().startSyncTask(TimeUtil.getTime00(-1), TimeUtil.getTime00(), true);
        AgentStationDailyIncomeV1Entity.getInstance().startSyncTask(TimeUtil.getTime00(-1), TimeUtil.getTime00(), false);
    }

    /**
     * 运行核心
     *
     * @param organizeCode String
     * @param CSId String
     * @param startTime long
     * @param endTime long
     */
    public static void execute(String organizeCode,String CSId, long startTime, long endTime) {
        if (startTime == 0) startTime = TimeUtil.getTime00(-1);
        if (endTime == 0) endTime = TimeUtil.getTime00();
        //触发v2版本统计
        AgentStationDailyIncomeV1Entity.getInstance().syncData(organizeCode,CSId, startTime, endTime);

    }

}
