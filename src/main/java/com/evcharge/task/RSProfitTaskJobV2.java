package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.RSProfit.RSProfitConfigEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.RSProfitXRocketMQConsumerV2;
import com.evcharge.service.RSProfit.RSProfitService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

import java.util.List;
import java.util.Map;

/**
 * 分润系统 作业进程
 * <p>
 * 此类用于管理和执行分润系统的作业任务，通过 Quartz Scheduler 定时调度，获取电费数据，
 * 并利用并发限制器确保任务在多线程环境下的安全性和可靠性。
 */
public class RSProfitTaskJobV2 implements Job {
    protected final static String TAG = "分润收益";
    private final static String mTaskName = RSProfitTaskJobV2.class.getSimpleName();
    private final static String mGroupName = "RSProfit";

    private static RSProfitTaskJobV2 _this;

    /**
     * 获取 RSProfitTaskJob 单例
     *
     * @return RSProfitTaskJob 实例
     */
    public static RSProfitTaskJobV2 getInstance() {
        if (_this == null) _this = new RSProfitTaskJobV2();
        return _this;
    }

    /**
     * 添加一个监控任务
     * <p>
     * 根据 cs_id 创建一个调度任务，并利用并发限制器确保任务的唯一性。
     * 如果任务已存在，则不进行重复添加。任务添加成功后，更新状态为 "启动中"。
     */
    public void init() {
        String cron = "0 45 1/3 * * ? *";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();

        // 添加任务到 Quartz 调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , RSProfitTaskJobV2.class
                , null
                , TAG
        );

        // 记录添加任务的结果日志
        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    /**
     * Quartz 调度器的执行方法
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", group, key);

        int page = 1;
        int limit = 100;

        while (true) {
            List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                    .field("cs_id,status")
                    .alias("rsc")
                    .whereIn("status", "1,2")
                    .page(page, limit)
                    .select();
            if (list == null || list.isEmpty()) break;

            page++;
            // 遍历所有充电站，为每个充电站添加监控任务
            for (Map<String, Object> nd : list) {
                String cs_id = MapUtil.getString(nd, "cs_id");
                int status = MapUtil.getInt(nd, "status"); // -1-停止，0-待确认，1-待启动，2-启动中

                switch (status) {
                    case -1:
                        break;
                    case 0:
                        LogsUtil.info(TAG, "%s - 待确认...", cs_id);
                        break;
                    case 1:
                    case 2:
//                        LogsUtil.info(TAG, "%s - 执行中...", cs_id);

//                        // 同步上个月的数据
//                        RSProfitService.getInstance().syncData(cs_id, TimeUtil.getMonthBegin00(-1));
//
//                        // 同步当前月的数据
//                        RSProfitService.getInstance().syncData(cs_id, TimeUtil.getMonthBegin00());

                        // region 使用RocketMQ进行削峰
                        JSONObject rocketMQData = new JSONObject();
                        rocketMQData.put("CSId", cs_id);
                        XRocketMQ.getGlobal().pushOneway(RSProfitXRocketMQConsumerV2.TOPIC, "ChargeStation", rocketMQData);
                        // endregion

                        break;
                }
            }
        }
    }

    public static void execute(String cs_id) {
        // 同步上个月的数据
        RSProfitService.getInstance().syncData(cs_id, TimeUtil.getMonthBegin00(-1));

        // 同步当前月的数据
        RSProfitService.getInstance().syncData(cs_id, TimeUtil.getMonthBegin00());
    }
}