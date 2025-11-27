package com.evcharge.entity.RSProfit;


import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 分润系统充电桩月汇总;
 *
 * @author : JED
 * @date : 2024-7-15
 */
@Getter
@Setter
@TargetDB("evcharge_rsprofit")
public class RSProfitChargeStationMonthSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    private String date;
    /**
     * 日期时间戳
     */
    private long date_time;
    /**
     * 收益人联系电话
     */
    private String channel_phone;
    /**
     * 充电桩ID
     */
    private String cs_id;
    /**
     * 总收益
     */
    private BigDecimal total_income;
    /**
     * 创建时间戳
     */
    private long create_time;
    /**
     * 更新时间戳
     */
    private long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static RSProfitChargeStationMonthSummaryEntity getInstance() {
        return new RSProfitChargeStationMonthSummaryEntity();
    }

    private final static String TAG = "分润收益月汇总";

    /**
     * 同步任务作业
     *
     * @param date_timestamp 统计时间戳（这里泛指按月统计）
     * @return 同步结果
     */
    public SyncResult syncTaskJob(String channel_phone, String cs_id, long date_timestamp) {
        if (date_timestamp == 0) date_timestamp = ChargeStationEntity.getInstance().getEarliestOnlineTime();
        if (date_timestamp == 0) date_timestamp = TimeUtil.getTimestamp();

        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM");
        LogsUtil.info(TAG, "[%s] %s 正在统计数据...", channel_phone, date);

        try {
            //当月凌晨时间戳
            final long startTime = TimeUtil.toMonthBegin00(date_timestamp);
            //当月结束时间戳
            final long endTime = TimeUtil.toMonthEnd24(date_timestamp);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("date", date);
            data.put("date_time", startTime);
            data.put("channel_phone", channel_phone);
            data.put("cs_id", cs_id);

            // 总收益
            BigDecimal total_income = RSProfitIncomeLogsEntity.getInstance()
                    .where("channel_phone", channel_phone)
                    .where("cs_id", cs_id)
                    .where("date_time", ">=", startTime)
                    .where("date_time", "<=", endTime)
                    .sumGetBigDecimal("amount");
            data.put("total_income", total_income);

            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance()
                    .where("date_time", startTime)
                    .where("channel_phone", channel_phone)
                    .where("cs_id", cs_id)
                    .exist()) {
                getInstance()
                        .where("date_time", startTime)
                        .where("channel_phone", channel_phone)
                        .where("cs_id", cs_id)
                        .update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "汇总数据发生错误，date=%s", date);
        }

        LogsUtil.info(TAG, "[%s] %s 统计数据 完成！", channel_phone, date);
        return new SyncResult(0, "");
    }

    /**
     * 同步数据（内嵌调用）
     */
    public void syncData(String channel_phone, String cs_id, long date_timestamp) {
        long startTime = TimeUtil.toMonthBegin00(date_timestamp);
        long month = TimeUtil.getMonthBegin00();
        if (startTime <= month) {
            SyncResult r = syncTaskJob(channel_phone, cs_id, startTime);
            if (r.code == 0) startTime = TimeUtil.getAddMonthTimestamp(startTime, 1);
            syncData(channel_phone, cs_id, startTime);
        }
    }

    /**
     * 开始同步
     */
    public void startSync(String channel_phone, long start_time) {
        List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                .field("cs_id")
                .where("channel_phone", channel_phone)
                .group("cs_id")
                .select();
        for (Map<String, Object> data : list) {
            String cs_id = MapUtil.getString(data, "cs_id");

            if (!StringUtil.hasLength(channel_phone)) continue;
            ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", channel_phone, cs_id, TAG), () -> {
                syncData(channel_phone, cs_id, start_time);
            });
        }
    }

    /**
     * 开始同步
     */
    public void startSync(long start_time) {
        List<Map<String, Object>> list = RSProfitConfigEntity.getInstance()
                .field("channel_phone,cs_id,start_time")
                .group("channel_phone,cs_id,start_time")
                .select();
        for (Map<String, Object> data : list) {
            String channel_phone = MapUtil.getString(data, "channel_phone");
            String cs_id = MapUtil.getString(data, "cs_id");
            long start_time_db = MapUtil.getLong(data, "start_time");

            if (!StringUtil.hasLength(channel_phone)) continue;
            ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", channel_phone, cs_id, TAG), () -> {
                syncData(channel_phone, cs_id, Math.max(start_time, start_time_db));
            });
        }
    }
}
