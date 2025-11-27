package com.evcharge.task;

import com.evcharge.entity.megadata.MDStreetDailySummaryEntity;
import com.evcharge.entity.platform.PlatformDaySummaryV2Entity;
import com.evcharge.entity.station.ChargeStationDailySummaryEntity;
import com.evcharge.entity.station.ChargeStationDaySummaryV2Entity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

/**
 * 充电桩每日数据统计任务（V2）
 * <p>
 * 职责：
 * 1) 通过 Quartz 定时器周期性触发（当前 Cron: 每两小时在第 10 分钟执行）；
 * 2) 统一计算统计窗口 [yesterday 00:00, today 00:00)，并依次触发：
 * - 充电站日汇总（V2）
 * - 平台日汇总（V2）
 * - 街道日汇总（MDStreet）
 * 3) 提供静态 execute(...) 供单站/指定时间窗口复算，兼容 V1/V2 两套口径（不改变现有业务流程）。
 */
public class ChargeStationDaySummaryTaskJobV2 implements Job {

    // ===== 常量与元信息 =====

    /**
     * 日志标签
     */
    protected static final String TAG = "充电桩日数据统计";
    /**
     * Job 名称（用于 Quartz 注册）
     */
    private static final String TASK_NAME = ChargeStationDaySummaryTaskJobV2.class.getSimpleName();
    /**
     * Job 分组
     */
    private static final String GROUP_NAME = "ChargeStationTask";
    /**
     * Cron 表达式：0秒，第10分，从0点开始每2小时一次（0,2,4,...,22点）
     */
    private static final String CRON_EXPRESSION = "0 10 0/2 * * ? *";

    /**
     * 惰性单例（与现有工程风格保持一致）
     */
    private static volatile ChargeStationDaySummaryTaskJobV2 _this;

    /**
     * 获取单例实例（DCL）
     */
    public static ChargeStationDaySummaryTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (ChargeStationDaySummaryTaskJobV2.class) {
                if (_this == null) _this = new ChargeStationDaySummaryTaskJobV2();
            }
        }
        return _this;
    }

    /**
     * 添加一个定时任务：
     * - 立即启动（startNow），并按 CRON_EXPRESSION 周期执行；
     * - MISFIRE 策略：错过触发后立即补触发一次（FireAndProceed）。
     */
    public void init() {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        // 添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(
                TASK_NAME,
                GROUP_NAME,
                trigger,
                ChargeStationDaySummaryTaskJobV2.class,
                null,
                TAG
        );

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    // ======================== Quartz Job 入口 ========================

    /**
     * 定时任务入口：
     * - 统一计算时间窗口（避免多次重复计算引起的极端时刻偏差）；
     * - 顺序触发三项统计（保持原有顺序与同步调用方式，不并发、不增加重试）。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        // 统一计算统计窗口：昨日 00:00 -> 今日 00:00
        final long startTime = TimeUtil.getTime00(-1);
        final long endTime = TimeUtil.getTime00();

        // V2：按站点维度的日汇总
        ChargeStationDaySummaryV2Entity.getInstance().startSyncTask(startTime, endTime, true);

        // V2：平台维度日汇总
        PlatformDaySummaryV2Entity.getInstance().startSyncTask(startTime, endTime, true);

        // 街道每日统计
        MDStreetDailySummaryEntity.getInstance().startSyncTask(startTime, endTime, true);
    }

    // ======================== 单站复算入口 ========================

    /**
     * 单站指定窗口复算：
     * - 若 start_time/end_time 为 0，则回落到 [昨日 00:00, 今日 00:00)；
     * - 先触发 V2 模型，再兼容性触发 V1（昨日、当日各一次）。
     *
     * @param CSId       站点 ID（字符串格式，V1 需要 long）
     * @param start_time 统计窗口开始（含），0 表示使用昨日 00:00
     * @param end_time   统计窗口结束（不含），0 表示使用今日 00:00
     */
    public static void execute(String CSId, long start_time, long end_time) {
        if (start_time == 0) start_time = TimeUtil.getTime00(-1);
        if (end_time == 0) end_time = TimeUtil.getTime00();

        // V2：站点窗口复算
        ChargeStationDaySummaryV2Entity.getInstance().syncData(CSId, start_time, end_time);

        // V1：兼容性复算（昨日 / 当日）
        final long csIdLong = Convert.toLong(CSId);
        final long dayMinus1 = TimeUtil.getAddDayTimestamp(start_time, -1);
        final long day0 = TimeUtil.getAddDayTimestamp(start_time, 0);

        ChargeStationDailySummaryEntity.getInstance().syncData(csIdLong, dayMinus1);
        ChargeStationDailySummaryEntity.getInstance().syncData(csIdLong, day0);
    }
}
