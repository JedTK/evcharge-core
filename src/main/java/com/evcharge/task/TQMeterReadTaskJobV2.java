package com.evcharge.task;

import com.evcharge.service.meter.TQ4GMeterService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

/**
 * 浙江拓强 - 4G电能表定时抄表任务
 * <p>
 * 功能：
 * - 定时拉取4G电能表列表
 * - 调用SDK进行抄表操作
 * - 支持单机或RocketMQ分布式处理（目前默认单机）
 * </p>
 * 作者：JED
 * 日期：2025-04-17
 */
public class TQMeterReadTaskJobV2 implements Job {
    private static final String TAG = "拓强4G电表-抄表";
    private static final String TASK_NAME = TQMeterReadTaskJobV2.class.getSimpleName();
    private static final String GROUP_NAME = "4GEM";

    /**
     * 每小时的 00 与 30 分执行（Quartz: 秒 分 时 日 月 周）
     */
    private static final String CRON_EXPRESSION = "0 0,30 * * * ?";

    private static volatile TQMeterReadTaskJobV2 instance = null;


    /**
     * 单例模式获取任务实例
     */
    public static TQMeterReadTaskJobV2 getInstance() {
        if (instance == null) {
            synchronized (TQMeterReadTaskJobV2.class) {
                if (instance == null) {
                    instance = new TQMeterReadTaskJobV2();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化并添加定时任务（Cron表达式：每小时的第30分和59分执行）
     */
    public void init() {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(TASK_NAME, GROUP_NAME)
                .withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        SyncResult result = QuartzSchedulerManager.getInstance().add(TASK_NAME
                , GROUP_NAME
                , trigger
                , TQMeterReadTaskJobV2.class
                , null
                , TAG);

        LogsUtil.info(TAG, "任务添加结果：%s，预计首次执行时间：%s", result.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    /**
     * Quartz定时任务执行入口
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行抄表任务", group, key);

        TQ4GMeterService.getInstance().readTask(false);
    }
}
