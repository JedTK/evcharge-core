package com.evcharge.service.Summary.Device;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceMonthSummaryEntity;
import com.evcharge.entity.device.DeviceSummaryEntity;
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
 * 设备数据统计服务类
 * 用于统计设备消费数据
 */
public class DeviceSummaryService {
    /**
     * 日志标签，便于日志统一过滤
     */
    private static final String TAG = "设备数据统计";

    /**
     * 获取一个新的 Service 实例。
     * 当前无状态持有，可直接 new。后续如需改为单例，可在此修改。
     */
    public static DeviceSummaryService getInstance() {
        return new DeviceSummaryService();
    }

    // region 数据同步核心逻辑

    /**
     * 启动同步任务调度器，所有设备进行汇总统计。
     *
     * @param useRocketMQ 是否通过 RocketMQ 推送任务（暂未启用）
     */
    public void syncTaskSchedule(boolean useRocketMQ) {
        int page = 1;
        int limit = 100;

        while (true) {
            // 分页查询设备列表（已启用状态）
            List<Map<String, Object>> list = DeviceEntity.getInstance()
                    .field("deviceCode")
                    .page(page, limit)
                    .order("id")
                    .select();

            if (list == null || list.isEmpty()) break;

            // 遍历每台设备进行任务下发
            for (Map<String, Object> nd : list) {
                String device_code = MapUtil.getString(nd, "deviceCode");

                if (useRocketMQ) {
                    // 预留RocketMQ支持，目前未启用
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("device_code", device_code);
                    XRocketMQ.getGlobal().pushOneway("", "", rocketMQData);
                } else {
                    ThreadUtil.getInstance().execute(() -> syncJob(device_code));
                }
            }

            page++;
        }
    }

    /**
     * 执行设备的统计任务
     *
     * @param device_code 设备编码
     * @return 同步结果对象
     */
    private SyncResult syncJob(@NonNull String device_code) {
        if (StringUtil.isEmpty(device_code)) return new SyncResult(2, "缺少设备编码");

        LogsUtil.info(TAG, "[%s] - 正在统计数据...", device_code);

        try {
            // 初始化数据结构
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("device_code", device_code);

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
            DeviceSummaryEntity entity = new DeviceSummaryEntity();
            data.put("update_time", TimeUtil.getTimestamp());

            if (entity.where("device_code", device_code).exist()) {
                // 存在则更新
                entity.where("device_code", device_code).update(data);
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
