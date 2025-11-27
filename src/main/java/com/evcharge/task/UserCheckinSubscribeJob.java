package com.evcharge.task;

import com.evcharge.entity.user.UserCheckinsEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;


public class UserCheckinSubscribeJob implements Job {
    protected final static String TAG = "微信订阅消息通知-签到提醒";
    private final static String mTaskName = UserCheckinSubscribeJob.class.getSimpleName();
    private final static String mGroupName = "User";
    private static UserCheckinSubscribeJob _this;

    public static UserCheckinSubscribeJob getInstance() {
        if (_this == null) _this = new UserCheckinSubscribeJob();
        return _this;
    }

    /**
     * 初始化任务
     *
     * @return
     */
    public SyncResult init() {
        String cron = "0 0 8 * * ?"; //每天早上8点
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , this.getClass()
                , null
                , TAG);

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
        return r;
    }

    /**
     * 执行任务
     *
     * @param jobExecutionContext
     * @throws JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LogsUtil.info(TAG, "\033[1;92m 执行中... 下次执行：%s\033[0m", TimeUtil.toTimeString(jobExecutionContext.getNextFireTime()));
        UserCheckinsEntity.getInstance().runWechatSubscribeMsgTask();
    }
}
