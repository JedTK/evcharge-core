package com.evcharge.task;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceUnitEntity;
import com.evcharge.entity.device.GeneralDeviceConfigEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.libsdk.ys7.YSSDK;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 萤石云监控警报任务类
 * <p>
 * 主要用于萤石云摄像头的警报处理和存储状态监控。
 * 该任务会定期检查设备的存储状态，如发现异常情况（存储介质错误、未格式化等），则推送警报信息。
 */
public class YSNVRWarnTaskJobV2 implements Job {
    // 日志标识文本，方便识别该任务的日志信息
    protected final static String TAG = "萤石云监控警报";

    private final static String mTaskName = YSNVRWarnTaskJobV2.class.getSimpleName();
    private final static String mGroupName = "YS_NVR";

    // 单例模式的实例，确保任务管理器只有一个实例运行
    private static volatile YSNVRWarnTaskJobV2 _this;

    /**
     * 获取单例对象
     * <p>
     * 双重检查锁（Double-Checked Locking）用于确保线程安全，同时提高性能
     */
    public static YSNVRWarnTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (YSNVRWarnTaskJobV2.class) {
                if (_this == null) _this = new YSNVRWarnTaskJobV2();
            }
        }
        return _this;
    }

    /**
     * 添加萤石云监控警报任务
     */
    public void init() {
        // 定时任务的 cron 表达式，任务将在每天 9:00、14:00 和 19:00 执行
        String cron = "0 0 9,14,19 * * ? *";
        // 创建 Cron 触发器，按照指定的 cron 表达式执行任务
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();

        // 添加任务到 Quartz 调度器，并传递设备序列号参数
        SyncResult r = QuartzSchedulerManager.getInstance().add(mTaskName
                , mGroupName
                , trigger
                , YSNVRWarnTaskJobV2.class
                , null
                , TAG);
        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    /**
     * 手动触发
     */
    public void trigger() {
        QuartzSchedulerManager.getInstance().triggerJob(mTaskName, mGroupName);
    }

    /**
     * Quartz 任务执行方法
     *
     * @param context Quartz 提供的任务执行上下文，包含任务执行相关的信息
     */
    @Override
    public void execute(JobExecutionContext context) {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();

        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务...", group, key);

        // 从系统全局配置中获取萤石云开放平台的 AppKey 和 AppSecret
        String appKey = SysGlobalConfigEntity.getString("YSS:AppKey");
        String appSecret = SysGlobalConfigEntity.getString("YSS:AppSecret");

        // 如果 AppKey 或 AppSecret 未配置，记录警告日志并退出任务
        if (!StringUtils.hasLength(appKey)) {
            LogsUtil.warn(TAG, "[%s-%s] 请配置萤石云开放平台 AppKey", group, key);
            return;
        }
        if (!StringUtils.hasLength(appSecret)) {
            LogsUtil.warn(TAG, "[%s-%s] 请配置萤石云开放平台 AppSecret", group, key);
            return;
        }

        // 初始化萤石云 SDK 并设置凭证
        YSSDK yssdk = new YSSDK()
                .setAppKey(appKey)
                .setAppSecret(appSecret);

        int page = 1;
        int limit = 100;
        while (true) {
            // 查询所有 NVR 类型的设备，并根据配置项判断是否需要添加萤石云监控任务
            List<Map<String, Object>> list = GeneralDeviceEntity.getInstance()
                    .field("gdc.serialNumber,gdc.config")
                    .alias("gd")
                    .join(GeneralDeviceConfigEntity.getInstance().theTableName(), "gdc", "gd.serialNumber = gdc.serialNumber")
                    .where("gd.typeCode", "4GNVR")
                    .where("gd.status", 1)
                    .page(page, limit)
                    .select();
            if (list == null || list.isEmpty()) break;

            page++;
            for (Map<String, Object> nd : list) {
                String config = MapUtil.getString(nd, "config");
                if (!StringUtils.hasLength(config)) continue;

                String liveProtocol = JsonUtil.getString(config, "$.LiveProtocol");
                if (!liveProtocol.equalsIgnoreCase("ys")) continue;

                String serialNumber = MapUtil.getString(nd, "serialNumber");
                LogsUtil.info(TAG, "%s - 执行任务中...", serialNumber);

                execute(serialNumber, yssdk);
            }
        }
    }

    /**
     * 检查设备存储介质状态
     *
     * @param deviceSerial 设备序列号
     * @param yssdk        萤石云 SDK 实例
     */
    private void execute(String deviceSerial, YSSDK yssdk) {
        // 调用萤石云 SDK 获取设备的存储介质状态
        SyncResult r = yssdk.deviceFormatStatus(deviceSerial);
        if (r.code != 0) return;

        /*
         * 解析响应数据，检查存储介质状态
         * data	object	响应数据
         * -storageStatus	array<object>	存储介质信息列表
         * --index	string	存储介质编号
         * --name	string	存储介质名称
         * --status	string	存储介质状态，0正常,1存储介质错,2未格式化,3正在格式化
         * --formattingRate	string	格式化进度
         */
        JSONArray list = (JSONArray) r.data;
        if (list == null || list.isEmpty()) return;

        // 遍历存储介质列表，检查每个介质的状态
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            String storageName = JsonUtil.getString(item, "name");
            int storageStatus = JsonUtil.getInt(item, "status");

            // 根据存储介质状态执行不同的逻辑
            String statusText = "正常";
            switch (storageStatus) {
                case 0:
                case 3:
                    continue; // 如果存储状态正常或正在格式化，继续下一次循环
                case 1: // 存储介质错误
                    statusText = "错误";
                    break;
                case 2: // 未格式化
                    statusText = "未格式化";
                    break;
            }

            // 获取设备实体对象
            GeneralDeviceEntity device = GeneralDeviceService.getInstance().getWithSerialNumber(deviceSerial);
            if (device == null) continue;

            // 组装警报消息的推送数据
            JSONObject rocketMQData = new JSONObject();
            rocketMQData.put("title", String.format("%s 存储异常", device.deviceName));
            rocketMQData.put("notify_type", "System");
            rocketMQData.put("organize_code", device.organize_code);
            rocketMQData.put("CSId", device.CSId);

            // 组装警报的扩展数据
            JSONObject rocketMQExtData = new JSONObject();
            rocketMQExtData.put("品牌", device.brandCode);
            rocketMQExtData.put("序列号", deviceSerial);
            rocketMQExtData.put("类型", device.typeCode);
            rocketMQExtData.put(storageName, statusText);

            rocketMQData.put("extraData", rocketMQExtData);
            rocketMQData.put("EndingTips", "进入萤石云App进行格式化操作");

            // 将警报消息推送到 RocketMQ
            XRocketMQ.getGlobal().pushOneway("{ENV}_SysNotify", "sendText", rocketMQData);
        }
    }
}