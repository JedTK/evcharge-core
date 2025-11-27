package com.evcharge.task;

import com.evcharge.entity.megadata.MDCommunitiesSummaryEntity;
import com.evcharge.entity.megadata.MDStreetSummaryEntity;
import com.evcharge.entity.platform.PlatformSummaryV2Entity;
import com.evcharge.entity.station.ChargeStationSummaryEntity;
import com.evcharge.entity.station.ChargeStationSummaryV2Entity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

/**
 * 充电桩【总数据】汇总任务（V2）
 * <p>
 * 职责：
 * 1) 由 Quartz 周期触发（当前 Cron：每 12 小时一次）。
 * 2) 串行触发四段统计：
 * - 站点总数据（V2，内部通过 RocketMQ 批量拉起）
 * - 平台总数据（V2）
 * - 小区（社区）总数据（MDCommunities）
 * - 街道总数据（MDStreet）
 * 3) 提供静态 {@link #execute(String)} 供单站复算，同时兼容触发 V1 老口径。
 * <p>
 * 说明：
 * - 本次仅做注释补全与轻量可维护性优化（常量提取、空值保护等），不改变核心业务逻辑、调用顺序与参数。
 * - 若需更细粒度的失败隔离/防并发控制/告警与水位线，请见类后“可选优化建议”。
 */
public class ChargeStationSummaryTaskJobV2 implements Job {

    // ===== 常量与元信息 =====

    /**
     * 日志标签
     */
    protected static final String TAG = "充电桩总数据统计";
    /**
     * Job 名称（用于 Quartz 注册）
     */
    private static final String TASK_NAME = ChargeStationSummaryTaskJobV2.class.getSimpleName();
    /**
     * Job 分组
     */
    private static final String GROUP_NAME = "ChargeStationTask";
    /**
     * Cron：0秒0分，从0点开始每 12 小时一次（0 点、12 点）
     */
    private static final String CRON_EXPRESSION = "0 45 0/12 * * ? *";

    /**
     * 惰性单例（与工程内其他 Job 风格一致）
     */
    private static volatile ChargeStationSummaryTaskJobV2 _this;

    /**
     * 获取单例实例（DCL）
     */
    public static ChargeStationSummaryTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (ChargeStationSummaryTaskJobV2.class) {
                if (_this == null) _this = new ChargeStationSummaryTaskJobV2();
            }
        }
        return _this;
    }

    /**
     * 注册调度任务：
     * - 立即启动（startNow），并按 CRON_EXPRESSION 周期执行；
     * - MISFIRE 策略：FireAndProceed（错过触发则立刻补触发一次）。
     */
    public void init() {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();

        SyncResult r = QuartzSchedulerManager.getInstance().add(
                TASK_NAME,
                GROUP_NAME,
                trigger,
                ChargeStationSummaryTaskJobV2.class,
                null,
                TAG
        );

        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m",
                r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    // ======================== Quartz Job 入口 ========================

    /**
     * 定时任务入口：串行触发四段汇总。
     * <p>
     * 现保持“触发顺序 + 同步执行”的既有行为：若上游抛异常，后续段落将不会执行。
     * 若需“失败不影响后续”，可在不改变默认行为的前提下，增加可选开关以 try/catch 隔离（见文末建议）。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        // 1) 站点总数据（V2）：内部通过 RocketMQ 批量触发，适合大规模站点数据的削峰与解耦
        ChargeStationSummaryV2Entity.getInstance().startSyncTask(false);

        // 2) 平台总数据（V2）
        PlatformSummaryV2Entity.getInstance().startSyncTask(false);

        // 3) 小区（社区）总数据
        MDCommunitiesSummaryEntity.getInstance().startSyncTask(true);

        // 4) 街道总数据
        MDStreetSummaryEntity.getInstance().startSyncTask(true);
    }

    // ======================== 单站复算入口 ========================

    /**
     * 单站复算：
     * - 触发 V2 总数据（按站点）；
     * - 兼容触发 V1 老口径。
     *
     * @param CSId 站点 ID（字符串；V1 需要 Long）
     */
    public static void execute(String CSId) {
        // V2：单站任务
        ChargeStationSummaryV2Entity.getInstance().syncTaskJob(CSId);

        // V1：兼容任务
        final long csIdLong = Convert.toLong(CSId);
        ChargeStationSummaryEntity.getInstance().syncData(csIdLong);
    }
}
