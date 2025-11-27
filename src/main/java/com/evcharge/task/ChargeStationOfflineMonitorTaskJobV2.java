package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.sys.SysNotificationConfigEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.ChargeStationXRocketMQConsumerV2;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 充电桩离线监控任务（V2）
 * <p>
 * 职责：
 * 1) 由 Quartz 周期触发，分页扫描“运营中”的充电站，将单站离线检测任务下发到 RocketMQ（削峰、解耦）。
 * 2) 静态方法 {@link #execute(String)}：对单个站点做离线统计与通知（按设备维度汇总、阈值判断）。
 * <p>
 * 设计要点（本次仅做注释与轻量安全优化，不改变核心业务逻辑）：
 * - 统一提取调度与阈值常量，便于后续配置化；
 * - 分页循环中做空值保护；
 * - 对时间戳、字符串拼接等维持原行为（避免影响前端/告警侧口径）。
 */
public class ChargeStationOfflineMonitorTaskJobV2 implements Job {

    // ===== 常量与元信息 =====

    /**
     * 日志标签
     */
    protected static final String TAG = "充电桩离线报告任务";
    /**
     * Job 名称（用于 Quartz 注册）
     */
    private static final String TASK_NAME = ChargeStationOfflineMonitorTaskJobV2.class.getSimpleName();
    /**
     * Job 分组
     */
    private static final String GROUP_NAME = "ChargeStationTask";

    /**
     * 离线判定时间容差（毫秒）：最近一次 update_time 超过 30 分钟未更新 → 判为离线
     */
    private static final long OFFLINE_TOLERANCE_MS = ECacheTime.MINUTE * 30L;
    /**
     * 离线占比阈值（0.5 = 50%）：达到/超过即触发通知
     */
    private static final double OFFLINE_RATE_THRESHOLD = 0.5d;

    /**
     * 惰性单例（与现有工程风格保持一致）
     */
    private static volatile ChargeStationOfflineMonitorTaskJobV2 _this;

    /**
     * 获取单例实例（DCL）
     */
    public static ChargeStationOfflineMonitorTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (ChargeStationOfflineMonitorTaskJobV2.class) {
                if (_this == null) _this = new ChargeStationOfflineMonitorTaskJobV2();
            }
        }
        return _this;
    }

    /**
     * 初始化：
     * - 使用 SimpleScheduleBuilder 每 SCAN_INTERVAL_MS 触发一次；
     * - 首次触发引入 1~60 分钟随机抖动，平摊高峰；
     * - MISFIRE 策略采用 FireNow：错过触发则立即补触发一次。
     */
    public void init() {
        String cron = "0 12 8,14,20 * * ?";
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();

        SyncResult r = QuartzSchedulerManager.getInstance().add(TASK_NAME
                , GROUP_NAME
                , trigger
                , ChargeStationOfflineMonitorTaskJobV2.class
                , null
                , TAG);
        LogsUtil.info(TAG, "\033[1;91m %s 预计执行时间：%s \033[0m", r.msg, TimeUtil.toTimeString(trigger.getNextFireTime()));
    }

    // ======================== Quartz Job 入口 ========================

    /**
     * 定时任务执行：
     * - 分页读取符合组织编码的站点列表；
     * - 仅对 status=1（运营中）的站点投递离线检测消息到 RocketMQ（单向）；
     * - 单站具体检测逻辑在 {@link #execute(String)}。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务...", group, key);

        // 轻量优化：避免在分页循环中重复读取组织编码
        final String organizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");

        execute(organizeCode, true);
    }

    /**
     * 离线检查批量运行任务(单独函数出来是可以提供给其他调用者调用)
     *
     * @param organizeCode
     * @param use_mq
     */
    public static void execute(String organizeCode, boolean use_mq) {
        int page = 1;
        int limit = 100;
        long pages;
        long total_count = ChargeStationEntity.getInstance()
                .field("CSId,name,status")
                .where("organize_code", organizeCode)
                .where("status", 1) // 状态：0=删除，1=运营中，2=建设中
                .countGetLong("1");
        if (total_count == 0) {
            LogsUtil.warn(TAG, "当前无运营的充电站!!!");
            return;
        }
        pages = Convert.toInt(Math.ceil(total_count * 1.0 / limit));

        while (page <= pages) {
            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                    .field("CSId,name,status")
                    .where("organize_code", organizeCode)
                    .where("status", 1)
                    .page(page, limit)
                    .select();
            if (list == null || list.isEmpty()) break;

            LogsUtil.info(TAG, "进度：%s/%s - 开始修复", page, pages);

            for (Map<String, Object> nd : list) {
                if (nd == null || nd.isEmpty()) continue;
                String CSId = MapUtil.getString(nd, "CSId");
                String name = MapUtil.getString(nd, "name");

                if (StringUtil.isEmpty(name)) continue;

                LogsUtil.info(TAG, "[%s-%s] - 执行...", CSId, name);

                if (use_mq) {
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("CSId", CSId);
                    rocketMQData.put("name", name);
                    // 削峰：投递单向消息，由消费端串行执行单站检测
                    XRocketMQ.getGlobal().pushSync(ChargeStationXRocketMQConsumerV2.TOPIC, "OfflineMonitorTask", rocketMQData);
                } else ChargeStationOfflineMonitorTaskJobV2.execute(CSId);
            }
            page++;
        }

        LogsUtil.info(TAG, "已全部执行完毕");
    }

    // ======================== 单站离线检测 ========================

    /**
     * 单站离线检测与通知：
     * 1) 按多级通知开关判断（全局→站点→类型：测试/私人），关闭则直接返回。
     * 2) 遍历站点下设备：
     * - 设备级开关关闭 → ignore_count++
     * - 缓存状态 Device:{deviceCode}:status == 1 → online_count++
     * - 否则检查 update_time（30 分钟容差）→ 在线/离线计数
     * 3) 计算离线占比（总设备数为分母，保持既有口径），达到阈值（≥50%）则推送通知。
     *
     * @param CSId 站点 ID
     * @return 执行结果
     */
    public static SyncResult execute(String CSId) {
        if (!StringUtils.hasLength(CSId)) return new SyncResult(910, "缺少参数，停止这个任务");

        // ===== 通知开关检查 =====
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId("DeviceOfflineNotifications", true).offline == 0) {
            return new SyncResult(0, "全站已关闭离线通知");
        }
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_%s", CSId), true).offline == 0) {
            return new SyncResult(0, "此充电站已关闭离线通知");
        }

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(910, "充电桩信息为空");

        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_isTest_%s", chargeStationEntity.isTest), true).offline == 0) {
            return new SyncResult(0, "测试充电桩已关闭离线通知");
        }
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_is_private_%s", chargeStationEntity.is_private), true).offline == 0) {
            return new SyncResult(0, "私人充电桩已关闭通知");
        }

        // ===== 遍历设备 =====
        List<Map<String, Object>> list = DeviceEntity.getInstance()
                .field("deviceCode,deviceNumber,online_status,isHost,update_time")
                .where("CSId", CSId).select();
        if (list == null || list.isEmpty()) return new SyncResult(3, "无设备数据");

        int online_count = 0;   // 在线设备数
        int offline_count = 0;  // 离线设备数
        int ignore_count = 0;   // 忽略设备数（不需要通知）
        StringBuilder offline_device = new StringBuilder();

        final long now = TimeUtil.getTimestamp();

        for (Map<String, Object> device_data : list) {
            if (device_data == null || device_data.isEmpty()) continue;

            String deviceCode = MapUtil.getString(device_data, "deviceCode");
            String deviceNumber = MapUtil.getString(device_data, "deviceNumber");
            int isHost = MapUtil.getInt(device_data, "isHost");

            // 设备级开关：关闭则忽略
            if (SysNotificationConfigEntity.getInstance().getWithUniqueId(deviceCode, true).offline == 0) {
                ignore_count++;
                continue;
            }

            // 先看缓存状态（快速路径）
            int status = DataService.getMainCache().getInt(String.format("Device:%s:status", deviceCode), -1);
            if (status == 1) {
                online_count++;
                continue;
            }

            // 再看最后上报时间（30 分钟内视为仍在线，保持原逻辑）
            long update_time = MapUtil.getLong(device_data, "update_time", 0);
            if (now <= update_time + OFFLINE_TOLERANCE_MS) {
                online_count++;
                continue;
            }

            // 判定为离线
            offline_count++;
            if (isHost == 1) {
                offline_device.append(String.format("[主]%s（%s）\n", deviceNumber, TimeUtil.toTimeString(update_time, "MM/dd HH:mm")));
            } else {
                offline_device.append(String.format("%s（%s）\n", deviceNumber, TimeUtil.toTimeString(update_time, "MM/dd HH:mm")));
            }
        }

        // 计算离线占比（分母为总设备数，保持现有口径；注意：包含被忽略的设备）
        double offline_rate = (double) offline_count / list.size();

        if (offline_rate >= OFFLINE_RATE_THRESHOLD) {
            // 地址拼接（保持现有拼接策略，可能包含 "null" 文本）
            String address = chargeStationEntity.province + chargeStationEntity.city + " " + chargeStationEntity.district + " " + chargeStationEntity.street + " " + chargeStationEntity.communities + " " + chargeStationEntity.roads + chargeStationEntity.address;

            // 2024-10-10 新版通知
            JSONObject notifyTransData = new JSONObject();
            notifyTransData.put("title", String.format("(%s)%s", CSId, chargeStationEntity.name));
            notifyTransData.put("time", TimeUtil.getTimeString());
            notifyTransData.put("online_count", online_count);
            notifyTransData.put("offline_count", offline_count);
            notifyTransData.put("offline_device", offline_device);
            notifyTransData.put("ignore_count", ignore_count);
            notifyTransData.put("address", address);

            NotifyService.getInstance().asyncPush(CSId, "SYSTEM.CS.OFFLINE", notifyTransData);
            return new SyncResult(0, "");
        }
        return new SyncResult(1, "");
    }
}
