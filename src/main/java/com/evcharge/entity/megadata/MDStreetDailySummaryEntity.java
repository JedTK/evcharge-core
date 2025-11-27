package com.evcharge.entity.megadata;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.AreaXRocketMQConsumerV2;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 街道/城镇每日数据汇总;
 *
 * @author : JED
 * @date : 2023-8-24
 */
public class MDStreetDailySummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 次数使用率（APR）：所有充电次数 / 全平台运行中的充电端口（不含私有桩）  (次/插座)
     */
    public BigDecimal chargeCountUseRate;
    /**
     * 时长使用率（APR）：所有充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)   %
     */
    public BigDecimal chargeTimeUseRate;
    /**
     * 2025-06-11 新使用率算法：端口使用率 = 在统计周期内至少被使用过一次的端口数量 / 总端口数
     * 注：同一端口多次使用只计为一次，用于衡量端口活跃覆盖率
     */
    public BigDecimal deviceSocketUseRate;
    /**
     * 总充电次数
     */
    public long chargingTotalCount;
    /**
     * 累计充电时长
     */
    public long totalChargeTime;
    /**
     * 累计耗电量（度）
     */
    public BigDecimal totalPowerConsumption;
    /**
     * 累计充电用户数
     */
    public long chargingUserCount;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static MDStreetDailySummaryEntity getInstance() {
        return new MDStreetDailySummaryEntity();
    }

    private final static String TAG = "街道/城镇日汇总";

    /**
     * 同步数据
     *
     * @param street         街道
     * @param street_code    街道编码
     * @param date_timestamp 统计时间戳
     */
    public SyncResult syncTaskJob(String street, String street_code, long date_timestamp) {
        long startTime = TimeUtil.toTimestamp00(date_timestamp);
        long endTime = TimeUtil.toTimestamp24(date_timestamp);

        String date = TimeUtil.toTimeString(startTime, "yyyy-MM-dd");

        LogsUtil.info(TAG, "[%s-%s] 正在统计 %s 数据...", street, street_code, date);
        try {
            MDStreetDailySummaryEntity summaryEntity = new MDStreetDailySummaryEntity();
            summaryEntity.street_code = street_code;
            summaryEntity.chargeCountUseRate = new BigDecimal(0);
            summaryEntity.chargeTimeUseRate = new BigDecimal(0);
            summaryEntity.totalPowerConsumption = new BigDecimal(0);

            String[] CSIds = ChargeStationEntity.getInstance().where("street_code", street_code).where("status", ">", 0).selectForStringArray("id");
            if (CSIds.length == 0) return new SyncResult(0, "");

            String[] deviceIds = DeviceEntity.getInstance().whereIn("CSId", CSIds).where("isHost", 0)//主机：0=否，1=是
                    .selectForStringArray("id");

            //充电次数
            int deviceSocketCount = 0;
            summaryEntity.chargingTotalCount = ChargeOrderEntity.getInstance().whereIn("CSId", CSIds)
                    .where("status", 2)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .count();
            if (deviceIds.length > 0) {
                //充电位总数
                deviceSocketCount = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).count();
            }
            //累计充电用户数
            summaryEntity.chargingUserCount = UserEntity.getInstance()
                    .whereIn("cs_id", CSIds)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .count();

            //endregion

            //region 充电时长、累计耗电量（度）
            Map<String, Object> sum_count = ChargeOrderEntity.getInstance().field("IFNULL(SUM(totalChargeTime),0) AS totalChargeTime,IFNULL(SUM(powerConsumption),0) AS totalPowerConsumption").whereIn("CSId", CSIds).where("status", 2).where("isTest", 0).where("create_time", ">=", startTime).where("create_time", "<=", endTime).find();
            //充电时长
            summaryEntity.totalChargeTime = MapUtil.getLong(sum_count, "totalChargeTime");
            //累计耗电量（度）
            summaryEntity.totalPowerConsumption = MapUtil.getBigDecimal(sum_count, "totalPowerConsumption");
            //endregion

            //region 次数使用率（APR）：此街道统计日充电次数 / 此街道运行中的充电端口

            summaryEntity.chargeCountUseRate = new BigDecimal(0);
            if (deviceSocketCount > 0) {
                summaryEntity.chargeCountUseRate = new BigDecimal(summaryEntity.chargingTotalCount)
                        .divide(new BigDecimal(deviceSocketCount), 6, RoundingMode.HALF_UP);
            }

            //endregion

            //region 时长使用率（APR）：此街道统计日充电时长「秒数」 / 此街道(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
            summaryEntity.chargeTimeUseRate = new BigDecimal(0);

            //端口运行时间 = 充电桩的运营时间 * 端口数
            List<Map<String, Object>> chargeStationEntityList = ChargeStationEntity.getInstance().field("id,online_time,totalSocket").where("status", 1).where("is_private", 0).where("is_restricted", 0).where("isTest", 0).order("id").select();
            //端口运行时间 = 充电桩的运营时间 * 端口数

            long totalSocketRunTime = 0;
            if (chargeStationEntityList != null) {
                for (Map<String, Object> nd : chargeStationEntityList) {
                    long online_time = MapUtil.getLong(nd, "online_time");
                    int totalSocket_temp = MapUtil.getInt(nd, "totalSocket");

                    //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计
                    if (online_time >= endTime) continue;

                    //表示充电桩的上线时间比统计开始时间还晚，应该以上线时间来进行计算,注意时间戳为毫秒级
                    if (online_time >= startTime) {
                        totalSocketRunTime += (endTime - online_time) / 1000 * totalSocket_temp;
                    } else {
                        totalSocketRunTime += (endTime - startTime) / 1000 * totalSocket_temp;
                    }
                }
            }
            if (totalSocketRunTime > 0) {
                summaryEntity.chargeTimeUseRate = BigDecimal.valueOf(summaryEntity.totalChargeTime).divide(BigDecimal.valueOf(totalSocketRunTime), 6, RoundingMode.HALF_UP);
            }

            //endregion

            // region remark 2025-06-11 2025-06-11 新使用率算法：端口使用率 = 在统计周期内至少被使用过一次的端口数量 / 总端口数
            // 注：同一端口多次使用只计为一次，用于衡量端口活跃覆盖率
            summaryEntity.deviceSocketUseRate = BigDecimal.ZERO;
            Map<String, Object> deviceSocketUsedCountData = ChargeOrderEntity.getInstance()
                    .field("COUNT(DISTINCT deviceCode, port) AS deviceSocketUseRate")
                    .whereIn("CSId", CSIds)
                    .where("status", 2)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            long deviceSocketUseRate = MapUtil.getLong(deviceSocketUsedCountData, "deviceSocketUseRate");
            if (deviceSocketUseRate > 0) {
                summaryEntity.deviceSocketUseRate = new BigDecimal(deviceSocketUseRate).divide(new BigDecimal(deviceSocketCount), 6, RoundingMode.HALF_UP);
            }
//            LogsUtil.warn(TAG, "[%s-%s] - %s 端口使用率：%s / %s = %s", street, street_code, date, deviceSocketUseRate, deviceSocketCount, summaryEntity.deviceSocketUseRate);
            // endregion

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("street_code", street_code);
            data.put("date", date);
            data.put("date_time", startTime);

            data.put("chargeCountUseRate", summaryEntity.chargeCountUseRate);
            data.put("chargeTimeUseRate", summaryEntity.chargeTimeUseRate);

            data.put("chargingTotalCount", summaryEntity.chargingTotalCount);
            data.put("chargingUserCount", summaryEntity.chargingUserCount);
            data.put("deviceSocketUseRate", summaryEntity.deviceSocketUseRate);

            data.put("totalChargeTime", summaryEntity.totalChargeTime);
            data.put("totalPowerConsumption", summaryEntity.totalPowerConsumption);
            data.put("update_time", TimeUtil.getTimestamp());

            if (summaryEntity.where("street_code", street_code).where("date_time", startTime).exist()) {
                summaryEntity.where("street_code", street_code).where("date_time", startTime).update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                summaryEntity.insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "街道/城镇数据汇总;", "街道/城镇数据汇总发生错误，street_code=%s", street_code);
        }

        LogsUtil.info(TAG, "[%s-%s] 统计数据 %s 完成！", street, street_code, date);
        return new SyncResult(0, "");
    }

    /**
     * 内嵌自己来重复修复（辅助使用）
     */
    public void syncData(String street, String street_code, long start_time, long end_time) {
        start_time = TimeUtil.toTimestamp00(start_time);
        // 使用循环代替递归
        while (start_time <= end_time) {
            syncTaskJob(street, street_code, start_time);
            start_time += ECacheTime.DAY;
        }
    }

    /**
     * 启动充电桩修复每日数据任务（支持普通线程与RocketMQ两种方式）
     *
     * @param start_time  任务开始时间戳
     * @param end_time    任务结束时间戳
     * @param useRocketMQ 是否使用RocketMQ批量启动任务
     */
    public void startSyncTask(long start_time, long end_time, boolean useRocketMQ) {
        // 标准化时间戳
        start_time = TimeUtil.toTimestamp00(start_time);
        end_time = TimeUtil.toTimestamp00(end_time);

        int page = 1;
        int limit = 100;

        while (true) {
            // 分页查询符合条件的充电站列表
            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                    .field("street,street_code")
                    .where("status", 1)
                    .group("street,street_code")
                    .order("street,street_code")
                    .page(page, limit)
                    .select();

            if (list == null || list.isEmpty()) break;

            page++;

            for (Map<String, Object> nd : list) {
                String street = MapUtil.getString(nd, "street");
                String street_code = MapUtil.getString(nd, "street_code");

                if (!StringUtils.hasLength(street_code)) continue;

                if (useRocketMQ) {
                    // 使用RocketMQ发送任务
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("street", street);
                    rocketMQData.put("street_code", street_code);

                    // 查询充电站最早在线时间，更新任务起始时间
                    Map<String, Object> data = ChargeStationEntity.getInstance()
                            .field("online_time")
                            .where("street_code", street_code)
                            .where("online_time", ">", 0)
                            .order("online_time")
                            .find();
                    if (data != null && !data.isEmpty()) {
                        long online_time = MapUtil.getLong(data, "online_time");
                        rocketMQData.put("start_time", Math.max(online_time, start_time));
                    } else {
                        rocketMQData.put("start_time", start_time);
                    }
                    rocketMQData.put("end_time", end_time);

                    XRocketMQ.getGlobal().pushOneway(AreaXRocketMQConsumerV2.TOPIC, "StreetDaySummaryTask", rocketMQData);
                } else {
                    // 使用线程直接启动任务
                    Map<String, Object> data = ChargeStationEntity.getInstance()
                            .field("online_time")
                            .where("street_code", street_code)
                            .where("online_time", ">", 0)
                            .order("online_time")
                            .find();
                    long online_time = MapUtil.getLong(data, "online_time");
                    long finalEnd_time = end_time;
                    long finalStart_time = Math.max(online_time, start_time);
                    ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", street, street_code, TAG), () -> syncData(street, street_code, finalStart_time, finalEnd_time)
                    );
                }
            }
        }
    }
}
