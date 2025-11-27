package com.evcharge.task;

import com.evcharge.entity.platform.PlatformMonthSummaryV2Entity;
import com.evcharge.entity.station.ChargeStationMonthSummaryV2Entity;
import com.evcharge.entity.station.ChargeStationMonthlySummaryEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

/**
 * 充电桩【每月】数据统计任务（V2）
 * <p>
 * 职责：
 * 1) 通过 Quartz 周期触发，计算时间窗口 [上月月初00:00, 本月月初00:00)；
 * 2) 先触发 站点月汇总(V2)，随后（可错峰 10s）触发 平台月汇总(V2)；
 * 3) 提供静态 execute(...) 以单站/指定窗口复算，并兼容触发 V1 老口径（上月、当月各一次）。
 * <p>
 * 重要说明（不改核心业务逻辑）：
 * - 仅做注释完善与轻量优化（统一时间窗口计算、常量提取、空值保护）；
 * - 保持 Cron、执行顺序、sleep 等行为不变；
 * - 类注释原文写“每日 5:00 触发”，但当前 Cron 为每 6 小时触发一次（见 CRON_EXPRESSION）。
 * 若确需“每日 05:00”触发，建议使用："0 0 5 * * ? *"（此变更会修改调度频率，故本提交未改）。
 */
public class ChargeStationMonthSummaryTaskJobV2 implements Job {

    // ===== 常量与元信息 =====

    /**
     * 日志标签
     */
    protected static final String TAG = "充电桩月数据统计";
    /**
     * Job 名称（用于 Quartz 注册）
     */
    private static final String TASK_NAME = ChargeStationMonthSummaryTaskJobV2.class.getSimpleName();
    /**
     * Job 分组
     */
    private static final String GROUP_NAME = "ChargeStationTask";
    /**
     * Cron：0秒0分，从0点开始每6小时一次（0,6,12,18点）
     */
    private static final String CRON_EXPRESSION = "0 30 0/6 * * ? *";
    /**
     * 站点→平台之间的错峰秒数（保持原有 10s 行为）
     */
    private static final int PLATFORM_DELAY_SECONDS = 10;

    /**
     * 惰性单例（与现有工程风格保持一致）
     */
    private static volatile ChargeStationMonthSummaryTaskJobV2 _this;

    /**
     * 获取单例实例（DCL）
     */
    public static ChargeStationMonthSummaryTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (ChargeStationMonthSummaryTaskJobV2.class) {
                if (_this == null) _this = new ChargeStationMonthSummaryTaskJobV2();
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
                ChargeStationMonthSummaryTaskJobV2.class,
                null,
                TAG
        );

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    // ======================== Quartz Job 入口 ========================

    /**
     * 定时任务入口：
     * - 统一计算时间窗口（避免多处重复计算引起的边界抖动）；
     * - 先站点V2，再平台V2，中间保留 10s 错峰（与现状一致）。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        // 统一计算本次统计窗口：上月月初 -> 本月月初
        final long startTime = TimeUtil.getMonthBegin00(-1);
        final long endTime = TimeUtil.getMonthBegin00();

        // V2：站点月汇总
        ChargeStationMonthSummaryV2Entity.getInstance().startSyncTask(startTime, endTime, true);

        // 错峰 10s（保持现有行为）：
        ThreadUtil.sleep(ECacheTime.SECOND * PLATFORM_DELAY_SECONDS);

        // V2：平台月汇总
        PlatformMonthSummaryV2Entity.getInstance().startSyncTask(startTime, endTime, true);
    }

    // ======================== 单站复算入口 ========================

    /**
     * 单站指定窗口复算：
     * - 若 start_time/end_time 为 0，则回落到 [上月月初, 本月月初)；
     * - 先触发 V2（站点窗口复算），再兼容性触发 V1（上月、当月）。
     *
     * @param CSId       站点 ID（字符串格式；V1 需 long）
     * @param start_time 统计窗口开始（含），0 表示使用上月月初
     * @param end_time   统计窗口结束（不含），0 表示使用本月月初
     */
    public static void execute(String CSId, long start_time, long end_time) {
        if (start_time == 0) start_time = TimeUtil.getMonthBegin00(-1);
        if (end_time == 0) end_time = TimeUtil.getMonthBegin00();

        // V2：站点窗口复算
        ChargeStationMonthSummaryV2Entity.getInstance().syncData(CSId, start_time, end_time);

        // V1：兼容性复算（上月 / 当月）
        final long csIdLong = Convert.toLong(CSId);
        final long monthMinus1 = TimeUtil.getMonthBegin00(-1);
        final long month0 = TimeUtil.getMonthBegin00();

        ChargeStationMonthlySummaryEntity.getInstance().syncData(csIdLong, monthMinus1);
        ChargeStationMonthlySummaryEntity.getInstance().syncData(csIdLong, month0);
    }
}
