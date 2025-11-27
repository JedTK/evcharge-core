package com.evcharge.task;

import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.quartz.DisallowConcurrentExecution; // 防并发执行
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 充电订单监控任务
 * <p>
 * 职责：周期性检查单个充电订单的运行状态（进行中/完成/异常），
 * 在达到规则边界（超预设时长、异常次数过多等）时，执行软结算或移除监控。
 */
@DisallowConcurrentExecution // 防止同一任务（同一 OrderSN）发生并发重入
public class ChargeOrderTaskJob implements Job {

    // ==================== 常量区（统一调参） ====================
    /**
     * 日志 TAG
     */
    protected static final String TAG = "充电订单监控任务";

    /**
     * Quartz 任务分组名（默认使用类名）
     */
    private static final String GROUP = ChargeOrderTaskJob.class.getSimpleName();

    /**
     * 首次调度延迟（秒）
     */
    private static final int START_DELAY_SECONDS = 10;

    /**
     * 调度间隔（分钟）
     */
    private static final int INTERVAL_MINUTES = 20;

    /**
     * 重复次数（执行次数-1，SimpleSchedule 语义），24 次 * 20 分钟 ≈ 8 小时
     */
    private static final int REPEAT_COUNT = 24;

    /**
     * 充电开始后“忽略检查”的静默窗口（分钟）——避免刚启动的短期波动
     */
    private static final int IGNORE_INITIAL_MINUTES = 10;

    /**
     * 超过预设 endTime 后允许的宽限期（分钟）——避免边界条件过早判停
     */
    private static final int GRACE_MINUTES = 30;

    /**
     * 单任务最大允许错误累计次数，超过后自动移除任务
     */
    private static final int MAX_ERROR_COUNT = 4;

    // ==================== 单例（原样保留，做了可见性修饰） ====================
    private static volatile ChargeOrderTaskJob INSTANCE;

    public static ChargeOrderTaskJob getInstance() {
        if (INSTANCE == null) {
            synchronized (ChargeOrderTaskJob.class) {
                if (INSTANCE == null) INSTANCE = new ChargeOrderTaskJob();
            }
        }
        return INSTANCE;
    }

    // ==================== 调度新增/移除/触发/恢复 ====================

    /**
     * 添加一个监控任务
     *
     * @param orderSN 订单号
     */
    public SyncResult add(String orderSN) {
        if (!StringUtils.hasLength(orderSN)) {
            return new SyncResult(1, "OrderSN为空");
        }

        // 避免重复添加
        if (QuartzSchedulerManager.getInstance().checkExists(orderSN, GROUP)) {
            return new SyncResult(0, "任务已存在");
        }

        // 计算首次触发时间：当前 + 10s
        Date startAt = TimeUtil.toDate(TimeUtil.getTimestamp() + ECacheTime.SECOND * START_DELAY_SECONDS);

        // 构建 Trigger（显式 identity，减少潜在冲突）
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(orderSN, GROUP)
                .startAt(startAt)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(INTERVAL_MINUTES)
                        .withRepeatCount(REPEAT_COUNT)
                        // Misfire 策略：错过触发则立刻执行一次
                        .withMisfireHandlingInstructionFireNow()
                )
                .build();

        // Job 参数
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("OrderSN", orderSN);

        // 添加任务到调度器（由自定义管理器封装）
        SyncResult r = QuartzSchedulerManager.getInstance().add(
                orderSN, GROUP, trigger, ChargeOrderTaskJob.class, jobDataMap, TAG
        );

        if (r.code == 0) {
            LogsUtil.info("",
                    "\033[1;94m [%s] - 开始监控：OrderSN=%s 下次执行：%s\033[0m",
                    TAG, orderSN, TimeUtil.toTimeString(trigger.getNextFireTime())
            );
        } else {
            LogsUtil.info("",
                    "\033[1;94m [%s] - 开始监控发生错误：OrderSN=%s 原因=%s \033[0m",
                    TAG, orderSN, r.msg
            );
        }
        return r;
    }

    /**
     * 移除监控任务
     *
     * @param orderSN   充电订单号
     * @param delReason 停止原因
     */
    public void remove(String orderSN, String delReason) {
        try {
            QuartzSchedulerManager.getInstance().del(orderSN, GROUP);
            LogsUtil.info(TAG,
                    "\033[1;91m 删除任务：OrderSN=%s 原因=%s \033[0m", orderSN, delReason
            );
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "删除任务发生错误");
        }
    }

    /**
     * 手动触发一个任务
     *
     * @param orderSN 订单号
     */
    public SyncResult trigger(String orderSN) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("OrderSN", orderSN);

            QuartzSchedulerManager.getInstance().triggerJob(orderSN, GROUP, jobDataMap);
            LogsUtil.info(TAG, "\033[1;91m 触发任务 - OrderSN = %s \033[0m", orderSN);
            return new SyncResult(0, "操作成功");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "手动触发任务生错误 - OrderSN = %s", orderSN);
            return new SyncResult(1, "操作失败");
        }
    }

    /**
     * 恢复监控任务（例如服务重启后，批量恢复近3天内仍在充电中的订单）
     * - 这里使用分页批量拉取，避免一次性装载过多内存
     * - 只恢复 status=1（充电中）的订单
     */
    public void resume() {
        ThreadUtil.getInstance().execute(String.format("[%s]恢复", TAG), () -> {
            int page = 1;
            int limit = 1000;
            while (true) {
                List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                        // 建议只拉必要字段，减少 IO 与反序列化（需实体层支持）
                        .field("id,OrderSN")
                        .where("status", 1)
                        .where("create_time", ">=", TimeUtil.getTime00(-1)) // 近2天
                        .page(page, limit)
                        .select();

                if (list == null || list.isEmpty()) break;
                page++;

                for (Map<String, Object> data : list) {
                    String orderSN = MapUtil.getString(data, "OrderSN");
                    add(orderSN); // 内部已做去重
                }
            }
        });
    }

    // ==================== Quartz 回调入口 ====================

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        // 使用 mergedJobDataMap：支持 Trigger/Job 的数据合并，兼容后续临时参数覆盖场景
        JobDataMap data = ctx.getMergedJobDataMap();

        String orderSN = MapUtil.getString(data, "OrderSN");
        LogsUtil.info(TAG, "\033[1;94m 执行任务：OrderSN=%s fireTime=%s \033[0m",
                orderSN, TimeUtil.toTimeString(ctx.getFireTime())
        );

        if (!StringUtils.hasLength(orderSN)) {
            // 即便为空也调用 remove，保证调度清理
            remove(orderSN, String.format("JobData中订单号为空 [%s.%s]", GROUP, orderSN));
            return;
        }

        SyncResult r = run(orderSN);

        if (r.code != 0) {
            // 错误累计计数：达到上限则移除任务，防止异常任务占用资源
            String key = errorCountKey(orderSN);
            int errorCount = DataService.getMainCache().getInt(key, 0) + 1;
            DataService.getMainCache().set(key, errorCount, ECacheTime.DAY);

            if (errorCount > MAX_ERROR_COUNT) {
                remove(orderSN, "任务执行期间发生错误次数过多");
            }
        } else {
            // 成功时可选清理错误计数（看你是否希望“隔离”错误历史）
            // DataService.getMainCache().del(errorCountKey(orderSN));
        }
    }

    // ==================== 业务核心检查逻辑 ====================

    /**
     * 更新日志：2024-02-18
     * 检查订单是否超过预设订单，如果超过则强制结束
     *
     * @param orderSN 订单号
     * @return 同步结果
     */
    private SyncResult run(String orderSN) {
        try {
            // 查询充电订单。建议仅选取必要字段，降低 DB 与对象构建成本：
            // .field("id,status,startTime,endTime,stopReasonCode,stopReasonText")
            // 需要实体层支持 .field(...)，若暂不支持可保留现状。
            ChargeOrderEntity order = ChargeOrderEntity.getInstance()
                    .where("OrderSN", orderSN)
                    .findEntity();

            // 1) 订单不存在（或被删除）
            if (order == null || order.id == 0) {
                return new SyncResult(10, "查询不到充电订单");
            }

            // 2) 已完成：移除监控
            if (order.status == 2) {
                remove(orderSN, "充电完成，已经进行了结算");
                return new SyncResult(0, "");
            }

            // 3) 异常中：超过10分钟或已有停止原因 => 移除监控
            if (order.status == -1) {
                long now = TimeUtil.getTimestamp();
                boolean overtime = now > order.startTime + IGNORE_INITIAL_MINUTES * ECacheTime.MINUTE;
                boolean hasReason = order.stopReasonCode != 0;
                if (overtime || hasReason) {
                    remove(orderSN, "充电订单发生错误，自动停止监控，原因：" + order.stopReasonText);
                }
                return new SyncResult(3, "充电订单发生错误，原因：" + order.stopReasonText);
            }

            // 4) 充电中
            if (order.status == 1) {
                long now = TimeUtil.getTimestamp();

                // 4.1 充电开始 10 分钟内视为稳定期，不做过多动作
                if (now < order.startTime + IGNORE_INITIAL_MINUTES * ECacheTime.MINUTE) {
                    return new SyncResult(0, "充电继续");
                }

                // 4.2 未超过“预设结束时间 + 30 分钟宽限期” => 继续
                if (now < order.endTime + GRACE_MINUTES * ECacheTime.MINUTE) {
                    return new SyncResult(0, "充电继续");
                }

                // 4.3 已超过预设时长 + 宽限期，仍处于充电中 => 触发软结算
                ChargeOrderEntity.getInstance().tryChargeFinish(order, 7, "充电监控自停");
            }

            return new SyncResult(0, "充电继续");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "执行期间发生错误 订单：%s", orderSN);
            return new SyncResult(1, "执行错误");
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 统一构造错误计数缓存 Key，避免到处拼接
     */
    private static String errorCountKey(String orderSN) {
        return String.format("Task:%s:ErrorCount:%s:%s", common.getLocalIPv4(), GROUP, orderSN);
    }
}
