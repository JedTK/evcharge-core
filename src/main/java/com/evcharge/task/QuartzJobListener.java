package com.evcharge.task;

import com.xyzs.utils.LogsUtil;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 * Quartz工作监听器类实现了JobListener接口。
 * <p>
 * 此类用于监听Quartz框架中任务的生命周期事件，并对以下三种事件进行处理：
 * 1. 任务即将执行。
 * 2. 任务被阻止执行。
 * 3. 任务执行完成（无论成功或失败）。
 * <p>
 * 日志工具LogsUtil被用于记录任务相关的日志信息，帮助调试和监控任务的执行。
 */
public class QuartzJobListener implements JobListener {

    /**
     * 定义监听器名称，用于标识当前JobListener。
     * Quartz框架会通过getName方法获取该名称，用于管理监听器。
     */
    private final static String TAG = "Quartz工作监听";

    /**
     * 获取监听器的名称。
     *
     * @return 监听器名称（字符串），用于标识当前监听器。
     */
    @Override
    public String getName() {
        return TAG;
    }

    /**
     * 当任务即将执行时触发此方法。
     * 该方法会在任务执行之前被调用，可以用于记录日志或初始化资源。
     *
     * @param context 任务执行上下文，包含任务的详细信息。
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
//        LogsUtil.info(TAG, "[%s:%s] 任务即将执行...\r\n" +
//                        "描述: %s\r\n" +
//                        "触发器: %s:%s\r\n"
//                , context.getJobDetail().getKey().getGroup()
//                , context.getJobDetail().getKey().getName()
//                , context.getJobDetail().getDescription()
//                , context.getTrigger().getKey().getGroup()
//                , context.getTrigger().getKey().getName()
//        );
    }

    /**
     * 当任务被阻止执行时触发此方法。
     * 此方法通常在触发器被另一个监听器或条件阻止后被调用。
     *
     * @param context 任务执行上下文，包含任务的详细信息。
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        LogsUtil.info(TAG, "[%s:%s] 任务被阻止执行...\r\n" +
                        "描述: %s\r\n" +
                        "触发器: %s\r\n"
                , context.getJobDetail().getKey().getGroup()
                , context.getJobDetail().getKey().getName()
                , context.getJobDetail().getDescription()
                , context.getTrigger().getKey()
        );
    }

    /**
     * 当任务执行完成后触发此方法。
     * 此方法会在任务执行结束时被调用，无论任务是成功还是失败。
     * 如果任务执行过程中抛出了异常，可以通过参数e获取异常信息。
     *
     * @param context 任务执行上下文，包含任务的详细信息。
     * @param e       如果任务执行过程中发生了异常，则e不为null，否则为null。
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {
        // 检查任务是否执行过程中发生异常
        if (e != null) {
            // 如果发生异常，记录错误日志并返回
            LogsUtil.error(e, TAG, "[%s:%s] 任务执行失败 %s %s"
                    , context.getJobDetail().getKey().getGroup()
                    , context.getJobDetail().getKey().getName()
                    , Thread.currentThread().getName()
                    , e.getMessage()
            );
        }

//        JSONObject params = new JSONObject();
//        if (!context.getMergedJobDataMap().isEmpty()) {
//            params = new JSONObject(context.getMergedJobDataMap());
//        }
//
//        // 如果任务成功完成，记录成功日志
//        LogsUtil.info(TAG, "[%s:%s] 任务执行成功！\r\n" +
//                        "耗时：%s" +
//                        "参数：%s"
//                , context.getJobDetail().getKey().getGroup()
//                , context.getJobDetail().getKey().getName()
//                , context.getJobRunTime()
//                , params.toJSONString()
//        );
    }
}