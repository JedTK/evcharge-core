package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.task.SmartSwitchTaskEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.libsdk.sky1088.SkySmartSwitch;
import com.evcharge.service.GeneralDevice.GeneralDeviceConfigService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 智能开关任务
 * <p>
 * 此类用于管理和执行智能开关的定时任务。通过集成Quartz框架，可以定时开启或关闭设备的某一条线路。
 */
@Deprecated
public class SmartSwitchTaskJob implements Job {
    protected final static String TAG = "智能开关任务"; // 日志标签，用于标识日志来源
    private final String mGroupName = this.getClass().getSimpleName(); // 任务分组名
    private static volatile SmartSwitchTaskJob _this; // 单例对象
    //    private final static ExecutionThrottle mExecutionThrottle = new ExecutionThrottle(true, "", com.xyzs.cache.ECacheTime.DAY, false);
    private final static ExecutionThrottle mExecutionThrottle = new ExecutionThrottle(1000, com.xyzs.cache.ECacheTime.DAY);

    /**
     * 获取单例实例
     *
     * @return SmartSwitchTaskJob 单例对象
     */
    public static SmartSwitchTaskJob getInstance() {
        if (_this == null) {
            synchronized (SmartSwitchTaskJob.class) {
                if (_this == null) {
                    _this = new SmartSwitchTaskJob();
                }
            }
        }
        return _this;
    }

    /**
     * 添加一个监控任务
     *
     * @param serialNumber 设备序列号
     * @return SyncResult 操作结果
     */
    public SyncResult add(String serialNumber) {
        // 限制添加频率，避免频繁调用
        mExecutionThrottle.run(false, data -> {
            // 校验序列号有效性
            if (!StringUtils.hasLength(serialNumber)) {
                return new SyncResult(2, "无效设备序列号");
            }

            // 查询对应任务信息
            SmartSwitchTaskEntity task = SmartSwitchTaskEntity.getInstance()
                    .where("serialNumber", serialNumber)
                    .findEntity();
            if (task == null || task.id == 0) return new SyncResult(2, "无效任务");
            if (task.status == 0) return new SyncResult(3, "任务已关闭");

            // 定义开启和关闭开关的任务数据
            JobDataMap onData = new JobDataMap();
            onData.put("serialNumber", serialNumber);
            onData.put("switch_index", task.switch_index);
            onData.put("switch_on_off", true);

            JobDataMap offData = new JobDataMap();
            offData.put("serialNumber", serialNumber);
            offData.put("switch_index", task.switch_index);
            offData.put("switch_on_off", false);

            // 添加开启和关闭任务
            add(serialNumber, task.cron_on, onData, "on", "打开开关任务");
            add(serialNumber, task.cron_off, offData, "off", "关闭开关任务");

            return new SyncResult(0, "");
        }, String.format("%sAdd_%s", mGroupName, serialNumber), ECacheTime.HOUR * 6, null);
        return new SyncResult(0, "");
    }

    /**
     * 添加单个任务到Quartz调度器
     *
     * @param serialNumber   设备序列号
     * @param cronExpression cron表达式，用于定义任务触发时间
     * @param dataMap        任务数据
     * @param taskNamePrefix 任务名前缀
     * @param taskText       日志信息
     * @return SyncResult 操作结果
     */
    private SyncResult add(String serialNumber, String cronExpression, JobDataMap dataMap, String taskNamePrefix, String taskText) {
        String taskName = String.format("%s_%s", serialNumber, taskNamePrefix);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed()
                ).build();

        // 添加任务到Quartz调度器
        SyncResult r = QuartzSchedulerManager.getInstance().add(taskName
                , mGroupName
                , trigger
                , this.getClass()
                , dataMap
                , TAG);

        LogsUtil.info(TAG, "\033[1;91m %s \033[0m", r.msg);
        return r;
    }

    /**
     * 恢复所有开启状态的监控任务
     */
    public void resume() {
        LogsUtil.info(TAG, "resume run...");

        mExecutionThrottle.run(data -> {
            ThreadUtil.getInstance().execute(String.format("[%s] - 恢复", TAG), () -> {
                int page = 1;
                int listRows = 100;
                List<Map<String, Object>> list;

                // 分页加载任务数据并恢复任务
                while (true) {
                    list = SmartSwitchTaskEntity.getInstance()
                            .field("id,serialNumber")
                            .where("status", 1)
                            .page(page, listRows)
                            .select();
                    if (list == null || list.isEmpty()) break;
                    LogsUtil.info(TAG, "分页添加任务[%s]", page);
                    page++;
                    list.forEach(nd -> add(MapUtil.getString(nd, "serialNumber")));
                }
            });
            return new SyncResult(0, "");
        }, String.format("%sResume", mGroupName), ECacheTime.DAY);
    }

    /**
     * Quartz调度器触发的任务执行逻辑
     *
     * @param context Quartz上下文，包含任务数据
     * @throws JobExecutionException 执行异常
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String task_name = context.getJobDetail().getKey().getName();
        JobDataMap data = context.getJobDetail().getJobDataMap();

        String serialNumber = MapUtil.getString(data, "serialNumber");
        int switch_index = MapUtil.getInt(data, "switch_index");
        boolean switch_on_off = MapUtil.getBool(data, "switch_on_off");

        // 执行任务逻辑
        run(serialNumber, switch_index, switch_on_off);

        // 恢复任务以确保所有状态正常
        resume();
    }

    /**
     * 具体的任务运行逻辑
     *
     * @param serialNumber  设备序列号
     * @param switch_index  线路索引
     * @param switch_on_off 开关状态（true为开启，false为关闭）
     * @return SyncResult 运行结果
     */
    private SyncResult run(String serialNumber, int switch_index, boolean switch_on_off) {
        try {
            LogsUtil.info(TAG, "[%s] 线路-%s %s 执行中...", serialNumber, switch_index, switch_on_off ? "打开" : "关闭");

            // 读取设备配置信息
            JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);
            if (config == null) {
                LogsUtil.warn(TAG, "无法找到设备数据");
                return new SyncResult(2, "无效配置");
            }

            // 天将军智能开关逻辑
            String token = config.getString("token");
            SkySmartSwitch smartSwitch = new SkySmartSwitch(serialNumber, token);

            // 执行开关操作
            SyncResult result = smartSwitch.executeSync(switch_index, switch_on_off, false);
            if (result.code != 0) {
                LogsUtil.info(TAG, "[%s] 线路-%s %s 失败:%s 请检查远程控制是否断电了"
                        , serialNumber
                        , switch_index
                        , switch_on_off ? "打开" : "关闭"
                );
                return result;
            }
            LogsUtil.info(TAG, "[%s] 线路-%s %s 成功", serialNumber, switch_index, switch_on_off ? "打开" : "关闭");
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s] 线路-%s %s 执行期间发生错误", serialNumber, switch_index, switch_on_off ? "打开" : "关闭");
        }
        return new SyncResult(1, "执行错误");
    }
}