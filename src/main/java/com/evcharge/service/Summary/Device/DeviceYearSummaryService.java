package com.evcharge.service.Summary.Device;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceMonthSummaryEntity;
import com.evcharge.entity.device.DeviceSummaryEntity;
import com.evcharge.entity.device.DeviceYearSummaryEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备年数据统计服务类
 * 用于统计设备每年的消费数据，并汇总至设备年汇总表
 */
public class DeviceYearSummaryService {

    /**
     * 日志标签，便于日志统一过滤
     */
    private static final String TAG = "设备年数据统计";

    /**
     * 获取一个新的 Service 实例。
     * 当前无状态持有，可直接 new。后续如需改为单例，可在此修改。
     */
    public static DeviceYearSummaryService getInstance() {
        return new DeviceYearSummaryService();
    }

    // region 数据同步核心逻辑

    /**
     * 启动同步任务调度器，在指定时间范围内对所有设备进行按年汇总统计。
     *
     * @param start_time  开始时间（秒时间戳）
     * @param end_time    结束时间（秒时间戳）
     * @param useRocketMQ 是否通过 RocketMQ 推送任务（暂未启用）
     */
    public void syncTaskSchedule(long start_time, long end_time, boolean useRocketMQ) {
        int page = 1;
        int limit = 100;

        // 将开始和结束时间归一化到每年1日 00:00:00，避免统计跨年数据
        start_time = TimeUtil.toYearBegin00(start_time);
        end_time = TimeUtil.toYearEnd24(end_time);

        while (true) {
            // 分页查询设备列表（已启用状态）
            List<Map<String, Object>> list = DeviceEntity.getInstance()
                    .field("deviceCode, d.create_time, cs.online_time")
                    .alias("d")
                    .leftJoin(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                    .page(page, limit)
                    .select();

            if (list == null || list.isEmpty()) break;

            // 遍历每台设备进行任务下发
            for (Map<String, Object> nd : list) {
                String device_code = MapUtil.getString(nd, "deviceCode");
                long create_time = MapUtil.getLong(nd, "create_time");
                long online_time = MapUtil.getLong(nd, "online_time", 0);
                if (create_time > 0) create_time = TimeUtil.toMonthBegin00(create_time);
                if (online_time > 0) online_time = TimeUtil.toMonthBegin00(online_time);

                long end_time_temp = end_time;
                long start_time_temp = Math.max(start_time, Math.max(create_time, online_time));
                if (start_time_temp > end_time_temp) {
                    end_time_temp = TimeUtil.getMonthEnd();
                }

                long finalEnd_time = end_time_temp;

                if (useRocketMQ) {
                    // 预留RocketMQ支持，目前未启用
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("device_code", device_code);
                    rocketMQData.put("start_time", start_time_temp);
                    rocketMQData.put("end_time", finalEnd_time);
                    XRocketMQ.getGlobal().pushOneway("", "", rocketMQData);
                } else {
                    // 本地线程执行同步任务
                    ThreadUtil.getInstance().execute(() -> syncTask(device_code, start_time_temp, finalEnd_time));
                }
            }

            page++;
        }
    }

    /**
     * 同步指定设备的年度统计任务
     *
     * @param device_code 设备编码
     * @param start_time  起始时间戳（秒）
     * @param end_time    结束时间戳（秒）
     */
    public void syncTask(@NonNull String device_code, long start_time, long end_time) {
        // 归一化时间戳
        start_time = TimeUtil.toMonthBegin00(start_time);
        end_time = TimeUtil.toMonthBegin00(end_time);

        // 循环每个年执行统计任务
        while (start_time <= end_time) {
            syncJob(device_code, start_time);
            start_time = TimeUtil.getAddYearTimestamp(start_time, 1); // 加一个年
        }
    }

    /**
     * 执行设备的年度统计任务
     *
     * @param device_code    设备编码
     * @param date_timestamp 任意位于当年的时间戳（秒）
     * @return 同步结果对象
     */
    private SyncResult syncJob(@NonNull String device_code, long date_timestamp) {
        if (StringUtil.isEmpty(device_code)) return new SyncResult(2, "缺少设备编码");
        if (date_timestamp == 0) date_timestamp = TimeUtil.getTimestamp();
        String date = TimeUtil.toTimeString(date_timestamp, "yyyy"); // 格式化为年年

        LogsUtil.info(TAG, "[%s] - %s - 正在统计数据...", device_code, date);

        try {
            // 获取当月起止时间戳
            final long startTime = TimeUtil.toYearBegin00(date_timestamp);
            final long endTime = TimeUtil.toYearEnd24(date_timestamp);

            // 初始化数据结构
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("device_code", device_code);
            data.put("date", date);
            data.put("date_time", startTime);

            if (!ChargeOrderEntity.getInstance()
                    .where("deviceCode", device_code)
                    .where("status", 2) // 已完成订单
                    .exist()) {
                LogsUtil.info(TAG, "[%s] - 设备没有充电订单无法统计", device_code);
                return new SyncResult(3, "设备没有充电订单无法统计");
            }

            Map<String, Object> nd = DeviceEntity.getInstance()
                    .field("deviceCode, d.create_time, cs.online_time, d.CSId")
                    .alias("d")
                    .leftJoin(ChargeStationEntity.getInstance().theTableName(), "cs", "d.CSId = cs.CSId")
                    .find();
            long create_time = MapUtil.getLong(nd, "create_time", 0);
            long online_time = MapUtil.getLong(nd, "online_time", 0);
            long start_time = 0;
            if (create_time > 0) start_time = TimeUtil.toMonthBegin00(create_time);
            if (online_time > 0) start_time = TimeUtil.toMonthBegin00(online_time);
            if (start_time == 0) {
                LogsUtil.info(TAG, "[%s] - 设备还没上线，无需统计", device_code);
                return new SyncResult(3, "设备还没上线");
            }
            // 运行天数
            long runDays = TimeUtil.getDaysDiff(start_time, TimeUtil.getTime24());

            // ========== 余额支付部分消费统计 ==========
            Map<String, Object> summaryData = DeviceMonthSummaryEntity.getInstance()
                    .field("IFNULL(SUM(consume_amount),0) AS consume_amount"
                            + ",IFNULL(SUM(per_consume_amount),0) AS per_consume_amount"
                            + ",IFNULL(SUM(per_adj_consume_amount),0) AS per_adj_consume_amount"
                            + ",IFNULL(SUM(card_consume_amount),0) AS card_consume_amount"
                            + ",IFNULL(SUM(card_adj_consume_amount),0) AS card_adj_consume_amount"
                    )
                    .where("device_code", device_code)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .find();

            BigDecimal consume_amount = MapUtil.getBigDecimal(summaryData, "consume_amount", 4, RoundingMode.HALF_UP);
            BigDecimal avg_day_consume_amount = consume_amount.divide(new BigDecimal(runDays), 4, RoundingMode.HALF_UP);

            data.put("consume_amount", MapUtil.getBigDecimal(summaryData, "consume_amount"));
            data.put("per_consume_amount", MapUtil.getBigDecimal(summaryData, "per_consume_amount"));
            data.put("per_adj_consume_amount", MapUtil.getBigDecimal(summaryData, "per_adj_consume_amount"));
            data.put("card_consume_amount", MapUtil.getBigDecimal(summaryData, "card_consume_amount"));
            data.put("card_adj_consume_amount", MapUtil.getBigDecimal(summaryData, "card_adj_consume_amount"));
            data.put("avg_day_consume_amount", avg_day_consume_amount);
            data.put("run_days", runDays);

            // ========== 写入或更新汇总表 ==========
            DeviceYearSummaryEntity entity = new DeviceYearSummaryEntity();
            data.put("update_time", TimeUtil.getTimestamp());

            if (entity.where("device_code", device_code)
                    .where("date_time", startTime)
                    .exist()) {
                // 存在则更新
                entity.where("device_code", device_code)
                        .where("date_time", startTime)
                        .update(data);
            } else {
                // 不存在则插入
                data.put("create_time", TimeUtil.getTimestamp());
                entity.insert(data);
            }

            LogsUtil.info(TAG, "[%s] - 统计完成 - 消费金额：%s 平均消费金额：%s 运行：%s 天", device_code, consume_amount, avg_day_consume_amount, runDays);

        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s] - 统计数据发生错误!!!", device_code);
        }

        return new SyncResult(0, "");
    }

    // endregion
}

