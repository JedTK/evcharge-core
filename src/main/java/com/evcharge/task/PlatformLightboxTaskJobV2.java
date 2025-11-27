package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.libsdk.sky1088.SkySmartSwitch;
import com.evcharge.service.GeneralDevice.GeneralDeviceConfigService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;

import java.util.List;
import java.util.Map;

/**
 * 平台灯箱控制任务类。
 * 该任务的目的是控制平台上的灯箱开关，通过定时任务控制灯箱在指定的时间段亮灯。
 * 亮灯时间：19:00-2:00，其他时间为关灯时间。
 * 任务使用Quartz定时框架执行。
 */
@DisallowConcurrentExecution
public class PlatformLightboxTaskJobV2 implements Job {
    // 日志标签
    private final static String TAG = "平台灯箱任务";
    // 任务名称
    private final static String mTaskName = PlatformLightboxTaskJobV2.class.getSimpleName();
    // 任务组名
    private final static String mGroupName = "Platform";

    // 单例对象
    private static volatile PlatformLightboxTaskJobV2 _this;

    /**
     * 获取单例实例。
     *
     * @return PlatformLightboxTaskJobV2 返回PlatformLightboxTaskJobV2的唯一实例
     */
    public static PlatformLightboxTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (PlatformLightboxTaskJobV2.class) {
                if (_this == null) {
                    _this = new PlatformLightboxTaskJobV2();
                }
            }
        }
        return _this;
    }

    /**
     * 初始化任务，包括添加定时任务：亮灯和关灯任务。
     * 任务执行的时间为：18:00亮灯，2:00关灯。
     *
     * @return SyncResult 返回任务初始化结果
     */
    public SyncResult init() {
        // 创建亮灯数据
        JobDataMap lightOnData = new JobDataMap();
        lightOnData.put("switch_on_off", true);

        // 创建关灯数据
        JobDataMap lightOffData = new JobDataMap();
        lightOffData.put("switch_on_off", false);

        // 添加亮灯和关灯的任务到任务列表
        addTaskList("on", "0 0 18 * * ?", lightOnData);  // 18:00亮灯
        addTaskList("off", "0 0 2 * * ?", lightOffData); // 2:00关灯

        return new SyncResult(0, "");
    }

    /**
     * 添加任务到Quartz调度器中。
     *
     * @param taskName       任务名称
     * @param cronExpression cron表达式，用于定义任务执行的时间
     * @param dataMap        任务执行时携带的参数
     * @return SyncResult 返回任务添加结果
     */
    private SyncResult addTaskList(String taskName, String cronExpression, JobDataMap dataMap) {
        // 设置任务名称格式
        taskName = String.format("%s_%s", mTaskName, taskName);

        // 创建触发器
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                        .withMisfireHandlingInstructionFireAndProceed() // 触发器的错过执行后进行立即执行
                ).build();

        // 添加任务到调度器
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
     * Quartz调度器会调用该方法来执行定时任务。
     * 根据任务的参数，决定是否打开或关闭灯箱。
     *
     * @param context Quartz提供的执行上下文
     * @throws JobExecutionException 任务执行异常
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 获取任务参数数据
        JobDataMap data = context.getJobDetail().getJobDataMap();
        boolean switch_on_off = MapUtil.getBool(data, "switch_on_off");
        execute(switch_on_off);  // 执行具体操作
    }

    /**
     * 根据传入的开关状态执行任务，控制设备开关。
     *
     * @param switch_on_off 是否开启设备（true：开启，false：关闭）
     */
    public void execute(boolean switch_on_off) {
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务", mGroupName, mTaskName);

        // 分页获取设备列表，限制每页最多100条设备
        int page = 1;
        int limit = 100;

        while (true) {
            // 查询平台上符合条件的设备
            List<Map<String, Object>> list = GeneralDeviceEntity.getInstance()
                    .field("id,serialNumber,status")
                    .whereIn("spuCode", new String[]{"smart-switch-c", "smart-switch-k202"})
                    .where("typeCode", "4GRCS")
//                    .where("status", 1)  // 只选择状态为1的设备
                    .page(page, limit)
                    .select();
            if (list == null || list.isEmpty()) {
                LogsUtil.info(TAG, "[%s-%s] - 全部执行完毕", mGroupName, mTaskName);
                break;  // 如果没有设备，退出
            }

            page++;  // 分页查询，增加页码
            for (Map<String, Object> nd : list) {
                String serialNumber = MapUtil.getString(nd, "serialNumber");

                int status = MapUtil.getInt(nd, "status"); // 设备状态，0表示删除，1表示正常
                if (status == 0) continue;  // 如果设备已删除，则跳过该设备
                run(serialNumber, switch_on_off);  // 执行任务，控制设备开关
            }
            ThreadUtil.sleep(500);  // 每次操作后休眠500毫秒
        }
    }

    /**
     * 执行具体的任务逻辑，控制设备的开关状态。
     *
     * @param serialNumber  设备序列号
     * @param switch_on_off 开关状态（true：开启，false：关闭）
     * @return SyncResult 返回任务执行结果
     */
    private SyncResult run(String serialNumber, boolean switch_on_off) {
        try {
            // 获取设备配置信息
            JSONObject config = GeneralDeviceConfigService.getInstance().getJSONObject(serialNumber);
            if (config == null) {
                LogsUtil.warn(TAG, "无法找到设备数据");
                return new SyncResult(2, "无效配置");
            }

            // 获取灯箱开关的索引
            int lightbox_switch_index = JsonUtil.getInt(config, "light_box", -1);
            if (lightbox_switch_index == -1) {
                LogsUtil.info(TAG, "[%s] 线路-%s %s 无法找到灯箱索引"
                        , serialNumber
                        , lightbox_switch_index
                        , switch_on_off ? "打开" : "关闭"
                );
                return new SyncResult(3, "无灯箱配置");
            }

            LogsUtil.info(TAG, "[%s] 线路-%s %s 执行中...", serialNumber, lightbox_switch_index, switch_on_off ? "打开" : "关闭");

            // 读取设备token信息
            String token = config.getString("token");
            if (StringUtil.isEmpty(token)) {
                LogsUtil.info(TAG, "[%s] 无效token", serialNumber);
                return new SyncResult(4, "无效token");
            }
            SkySmartSwitch smartSwitch = new SkySmartSwitch(serialNumber, token);

            // 最大重试次数
            int maxRetries = 1;
            for (int i = 0; i < maxRetries; i++) {
                // 执行开关操作
                SyncResult result = smartSwitch.executeSync(lightbox_switch_index, switch_on_off, false);
                if (result.code == 0) {
                    LogsUtil.info(TAG, "[%s] 线路-%s %s 执行成功", serialNumber, lightbox_switch_index, switch_on_off ? "打开" : "关闭");
                    return result;  // 成功则返回结果
                }
                LogsUtil.warn(TAG, "[%s] 线路-%s %s失败，第%d次重试"
                        , serialNumber
                        , lightbox_switch_index
                        , switch_on_off ? "打开" : "关闭", i + 1);
                ThreadUtil.sleep(1000);  // 重试前休眠1秒
            }

            // 超过最大重试次数时，返回失败结果
            LogsUtil.error(TAG, "[%s] 线路-%s %s失败，已达最大重试次数", serialNumber, lightbox_switch_index, switch_on_off ? "打开" : "关闭");
            return new SyncResult(5, "开关操作失败");
        } catch (Exception e) {
            // 发生异常时的处理
            LogsUtil.error(e, TAG, "[%s] %s 执行期间发生错误", serialNumber, switch_on_off ? "打开" : "关闭");
        }
        return new SyncResult(1, "执行错误");
    }
}