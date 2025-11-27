package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.RSProfit.RSProfitChargeStationMonthSummaryEntity;
import com.evcharge.entity.RSProfit.RSProfitChargeStationSummaryEntity;
import com.evcharge.entity.RSProfit.RSProfitConfigEntity;
import com.evcharge.entity.RSProfit.RSProfitMonthSummaryEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.RSProfitXRocketMQConsumerV2;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

import java.util.List;
import java.util.Map;


/**
 * 分润系统 收益汇总任务
 */
public class RSProfitTaskSummaryTaskJobV2 implements Job {
    protected final static String TAG = "分润收益月汇总";
    private final static String mTaskName = RSProfitTaskSummaryTaskJobV2.class.getSimpleName();
    private final static String mGroupName = "RSProfit";

    private static RSProfitTaskSummaryTaskJobV2 _this;

    public static RSProfitTaskSummaryTaskJobV2 getInstance() {
        if (_this == null) _this = new RSProfitTaskSummaryTaskJobV2();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public void init() {
        String cron = "0 55 1/8 * * ? *";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , RSProfitTaskSummaryTaskJobV2.class
                , null
                , TAG);

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
                    .field("channel_phone")
                    .page(page, limit)
                    .group("channel_phone")
                    .select();
            if (list == null || list.isEmpty()) break;

            page++;
            // 遍历所有充电站，为每个充电站添加监控任务
            for (Map<String, Object> nd : list) {
                String channel_phone = MapUtil.getString(nd, "channel_phone");

//                LogsUtil.info(TAG, "[%s] 正在执行任务中...", group, key, channel_phone);

//                execute(channel_phone);

                // region 使用RocketMQ进行削峰
                JSONObject rocketMQData = new JSONObject();
                rocketMQData.put("channel_phone", channel_phone);
                XRocketMQ.getGlobal().pushOneway(RSProfitXRocketMQConsumerV2.TOPIC, "ChannelSummaryTask", rocketMQData);
                // endregion
            }
        }
    }

    public static void execute(String channel_phone) {
        // 获得上个月时间：因为电费等数据都是上个月才产生的
        long lastMonthTimestamp = TimeUtil.getMonthBegin00(-1);
        //执行分润系统月统计数据
        RSProfitMonthSummaryEntity.getInstance().syncTaskJob(channel_phone, lastMonthTimestamp);
        //执行分润系统充电站月统计数据
        RSProfitChargeStationMonthSummaryEntity.getInstance().startSync(channel_phone, lastMonthTimestamp);

        // 获得当月时间
        long monthTimestamp = TimeUtil.getMonthBegin00();
        //执行分润系统月统计数据
        RSProfitMonthSummaryEntity.getInstance().syncTaskJob(channel_phone, monthTimestamp);
        //执行分润系统充电站月统计数据
        RSProfitChargeStationMonthSummaryEntity.getInstance().startSync(channel_phone, monthTimestamp);

        //执行分润系统充电站统计数据
        RSProfitChargeStationSummaryEntity.getInstance().startSync(channel_phone);
    }
}
