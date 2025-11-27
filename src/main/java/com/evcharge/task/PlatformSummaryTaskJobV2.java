package com.evcharge.task;

import com.evcharge.entity.payment.PaymentTypeMonthSummaryEntity;
import com.evcharge.entity.platform.PlatformSummaryEntity;
import com.evcharge.entity.platform.PlatformSummaryV2Entity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

/**
 * 平台数据汇总任务
 */
public class PlatformSummaryTaskJobV2 implements Job {
    protected final static String TAG = "平台数据统计";
    private final static String mTaskName = PlatformSummaryTaskJobV2.class.getSimpleName();
    private final static String mGroupName = "Platform";

    private static PlatformSummaryTaskJobV2 _this;

    public static PlatformSummaryTaskJobV2 getInstance() {
        if (_this == null) _this = new PlatformSummaryTaskJobV2();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public SyncResult init() {
        String cron = "0 30 0/8 * * ? *";
        //触发时间
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
                , TAG
        );

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
        return r;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        PlatformSummaryV2Entity.getInstance().startSyncTask(true);
    }

    public static void execute(String organize_code) {
        // 触发v2版本统计
        PlatformSummaryV2Entity.getInstance().syncTaskJob(organize_code);

        // 触发支付类型版本统计
        PaymentTypeMonthSummaryEntity.getInstance().startSync(TimeUtil.getTimestamp());

        if (organize_code.equalsIgnoreCase(SysGlobalConfigEntity.getString("System:Organize:Code", "genkigo"))) {
            // 触发v1版本统计
            ThreadUtil.sleep(ECacheTime.SECOND * 10);
            PlatformSummaryEntity.getInstance().syncData();
        }
    }
}
