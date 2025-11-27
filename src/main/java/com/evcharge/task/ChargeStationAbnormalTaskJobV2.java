package com.evcharge.task;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ChargeStationErrorManageEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.sys.SysNotificationConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.ChargeStationXRocketMQConsumerV2;
import com.evcharge.service.notify.NotifyService;
import com.xyzs.entity.SyncResult;
import com.xyzs.task.QuartzSchedulerManager;
import com.xyzs.utils.*;
import org.quartz.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 充电桩设备异常监控任务（V2）
 * <p>
 * 职责：
 * 1) 定时分页扫描【充电站】（运营中），将单站检测任务通过 RocketMQ 投递，做削峰与解耦。
 * 2) 静态方法 {@link #execute(String)} 为单站检测入口：做通知开关过滤、异常统计、阈值判断、下发通知与错误记录。
 * 3) 具体异常判定逻辑集中在 {@link #checkAbnormal(String)}：
 * - 遍历站内设备与其端口（socket），按端口状态做规则判断，统计 normal/abnormal 及异常设备列表。
 * - 空闲端口额外基于最近充电订单做“无法启动”判定，详见 {@link #checkAbnormalOnChargeOrder(String)}。
 * <p>
 * 线程模型与安全：
 * - 作为 Quartz Job，被调度器串行/并行触发，类本身无共享可变状态；
 * - 提供 DCL（双重检查锁）形式的惰性单例获取，仅用于外部按需调用；
 * - 不在类内缓存外部实体对象，避免陈旧数据。
 */
public class ChargeStationAbnormalTaskJobV2 implements Job {

    // ===== 常量与元信息 =====
    /**
     * 日志标签
     */
    protected static final String TAG = "设备异常监控";
    /**
     * Job 名称（用于 Quartz 注册）
     */
    private static final String TASK_NAME = ChargeStationAbnormalTaskJobV2.class.getSimpleName();
    /**
     * Job 分组名
     */
    private static final String GROUP_NAME = "ChargeStationTask";

    /**
     * 异常率阈值（当前逻辑为 0.6 = 60%）。
     */
    private static final BigDecimal ABNORMAL_RATE_THRESHOLD = BigDecimal.valueOf(0.6);

    /**
     * 惰性单例
     */
    private static volatile ChargeStationAbnormalTaskJobV2 _this;

    /**
     * 获取单例实例（DCL）
     */
    public static ChargeStationAbnormalTaskJobV2 getInstance() {
        if (_this == null) {
            synchronized (ChargeStationAbnormalTaskJobV2.class) {
                if (_this == null) _this = new ChargeStationAbnormalTaskJobV2();
            }
        }
        return _this;
    }

    /**
     * 初始化调度：
     * Cron 表达式："0 30 1/6 * * ? *"，含义为：
     * - 秒=0，分=30，小时=从1点开始每6小时一次（1,7,13,19点），每天，每月，每周，任意年份。
     * - withMisfireHandlingInstructionFireAndProceed: 错过触发后立即补触发一次。
     */
    public void init() {
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 1/12 * * ? *")
                        .withMisfireHandlingInstructionFireAndProceed()
                ).build();

        SyncResult r = QuartzSchedulerManager.getInstance().add(
                TASK_NAME, GROUP_NAME, trigger,
                ChargeStationAbnormalTaskJobV2.class, null, TAG
        );

        LogsUtil.info(TAG, "\033[1;91m %s \033[0m", r.msg);
    }

    // ======================== Quartz Job 入口 ========================

    /**
     * 定时任务执行：分页扫描所有“运营中”的充电站，并将单站检测任务异步投递到 RocketMQ。
     * <p>
     * 说明：
     * - 此处只负责“分发”，不直接做重负载统计；单站检测在 {@link #execute(String)} 中完成。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String group = context.getJobDetail().getKey().getGroup();
        String key = context.getJobDetail().getKey().getName();
        LogsUtil.info(TAG, "[%s-%s] - 开始执行任务...", group, key);

        // 轻量优化：避免在分页循环内重复读取组织编码
        final String organizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");

        int page = 1;
        int limit = 100;

        while (true) {
            // 分页读取站点基础信息
            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                    .field("CSId,name,status")
                    .where("organize_code", organizeCode)
                    .page(page, limit)
                    .select();

            if (list == null || list.isEmpty()) break;
            page++;

            // 遍历所有充电站，为每个充电站添加监控任务（仅运营中：status=1）
            for (Map<String, Object> nd : list) {
                if (nd == null || nd.isEmpty()) continue;
                String CSId = MapUtil.getString(nd, "CSId");
                String name = MapUtil.getString(nd, "name");
                int status = MapUtil.getInt(nd, "status"); // 0=删除，1=运营中，2=建设中

                if (status == 1 && StringUtils.hasLength(CSId)) {
                    // 使用 RocketMQ 进行削峰：单向消息，不关心返回
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("CSId", CSId);
                    rocketMQData.put("name", name);
                    XRocketMQ.getGlobal().pushSync(
                            ChargeStationXRocketMQConsumerV2.TOPIC,
                            "AbnormalTask",
                            rocketMQData
                    );
                }
            }
        }
    }

    // ======================== 单站检测入口 ========================

    /**
     * 执行单站监控逻辑：通知开关检查 → 异常统计 → 阈值判断 → 通知推送与错误记录。
     *
     * @param CSId 充电桩（站点）ID，必填
     * @return 执行结果
     */
    public static SyncResult execute(String CSId) {
        if (!StringUtils.hasLength(CSId)) return new SyncResult(910, "缺少参数，停止这个任务");

        // ===== 通知开关检查（全局 → 站点 → 测试/私人类型） =====
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId("ChargeStationAbnormalNotifications", true).offline == 0) {
            return new SyncResult(0, "全站已关闭异常通知");
        }
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_%s", CSId), true).offline == 0) {
            return new SyncResult(0, "此充电站已关闭离线通知");
        }

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(910, "充电桩信息为空");

        // 测试/私人充电桩开关
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_isTest_%s", chargeStationEntity.isTest), true).offline == 0) {
            return new SyncResult(0, "测试充电桩已关闭离线通知");
        }
        if (SysNotificationConfigEntity.getInstance().getWithUniqueId(String.format("CS_is_private_%s", chargeStationEntity.is_private), true).offline == 0) {
            return new SyncResult(0, "私人充电桩已关闭通知");
        }

        // ===== 统计异常 =====
        SyncResult r = checkAbnormal(CSId);
        if (r.code != 0) return r;
        @SuppressWarnings("unchecked")
        Map<String, Object> cbdata = (Map<String, Object>) r.data;
        if (cbdata == null || !cbdata.containsKey("normal_count") || !cbdata.containsKey("abnormal_count")) {
            return new SyncResult(4, "获取设备统计信息失败");
        }

        int normal_count = MapUtil.getInt(cbdata, "normal_count");
        int abnormal_count = MapUtil.getInt(cbdata, "abnormal_count");
        @SuppressWarnings("unchecked")
        Map<String, Object> abnormal_device = (Map<String, Object>) cbdata.get("abnormal_device");

        // 避免除 0
        if (normal_count + abnormal_count == 0) {
            return new SyncResult(1, "设备总数为零，无法计算异常率");
        }

        // 异常率（保留 2 位小数）
        BigDecimal rate = new BigDecimal(abnormal_count)
                .divide(new BigDecimal(normal_count + abnormal_count), 2, RoundingMode.HALF_UP);

        // 阈值判断（注意：当前为 60%）
        if (rate.compareTo(ABNORMAL_RATE_THRESHOLD) < 0) {
            // 现有文案写“低于50%无需通知”，与阈值不一致，保持现状，仅返回说明。
            return new SyncResult(1, "异常率低于阈值（当前配置为60%），无需通知");
        }

        // ===== 组装地址文本（保持原始拼接逻辑，可能包含 "null"） =====
        String address = chargeStationEntity.province
                + chargeStationEntity.city
                + " " + chargeStationEntity.district
                + " " + chargeStationEntity.street
                + " " + chargeStationEntity.communities
                + " " + chargeStationEntity.roads
                + chargeStationEntity.address;

        // ===== 组装异常设备文本 =====
        StringBuilder abnormalDeviceText = new StringBuilder();
        StringBuilder deviceNumberStr = new StringBuilder(); // 用于异常站点信息表
        if (abnormal_device != null && !abnormal_device.isEmpty()) {
            for (String deviceNumber : abnormal_device.keySet()) {
                String status_msg = MapUtil.getString(abnormal_device, deviceNumber);
                abnormalDeviceText.append(String.format(">%s %s\n", deviceNumber, status_msg));
                deviceNumberStr.append(deviceNumber).append(",");
            }
        }

        // ===== 新版本通知系统（2024-10-10） =====
        JSONObject notifyTransData = new JSONObject();
        notifyTransData.put("title", String.format("(%s)%s 异常", CSId, chargeStationEntity.name));
        notifyTransData.put("time", TimeUtil.getTimeString());
        notifyTransData.put("abnormal_rate", rate.multiply(new BigDecimal(100)));
        notifyTransData.put("abnormal_device", abnormalDeviceText);
        notifyTransData.put("address", address);
        NotifyService.getInstance().asyncPush(CSId, "SYSTEM.CS.ABNORMAL", notifyTransData);

        // ===== 写入异常站点信息表（2024-10-30） =====
        String errorContent = String.format(
                "标题：%s\n;时间：%s\n;异常率：%s\n;地址：%s\n",
                String.format("(%s)%s 异常", CSId, chargeStationEntity.name),
                TimeUtil.getTimeString(),
                rate.multiply(new BigDecimal(100)),
                address
        );
        LogsUtil.info(TAG, "创建异常站点信息:" + errorContent);
        ChargeStationErrorManageEntity.getInstance().createMsg(2, chargeStationEntity, deviceNumberStr.toString(), errorContent);

        return new SyncResult(0, "");
    }

    // ======================== 异常统计核心 ========================

    /**
     * 核心异常统计：
     * - 遍历站点设备与其端口（socket），按端口状态做规则判断；
     * - 累计 normal_count / abnormal_count；
     * - 记录异常设备及原因（key=deviceNumber，value=status_msg）。
     *
     * @param CSId 站点 ID
     * @return SyncResult.data 结构：{
     * "normal_count": int,
     * "abnormal_count": int,
     * "abnormal_device": Map<String,Object>
     * }
     */
    public static SyncResult checkAbnormal(String CSId) {
        if (!StringUtils.hasLength(CSId)) return new SyncResult(2, "请输出充电桩ID");

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
        if (chargeStationEntity == null) return new SyncResult(2, "无法查询充电桩信息");

        // 查询站点下所有设备
        List<Map<String, Object>> list = DeviceEntity.getInstance()
                .field("id,deviceCode,deviceNumber,online_status,isHost,update_time")
                .where("CSId", CSId)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(3, "无设备数据");

        int normal_count = 0;   // 正常端口计数（按 socket 统计）
        int abnormal_count = 0; // 异常端口计数
        Map<String, Object> abnormal_device = new LinkedHashMap<>();

        for (Map<String, Object> device_data : list) {
            if (device_data == null || device_data.isEmpty()) continue;

            String deviceCode = MapUtil.getString(device_data, "deviceCode");
            String deviceNumber = MapUtil.getString(device_data, "deviceNumber");

            // 查询设备端口
            List<Map<String, Object>> socketList = DeviceSocketEntity.getInstance()
                    .field("id,index,port,status,status_msg,usePower,limitChargePower,temperature,door_status,fan_status,inputVoltage,outputVoltage,update_time")
                    .where("deviceCode", deviceCode)
                    .select();

            if (socketList == null || socketList.isEmpty()) continue;

            for (Map<String, Object> data : socketList) {
                if (data == null || data.isEmpty()) continue;

//                int index = MapUtil.getInt(data, "index"); // 端口序号（当前逻辑未使用，仅保留）
                int status = MapUtil.getInt(data, "status");
                String status_msg = MapUtil.getString(data, "status_msg");

                // 状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充，6=定时预约，7=等待用户确认
                if (status == 4) {
                    abnormal_count++;
                    abnormal_device.put(deviceNumber, status_msg);
                    continue; // 明确异常，直接下一个端口
                } else if (status == 1) { // 充电中
                    // === 检查充电功率是否超额 ===
                    // 注意：原逻辑取值 usePower 来自 "limitChargePower"，疑似笔误，但为保持行为不改动，仅加注释。
                    double usePower = MapUtil.getDouble(data, "limitChargePower"); // TODO: 可能应为 "usePower"
                    double limitChargePower = MapUtil.getDouble(data, "limitChargePower");
                    if (usePower > limitChargePower + 500) {
                        abnormal_count++;
                        abnormal_device.put(deviceNumber, String.format("功率过大(%sW)", usePower));
                        continue;
                    }

                    // === 温度是否过高 ===
                    double temperature = MapUtil.getDouble(data, "temperature");
                    if (temperature >= 50) {
                        abnormal_count++;
                        abnormal_device.put(deviceNumber, String.format("温度过高(%s℃)", temperature));
                        continue;
                    }

                    // === 输出电压是否过高 ===
                    double outputVoltage = MapUtil.getDouble(data, "outputVoltage");
                    if (outputVoltage >= 230) {
                        abnormal_count++;
                        abnormal_device.put(deviceNumber, String.format("电压过高(%sV", outputVoltage));
                        continue;
                    }
                } else if (status == 0) { // 空闲
                    // 结合最近充电订单判断是否“无法启动”
                    if (checkAbnormalOnChargeOrder(deviceCode)) {
                        abnormal_count++;
                        abnormal_device.put(deviceNumber, "无法启动");
                        continue;
                    }
                }

                // 未触发异常规则，按正常计数
                normal_count++;
            }
        }

        Map<String, Object> cbdata = new LinkedHashMap<>();
        cbdata.put("normal_count", normal_count);
        cbdata.put("abnormal_count", abnormal_count);
        cbdata.put("abnormal_device", abnormal_device);
        return new SyncResult(0, "", cbdata);
    }

    // ======================== 辅助判定 ========================

    /**
     * 基于充电订单的异常判定：用于端口处于“空闲(status=0)”时判断设备是否可能存在“无法启动”。
     * <p>
     * 重要：本方法返回值语义为【true = 异常，false = 正常】（与原方法注释不一致，现已改正注释以匹配现有调用逻辑）。
     * 规则：
     * - 若无任何订单记录：视为异常（返回 true）。
     * - 否则读取最近 50 条：
     * * 若最新一条为“充电中(1)”或“已完成(2)”，说明设备正常（返回 false）。
     * * 统计“待启动(0)”占比，若 ≥ 50%，视为异常（返回 true）。
     *
     * @param deviceCode 设备编码
     * @return true=异常 | false=正常
     */
    public static boolean checkAbnormalOnChargeOrder(String deviceCode) {
        List<Map<String, Object>> list = ChargeOrderEntity.getInstance()
                .field("id,status")
                .where("deviceCode", deviceCode)
                .page(1, 50)
                .order("id DESC")
                .select();
        if (list == null || list.isEmpty()) return true; // 异常：一直没有充电订单

        int error_count = 0;
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> data = list.get(i);
            // 状态：-1=启动错误，0=待启动，1=充电中，2=已完成
            int status = MapUtil.getInt(data, "status");

            // 若最新一笔正在充电或已完成，视为设备正常
            if (i == 0 && (status == 1 || status == 2)) return false;

            // 待启动计入错误比重
            if (status == 0) error_count++;
        }
        double rate = (double) error_count / list.size();
        return rate >= 0.5; // true=异常（≥50%）
    }

    /**
     * 基于最近充电时间的异常判定：
     * <p>
     * 返回值语义同上：true=异常 | false=正常。
     * 规则：
     * - 若不存在任何订单：返回 true（异常）。
     * - 若当前时间超过最后一次订单创建时间 3 天：返回 true（异常）。
     * <p>
     * 注：当前方法未被上游调用，保留供后续场景使用。
     */
    public static boolean checkAbnormalOnLastChargeTime(String deviceCode) {
        Map<String, Object> data = ChargeOrderEntity.getInstance()
                .field("id,status,create_time")
                .where("deviceCode", deviceCode)
                .limit(1)
                .order("create_time DESC")
                .find();
        if (data == null || data.isEmpty()) return true; // 异常：一直没有充电订单

        long create_time = MapUtil.getLong(data, "create_time");
        return TimeUtil.getTimestamp() > create_time + ECacheTime.DAY * 3L;
    }
}
