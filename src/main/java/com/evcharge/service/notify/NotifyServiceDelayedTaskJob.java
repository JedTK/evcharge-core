package com.evcharge.service.notify;

import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.quartz.*;

import java.util.Date;

/**
 * 通知服务延迟任务类，负责处理通知发送失败后的延迟重试任务。
 * <p>
 * 功能：
 * - 基于 Quartz 实现通知的延迟重发任务
 * - 支持添加新的延迟任务（消息重发）到调度器
 * - 通过 Quartz Scheduler 定时触发重发任务
 * <p>
 * 特点：
 * - 单例模式，确保只创建一个实例
 * - 使用双重检查锁定（Double-checked locking）确保多线程环境下的安全性
 */
public class NotifyServiceDelayedTaskJob implements Job {

    private final static String TAG = "通知延迟任务"; // 日志标识

    /**
     * 单例模式，确保该类只有一个实例，使用 volatile 确保可见性。
     */
    private static volatile NotifyServiceDelayedTaskJob _this;

    /**
     * 获取 NotifyServiceDelayedTaskJob 的单例实例。
     *
     * @return NotifyServiceDelayedTaskJob 的实例
     */
    public static NotifyServiceDelayedTaskJob getInstance() {
        if (_this == null) {
            // 双重检查锁定（Double-checked locking）以保证线程安全的单例模式
            synchronized (NotifyServiceDelayedTaskJob.class) {
                if (_this == null) {
                    _this = new NotifyServiceDelayedTaskJob();
                }
            }
        }
        return _this;
    }

    /**
     * 添加延迟任务 - 将通知重发任务添加到 Quartz 调度器中，在指定延迟时间后执行任务。
     *
     * @param message_id       消息的唯一标识符
     * @param retry_timeout_ms 延迟时间（毫秒）
     * @return SyncResult 添加任务的结果，包含状态码和描述信息
     */
    public SyncResult add(String message_id, int retry_timeout_ms) {
        try {
            // 创建一个 Quartz 触发器，设置延迟执行时间为当前时间 + retry_timeout_ms，且只执行一次
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(new Date(TimeUtil.getTimestamp() + retry_timeout_ms)) // 延迟时间
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0)) // 任务只执行一次
                    .build();

            // 创建 JobDataMap 用于存储任务执行时所需的参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("message_id", message_id); // 将消息 ID 传递给任务

            // 将任务添加到 Quartz 调度器中，任务将在指定的延迟时间后执行
            SyncResult r = QuartzSchedulerManager.getInstance().add(message_id
                    , this.getClass().getSimpleName() // 使用类名作为任务标识符
                    , trigger
                    , this.getClass() // 任务执行类
                    , jobDataMap);
            if (r.code == 0) {
                // 如果任务添加成功，记录成功日志
                LogsUtil.info(TAG, "[%s] 消息入队成功", message_id);
            }
            return r; // 返回任务添加结果
        } catch (Exception e) {
            // 捕获异常并记录错误日志
            LogsUtil.error(e, TAG, "添加任务发生错误");
        }
        // 如果任务添加失败，返回失败结果
        return new SyncResult(1, "添加失败");
    }

    /**
     * 任务执行方法 - Quartz 调度器在任务触发时调用该方法，执行延迟的通知重发操作。
     *
     * @param jobExecutionContext Quartz 提供的任务上下文，包含任务执行时的参数和环境信息
     * @throws JobExecutionException 如果任务执行过程中发生异常
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 从任务上下文中获取 JobDataMap，读取任务参数
        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
        String message_id = MapUtil.getString(data, "message_id"); // 获取消息 ID

        // 调用 NotifyService 执行重发逻辑
        NotifyService.getInstance().re_push(message_id);
    }
}