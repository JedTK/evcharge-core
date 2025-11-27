package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
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
 * 平台灯箱控制
 * 夏天：
 * 亮灯时间：19:00-2:00，其他为关灯时间
 * 冬天：
 * 亮灯时间：18:00-2:00，其他为关灯时间
 */
@Deprecated
public class PlatformLightboxTaskJob implements Job {
    protected final static String TAG = "平台灯箱任务";
    private final String mGroupName = this.getClass().getSimpleName();
    private static volatile PlatformLightboxTaskJob _this;

    public static PlatformLightboxTaskJob getInstance() {
        if (_this == null) {
            synchronized (PlatformLightboxTaskJob.class) {
                if (_this == null) {
                    _this = new PlatformLightboxTaskJob();
                }
            }
        }
        return _this;
    }

    /**
     * 添加一个监控任务
     */
    public SyncResult add(String serialNumber) {
        // 限制添加频率
        ExecutionThrottle.getInstance().run(data -> {
            if (!StringUtils.hasLength(serialNumber)) {
                return new SyncResult(2, "无效设备序列号");
            }

            // 亮灯数据
            JobDataMap lightOnData = new JobDataMap();
            lightOnData.put("serialNumber", serialNumber);
            lightOnData.put("switchStatus", true);

            // 关灯数据
            JobDataMap lightOffData = new JobDataMap();
            lightOffData.put("serialNumber", serialNumber);
            lightOffData.put("switchStatus", false);


            //region 春夏秋冬设置时间
//        // 春季：3、4、5月：18:00亮灯，2:00关灯
//        addTaskList("春季亮灯", "spring_on", "0 0 18 ? 3,4,5 * *", serialNumber, lightOnData);
//        addTaskList("春季关灯", "spring_off", "0 0 2 ? 3,4,5 * *", serialNumber, lightOnData);
//
//        // 夏季：6、7、8月：19:00亮灯，2:00关灯
//        addTaskList("夏季亮灯", "summer_on", "0 0 19 ? 6,7,8 * *", serialNumber, lightOnData);
//        addTaskList("夏季关灯", "summer_off", "0 0 2 ? 6,7,8 * *", serialNumber, lightOnData);
//
//        // 秋季：9、10、11月：19:00亮灯，2:00关灯
//        addTaskList("秋季亮灯", "autumn_on", "0 0 19 ? 9,10,11 * *", serialNumber, lightOnData);
//        addTaskList("秋季关灯", "autumn_off", "0 0 2 ? 9,10,11 * *", serialNumber, lightOnData);
//
//        // 冬季：12、1、2月：18:00亮灯，2:00关灯
//        addTaskList("冬季亮灯", "winter_on", "0 0 18 ? 12,1,2 * *", serialNumber, lightOnData);
//        addTaskList("冬季关灯", "winter_off", "0 0 2 ? 12,1,2 * *", serialNumber, lightOnData);
            //endregion

            // 18:00亮灯，2:00关灯
            addTaskList("spring_winter_on", "0 0 18,19 * 1,2,3,4,5,12 ?", serialNumber, lightOnData);
            addTaskList("summer_autumn_on", "0 0 18,19 * 6,7,8,9,10,11 ?", serialNumber, lightOnData);
            addTaskList("off", "0 0 2,6 * * ?", serialNumber, lightOffData);

            return new SyncResult(0, "");
        }, String.format("%sAdd_%s", mGroupName, serialNumber), ECacheTime.DAY, null);
        return new SyncResult(0, "");
    }

    /**
     * 添加任务列表
     *
     * @param taskName       任务名，区分任务
     * @param serialNumber   设备序列号
     * @param cronExpression cron表达式
     * @param dataMap        任务传递数据
     * @return SyncResult 任务添加结果
     */
    private SyncResult addTaskList(String taskName, String cronExpression, String serialNumber, JobDataMap dataMap) {
        taskName = String.format("%s_%s", serialNumber, taskName);

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed()
                ).build();

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
     * 移除监控任务
     *
     * @param serialNumber 设备序列号
     * @param delReason    停止原因
     */
    public void remove(String serialNumber, String delReason) {
        QuartzSchedulerManager.getInstance().del(serialNumber, mGroupName);
        LogsUtil.info(TAG, "\033[1;91m [%s] - 删除任务 - %s \033[0m", serialNumber, delReason);
    }

    /**
     * 手动触发一个任务
     *
     * @param serialNumber 设备序列号
     */
    public SyncResult trigger(String serialNumber) {
        LogsUtil.info(TAG, "\033[1;91m [%s] - 触发任务 \033[0m");
        QuartzSchedulerManager.getInstance().triggerJob(serialNumber, mGroupName, null);
        return new SyncResult(0, "操作成功");
    }

    /**
     * 恢复监控任务
     */
    public void resume() {
        ThreadUtil.getInstance().execute(String.format("[%s] - 恢复", TAG), () -> {
            LogsUtil.info(TAG, "\033[1;94m 恢复任务 \033[0m");

            int page = 1;
            int listRows = 1000;
            List<Map<String, Object>> list;
            do {
                list = GeneralDeviceEntity.getInstance()
                        .field("id,serialNumber")
                        .where("spuCode", "smart-switch-c")
                        .where("typeCode", "4GRCS")
                        .page(page, listRows)
                        .select();
                if (list != null && !list.isEmpty()) {
                    list.forEach(data -> {
                        String serialNumber = MapUtil.getString(data, "serialNumber");
                        add(serialNumber);
                    });
                    page++;
                }
            } while (list != null && !list.isEmpty());
        });
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap data = jobExecutionContext.getJobDetail().getJobDataMap();

        String serialNumber = MapUtil.getString(data, "serialNumber");
        boolean switchStatus = MapUtil.getBool(data, "switchStatus");
        LogsUtil.info(TAG, String.format("\033[1;94m [%s] - 执行任务 \033[0m", serialNumber));

        run(serialNumber, switchStatus);
    }

    private SyncResult run(String serialNumber, boolean switchStatus) {
        try {
            //读取配置信息
            JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);
            if (config == null) {
                LogsUtil.warn(TAG, "无法找到设备数据");
                return new SyncResult(2, "无效配置");
            }

            String token = config.getString("token");
            int lightSwitchIndex = 2;//灯箱 - 开关索引

            SkySmartSwitch smartSwitch = new SkySmartSwitch(serialNumber, token);
            SyncResult result = smartSwitch.executeSync(lightSwitchIndex, switchStatus, false);
            if (result.code != 0) {
                LogsUtil.warn(TAG, "[灯箱][%s] 无法操作(%s)，请检查4G远程控制是否断电了", serialNumber, result.msg);
            } else {
                LogsUtil.info(TAG, "[灯箱][%s] %s", serialNumber, switchStatus ? "打开" : "关闭");
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s] - 执行期间发生错误", serialNumber);
        }
        return new SyncResult(1, "执行错误");
    }
}
