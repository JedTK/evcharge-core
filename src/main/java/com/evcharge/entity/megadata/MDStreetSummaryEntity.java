package com.evcharge.entity.megadata;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.*;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.AreaXRocketMQConsumerV2;
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
 * 街道/城镇数据汇总;
 *
 * @author : JED
 * @date : 2023-8-21
 */
public class MDStreetSummaryEntity extends BaseEntity implements Serializable {
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
     * 省
     */
    public String province;
    /**
     * 市
     */
    public String city;
    /**
     * 区/县
     */
    public String district;
    /**
     * 街道和城镇
     */
    public String street;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 充电桩使用率
     */
    public double csUseRate;
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
     * 2025-06-11 7天端口使用率
     */
    public BigDecimal deviceSocket7DayUseRate;
    /**
     * 2025-06-11 30天端口使用率
     */
    public BigDecimal deviceSocket30DayUseRate;
    /**
     * 充电桩数量
     */
    public int csCount;
    /**
     * 充电桩建造中数量
     */
    public int csBuildingCount;
    /**
     * 充电位总数
     */
    public int deviceSocketCount;
    /**
     * 充电位使用中数量
     */
    public int deviceSocketUsingCount;
    /**
     * 充电位空闲中数量
     */
    public int deviceSocketIdleCount;
    /**
     * 充电位占用中数量
     */
    public int deviceSocketOccupiedCount;
    /**
     * 充电位异常数量
     */
    public int deviceSocketErrorCount;
    /**
     * 总充电次数
     */
    public long chargingTotalCount;
    /**
     * 本月充电次数
     */
    public long chargingMonthCount;
    /**
     * 昨日充电次数
     */
    public long chargingYesterdayCount;
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
     * 监视器设备数
     */
    public int monitorTotalCount;
    /**
     * 监视器在线数
     */
    public int monitorOnlineCount;
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
    public static MDStreetSummaryEntity getInstance() {
        return new MDStreetSummaryEntity();
    }

    private final static String TAG = "街道/城镇数据汇总";

    /**
     * 同步数据
     *
     * @param street_code 街道编码
     */
    public SyncResult syncTaskJob(String street, String street_code) {
        LogsUtil.info(TAG, "[%s-%s] 正在统计数据...", street, street_code);

        try {
            MDStreetSummaryEntity summaryEntity = MDStreetSummaryEntity.getInstance()
                    .where("street_code", street_code)
                    .findEntity();
            if (summaryEntity == null || summaryEntity.id == 0) {
                summaryEntity = new MDStreetSummaryEntity();
            }
            summaryEntity.street_code = street_code;
            summaryEntity.chargeCountUseRate = new BigDecimal(0);
            summaryEntity.chargeTimeUseRate = new BigDecimal(0);
            summaryEntity.totalPowerConsumption = new BigDecimal(0);

            summaryEntity.street_code = street_code;

            String[] CSIds = ChargeStationEntity.getInstance()
                    .where("street_code", street_code)
                    .where("status", ">", 0)
                    .selectForStringArray("id");
            if (CSIds.length == 0) return new SyncResult(0, "");

            String[] deviceIds = DeviceEntity.getInstance()
                    .whereIn("CSId", CSIds)
                    .where("isHost", 0)//主机：0=否，1=是
                    .selectForStringArray("id");


            //region 充电桩数量、充电桩建造中数量、充电位总数、充电位使用中数量、充电位空闲中数量、充电位占用中数量、充电位异常数量

            //充电桩数量
            summaryEntity.csCount = CSIds.length;
            //充电桩建造中数量
            summaryEntity.csBuildingCount = ChargeStationEntity.getInstance()
                    .whereIn("id", CSIds)
                    .where("status", 2)//状态：0=删除，1=正常，2=在建
                    .count();

            if (deviceIds.length > 0) {
                //充电位总数
                summaryEntity.deviceSocketCount = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds).count();
                //充电位使用中数量
                summaryEntity.deviceSocketUsingCount = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds)
                        .whereIn("status", "1,5")//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位空闲中数量
                summaryEntity.deviceSocketIdleCount = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds)
                        .where("status", 0)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位占用中数量
                summaryEntity.deviceSocketOccupiedCount = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds)
                        .whereIn("status", "2,3")//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位异常数量
                summaryEntity.deviceSocketErrorCount = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds)
                        .where("status", 4)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
            }
            //endregion

            //region 总充电次数、本月充电次数、昨日充电次数、累计充电用户数
            //总充电次数
            summaryEntity.chargingTotalCount = ChargeOrderEntity.getInstance()
                    .whereIn("CSId", CSIds)
                    .where("status", 2)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                    .count();

            //本月充电次数
            summaryEntity.chargingMonthCount = ChargeOrderEntity.getInstance()
                    .whereIn("CSId", CSIds)
                    .where("status", 2)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                    .where("create_time", ">=", TimeUtil.getMonthBegin00())
                    .where("create_time", "<=", TimeUtil.getMonthEnd24())
                    .count();

            //昨日充电次数
            summaryEntity.chargingYesterdayCount = ChargeOrderEntity.getInstance()
                    .whereIn("CSId", CSIds)
                    .where("status", 2)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                    .where("create_time", ">=", TimeUtil.getTime00(-1))
                    .where("create_time", "<=", TimeUtil.getTime24(-1))
                    .count();

            //累计充电用户数
            summaryEntity.chargingUserCount = UserEntity.getInstance()
                    .whereIn("cs_id", CSIds)
                    .count();

            //endregion

            //region 充电时长、累计耗电量（度）
            Map<String, Object> sumcount = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(totalChargeTime),0) AS totalChargeTime,IFNULL(SUM(powerConsumption),0) AS totalPowerConsumption")
                    .whereIn("CSId", CSIds)
                    .where("status", 2)
                    .where("isTest", 0)
                    .find();
            //当月充电时长
            summaryEntity.totalChargeTime = MapUtil.getLong(sumcount, "totalChargeTime");
            //累计耗电量（度）
            summaryEntity.totalPowerConsumption = MapUtil.getBigDecimal(sumcount, "totalPowerConsumption");
            //endregion

            //region 监视器设备数、监视器在线数

            summaryEntity.monitorTotalCount = GeneralDeviceEntity.getInstance()
                    .where("typeCode", "4GNVR")
                    .where("status", 1)//状态：0-删除，1-正常
                    .whereIn("CSId", CSIds)
                    .count();

            summaryEntity.monitorOnlineCount = GeneralDeviceEntity.getInstance()
                    .where("typeCode", "4GNVR")
                    .where("status", 1)//状态：0-删除，1-正常
                    .where("online_status", 1)//在线状态：0-离线，1-在线
                    .whereIn("CSId", CSIds)
                    .count();

            //endregion

            //region 次数使用率（APR）：此街道充电次数 / 此街道运行中的充电端口

            summaryEntity.chargeCountUseRate = new BigDecimal(0);
            if (summaryEntity.deviceSocketCount > 0) {
                summaryEntity.chargeCountUseRate = BigDecimal.valueOf(summaryEntity.chargingTotalCount)
                        .divide(BigDecimal.valueOf(summaryEntity.deviceSocketCount), 6, RoundingMode.HALF_UP);
            }

            //endregion

            //region 时长使用率（APR）：此街道充电时长「秒数」 / 此街道(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
            summaryEntity.chargeTimeUseRate = new BigDecimal(0);

            List<Map<String, Object>> chargeStationEntityList = ChargeStationEntity.getInstance()
                    .whereIn("id", CSIds)
                    .field("id,online_time,totalSocket")
                    .where("status", 1)
//                    .where("is_private", 0)
//                    .where("is_restricted", 0)
//                    .where("isTest", 0)
                    .order("id")
                    .select();
            //端口运行时间 = 充电桩的运营时间 * 端口数

            long totalSocketRunTime = 0;
            if (chargeStationEntityList != null) {
                for (Map<String, Object> nd : chargeStationEntityList) {
                    long online_time = MapUtil.getLong(nd, "online_time");
                    int totalSocket_temp = MapUtil.getInt(nd, "totalSocket");

                    //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计(这里实际永远不会为true，除非有人设定好上线时间)
                    if (online_time >= TimeUtil.getTimestamp()) continue;

                    //应该以上线时间来进行计算,注意时间戳为毫秒级
                    totalSocketRunTime += (TimeUtil.getTimestamp() - online_time) / 1000 * totalSocket_temp;
                }
            }
            if (totalSocketRunTime > 0) {
                summaryEntity.chargeTimeUseRate = BigDecimal.valueOf(summaryEntity.totalChargeTime)
                        .divide(BigDecimal.valueOf(totalSocketRunTime), 6, RoundingMode.HALF_UP);
            }
            //endregion

            // region remark 2025-06-11 2025-06-11 新使用率算法：端口使用率 = 在统计周期内至少被使用过一次的端口数量 / 总端口数
            // 注：同一端口多次使用只计为一次，用于衡量端口活跃覆盖率
            Map<String, Object> deviceSocketUseRateData = MDStreetDailySummaryEntity.getInstance()
                    .field("AVG(deviceSocketUseRate) AS deviceSocketUseRate ")
                    .where("street_code", street_code)
                    .find();
            summaryEntity.deviceSocketUseRate = MapUtil.getBigDecimal(deviceSocketUseRateData, "deviceSocketUseRate", 14, RoundingMode.HALF_UP);

            Map<String, Object> deviceSocket7DayUseRateData = MDStreetDailySummaryEntity.getInstance()
                    .field("AVG(deviceSocketUseRate) AS deviceSocketUseRate ")
                    .where("street_code", street_code)
                    .where("date_time", ">=", TimeUtil.getTime00(-7))
                    .find();
            summaryEntity.deviceSocket7DayUseRate = MapUtil.getBigDecimal(deviceSocket7DayUseRateData, "deviceSocketUseRate", 14, RoundingMode.HALF_UP);

            Map<String, Object> deviceSocket30DayUseRateData = MDStreetDailySummaryEntity.getInstance()
                    .field("AVG(deviceSocketUseRate) AS deviceSocketUseRate ")
                    .where("street_code", street_code)
                    .where("date_time", ">=", TimeUtil.getTime00(-30))
                    .find();
            summaryEntity.deviceSocket30DayUseRate = MapUtil.getBigDecimal(deviceSocket30DayUseRateData, "deviceSocketUseRate", 14, RoundingMode.HALF_UP);

            // endregion

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("csUseRate", summaryEntity.csUseRate);
            data.put("chargeCountUseRate", summaryEntity.chargeCountUseRate);
            data.put("chargeTimeUseRate", summaryEntity.chargeTimeUseRate);
            data.put("deviceSocketUseRate", summaryEntity.deviceSocketUseRate);
            data.put("deviceSocket7DayUseRate", summaryEntity.deviceSocket7DayUseRate);
            data.put("deviceSocket30DayUseRate", summaryEntity.deviceSocket30DayUseRate);

            data.put("csCount", summaryEntity.csCount);
            data.put("csBuildingCount", summaryEntity.csBuildingCount);

            data.put("deviceSocketCount", summaryEntity.deviceSocketCount);
            data.put("deviceSocketUsingCount", summaryEntity.deviceSocketUsingCount);
            data.put("deviceSocketIdleCount", summaryEntity.deviceSocketIdleCount);
            data.put("deviceSocketOccupiedCount", summaryEntity.deviceSocketOccupiedCount);
            data.put("deviceSocketErrorCount", summaryEntity.deviceSocketErrorCount);

            data.put("chargingTotalCount", summaryEntity.chargingTotalCount);
            data.put("chargingMonthCount", summaryEntity.chargingMonthCount);
            data.put("chargingYesterdayCount", summaryEntity.chargingYesterdayCount);
            data.put("chargingUserCount", summaryEntity.chargingUserCount);

            data.put("totalChargeTime", summaryEntity.totalChargeTime);
            data.put("totalPowerConsumption", summaryEntity.totalPowerConsumption);

            data.put("monitorTotalCount", summaryEntity.monitorTotalCount);
            data.put("monitorOnlineCount", summaryEntity.monitorOnlineCount);

            //region 自动查找街道经纬度中心点
            if (summaryEntity.lon == 0 || summaryEntity.lat == 0) {
                double lon = 0;
                double lat = 0;
                SysStreetEntity streetEntity = SysStreetEntity.getInstance().getWithCode(street_code);
                if (streetEntity != null) {
                    data.put("street", streetEntity.street_name);
                    if (summaryEntity.csCount >= 10) {
                        lon = Double.parseDouble(streetEntity.lng);
                        lat = Double.parseDouble(streetEntity.lat);
                    }

                    SysAreaEntity areaEntity = SysAreaEntity.getInstance().getWithCode(streetEntity.area_code);
                    if (areaEntity != null) {
                        data.put("district", areaEntity.area_name);

                        SysCityEntity cityEntity = SysCityEntity.getInstance().getWithCode(areaEntity.city_code);
                        if (cityEntity != null) {
                            data.put("city", cityEntity.city_name);

                            SysProvinceEntity provinceEntity = SysProvinceEntity.getInstance().getWithCode(cityEntity.province_code);
                            if (provinceEntity != null) {
                                data.put("province", provinceEntity.province_name);
                            }
                        }
                    }
                }

                if (lon == 0 || lat == 0) {
                    //不然就以首个充电桩为中心
                    Map<String, Object> cs_data = ChargeStationEntity.getInstance()
                            .field("id,lon,lat")
                            .where("status", 1)
                            .where("street_code", street_code)
                            .where("isTest", 0)
                            .find();
                    if (cs_data != null && !cs_data.isEmpty()) {
                        lon = MapUtil.getDouble(cs_data, "lon");
                        lat = MapUtil.getDouble(cs_data, "lat");
                    }
                }
                data.put("lon", lon);
                data.put("lat", lat);
            }

            //endregion

            data.put("update_time", TimeUtil.getTimestamp());

            if (summaryEntity.where("street_code", street_code).exist()) {
                summaryEntity.where("street_code", street_code).update(data);
            } else {
                data.put("street_code", street_code);
                data.put("create_time", TimeUtil.getTimestamp());
                summaryEntity.insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "街道/城镇数据汇总;", "街道/城镇数据汇总发生错误，street_code=%s", street_code);
        }

        LogsUtil.info(TAG, "[%s-%s] 数据完成！", street, street_code);
        return new SyncResult(0, "");
    }

    /**
     * 启动充电桩修复每日数据的任务（支持普通线程与RocketMQ两种方式）
     *
     * @param useRocketMQ 是否使用RocketMQ批量启动任务
     */
    public void startSyncTask(boolean useRocketMQ) {
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
                    XRocketMQ.getGlobal().pushOneway(AreaXRocketMQConsumerV2.TOPIC, "StreetSummaryTask", rocketMQData);
                } else {
                    // 使用线程直接启动任务
                    ThreadUtil.getInstance().execute(String.format("[%s-%s] %s", street, street_code, TAG), () -> syncTaskJob(street, street_code));
                }
            }
        }
    }
}
