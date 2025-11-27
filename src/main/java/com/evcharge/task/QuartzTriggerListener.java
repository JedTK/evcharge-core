package com.evcharge.task;

import com.xyzs.utils.LogsUtil;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;

/**
 * Quartz触发器监听类
 * <p>
 * 实现了Quartz的TriggerListener接口，用于监听触发器的各种事件，例如触发器触发、触发器未执行、触发器完成等。
 * 开发者可以通过该类对触发器相关的行为进行自定义处理。
 */
public class QuartzTriggerListener implements TriggerListener {
    // 定义日志标签，用于标识日志输出的来源
    private final static String TAG = "Quartz触发器监听";

    /**
     * 返回监听器的名称
     * <p>
     * Quartz框架要求每个TriggerListener都要有一个名称，名称可以用于区分不同的监听器实例。
     *
     * @return 返回监听器的名称
     */
    @Override
    public String getName() {
        return TAG;
    }

    /**
     * 当触发器被触发时调用
     * <p>
     * 该方法在触发器实际触发作业（Job）之前被调用。可以在此方法中记录日志、统计触发次数或执行其他操作。
     *
     * @param trigger 触发器对象，包含触发器的详细信息
     * @param context JobExecutionContext对象，包含触发作业的上下文信息
     */
    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
//        LogsUtil.info(TAG, "[%s:%s] 触发器被触发: 作业名称: %s, 作业组: %s, 开始时间: %s, 结束时间: %s 作业数据: %s",
//                trigger.getKey().getGroup(),
//                trigger.getKey().getName(),
//                context.getJobDetail().getKey().getName(),
//                context.getJobDetail().getKey().getGroup(),
//                trigger.getStartTime(),
//                trigger.getEndTime(),
//                context.getMergedJobDataMap());
    }

    /**
     * 决定是否否决触发器执行
     * <p>
     * Quartz框架在触发器触发作业之前会调用此方法。返回true表示否决此次作业执行，返回false则继续执行作业。
     * 通过该方法可以实现条件触发、动态控制作业是否执行等功能。
     *
     * @param trigger 触发器对象
     * @param context JobExecutionContext对象，包含触发作业的上下文信息
     * @return 是否否决触发器执行。返回true表示否决作业执行，false表示允许作业执行
     */
    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
//        LogsUtil.info(TAG, "[%s] 触发器被否决...", context.getJobDetail().getKey());
        return false; // 默认返回false，允许作业执行
    }

    /**
     * 当触发器未按计划执行时调用
     * <p>
     * Quartz框架会在触发器错过执行时间（misfire）时调用此方法。例如，当调度器停止或者作业执行时间超过计划时间时，会触发该回调。
     * 可以在此方法中记录错过的触发信息或进行补偿操作。
     *
     * @param trigger 触发器对象，包含触发器的详细信息
     */
    @Override
    public void triggerMisfired(Trigger trigger) {
        LogsUtil.info(TAG, "[%s:%s] 触发器未执行... %s"
                , trigger.getKey().getGroup()
                , trigger.getKey().getName()
                , (trigger.mayFireAgain() ? "等待下一次触发" : "已完成所有触发"));
    }

    /**
     * 当触发器完成时调用
     * <p>
     * 该方法在触发器完成作业执行后被调用。可以在此方法中执行一些收尾操作，例如清理资源、记录执行结果等。
     *
     * @param trigger                       触发器对象，包含触发器的详细信息
     * @param context                       JobExecutionContext对象，包含触发作业的上下文信息
     * @param completedExecutionInstruction 枚举类型，指示触发器的完成状态（例如正常完成、重新触发等）
     */
    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
//        LogsUtil.info(TAG, "[%s:%s] 触发器触发完成: 状态: %s, 完成指令: %s",
//                context.getJobDetail().getKey().getGroup(),
//                context.getJobDetail().getKey().getName(),
//                (trigger.mayFireAgain() ? "等待下一次触发" : "已完成所有触发"),
//                completedExecutionInstruction);
    }
}