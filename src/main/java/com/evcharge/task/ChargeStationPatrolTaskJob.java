package com.evcharge.task;

import com.evcharge.entity.megadata.MDStreetLogsEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 充电桩巡检任务
 */
@Deprecated
public class ChargeStationPatrolTaskJob implements Job {
    protected final static String TAG = "充电桩模拟巡检";
    private final String mGroupName = this.getClass().getSimpleName();

    private static ChargeStationPatrolTaskJob _this;

    public static ChargeStationPatrolTaskJob getInstance() {
        if (_this == null) _this = new ChargeStationPatrolTaskJob();
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public SyncResult add(String CSId) {
        if (!StringUtils.hasLength(CSId)) return new SyncResult(1, "");

        int h = common.randomInt(9, 20);
        int m = common.randomInt(1, 59);
        int s = common.randomInt(1, 59);
        String cron = String.format("%s %s %s ? * 3,6 *", s, m, h);
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing()
                ).build();

        //参数
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("CSId", CSId);

        //添加任务到调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(CSId, mGroupName, trigger, this.getClass(), jobDataMap);
        LogsUtil.info(TAG, "\033[1;91m %s \033[0m", r.msg);
        return r;
    }

    /**
     * 移除监控任务
     *
     * @param CSId      充电桩ID
     * @param delReason 停止原因
     */
    public void remove(String CSId, String delReason) {
        try {
            QuartzSchedulerManager.getInstance().del(CSId, mGroupName);
            LogsUtil.info(TAG, "\033[1;91m 删除任务：CSId=%s 原因=%s \033[0m", CSId, delReason);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "删除任务发生错误");
        }
    }

    /**
     * 手动触发一个任务
     *
     * @param CSId 订单号
     */
    public SyncResult trigger(String CSId) {
        try {
            //参数
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("CSId", CSId);

            QuartzSchedulerManager.getInstance().triggerJob(CSId, mGroupName, jobDataMap);

            LogsUtil.info(TAG, "\033[1;91m 触发任务 - CSId = %s \033[0m", CSId);
            return new SyncResult(0, "操作成功");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "手动触发任务生错误 - CSId = %s", CSId);
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 恢复监控任务
     */
    public void resume() {
        ThreadUtil.getInstance().execute(String.format("恢复[%s]", TAG), () -> {
            ThreadUtil.sleep(3000);
            LogsUtil.info(TAG, "\033[1;91m 恢复任务 \033[0m");

            List<ChargeStationEntity> list = ChargeStationEntity.getInstance().where("status", ">", 0).where("isTest", 0).selectList();
            if (list.isEmpty()) return;

            for (ChargeStationEntity chargeStationEntity : list) {
                add(String.format("%s", chargeStationEntity.id));
            }
        });
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            String group = jobExecutionContext.getJobDetail().getKey().getGroup();
            String name = jobExecutionContext.getJobDetail().getKey().getName();

            JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();
            long CSId = MapUtil.getLong(data, "CSId");

            LogsUtil.info(TAG, "\033[1;92m 执行中... CSId=%s 下次执行：%s\033[0m", CSId, TimeUtil.toTimeString(jobExecutionContext.getNextFireTime()));

            SyncResult r = run(CSId);
            if (r.code == 910) {
                QuartzSchedulerManager.getInstance().del(name, group);
                LogsUtil.error(TAG, "%s 停止执行... JobDataMap: %s", r.msg, MapUtil.toJSONString(data));
                return;
            }
            if (r.code != 0 && r.code != 1) {
                LogsUtil.error(TAG, "执行期间发生错误: %s  JobDataMap: %s", r.msg, MapUtil.toJSONString(data));
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "执行期间发生错误");
        }
    }

    public SyncResult run(long CSId) {
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().findEntity(CSId);
        if (chargeStationEntity == null) {
            return new SyncResult(910, "不存在充电桩");
        }
        if (chargeStationEntity.isTest == 1) {
            return new SyncResult(3, "测试充电桩不执行");
        }
        if (!StringUtils.hasLength(chargeStationEntity.street_code)) return new SyncResult(2, "街道代码为空");

        MDStreetLogsEntity logsEntity = new MDStreetLogsEntity();
        logsEntity.street_code = chargeStationEntity.street_code;
        logsEntity.cs_code = String.format("%s", CSId);
        logsEntity.content = chargeStationEntity.name;
        logsEntity.log_type = 101;//日志类型：10-危险通知，11-警告通知，101-巡检，102-断点，103-上电，104-监控离线，105-监控上线
        logsEntity.create_time = TimeUtil.getTimestamp();

        if (logsEntity.insert() > 0) {
            return new SyncResult(0, "");
        }
        return new SyncResult(1, "");
    }
}
