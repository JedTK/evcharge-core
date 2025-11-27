package com.evcharge.entity.platform;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.rbac.RBOrganizeEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.PlatformXRocketMQConsumerV2;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台日汇总v2版本;
 *
 * @author : JED
 * @date : 2024-6-24
 */
@Getter
@Setter
public class PlatformDaySummaryV2Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 组织代码
     */
    private String organize_code;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    private String date;
    /**
     * 日期时间戳
     */
    private long date_time;
    /**
     * 累计的注册用户数
     */
    private int total_registered_users;
    /**
     * 充电桩数量
     */
    private int charge_station_count;
    /**
     * 充电网络主机数
     */
    private int total_host_device_count;
    /**
     * 充电终端数
     */
    private int total_charge_device_count;
    /**
     * 总端口数
     */
    private int total_socket;
    /**
     * 端口运行时间(秒)
     */
    private long total_socket_run_time;
    /**
     * 累计使用次数
     */
    private long total_use_count;
    /**
     * 累计安全预警次数
     */
    private long total_use_error;
    /**
     * 累计耗电量（度）
     */
    private BigDecimal total_power_consumption;
    /**
     * 累计充电时长(秒)
     */
    private long total_charge_time;
    /**
     * 当前用户余额
     */
    private BigDecimal total_user_balance;
    /**
     * 计次充电的次数
     */
    private int pay_per_charge_count;
    /**
     * 计次充电的人数
     */
    private int pay_per_charge_users;
    /**
     * 计次充电的时长(秒)
     */
    private long pay_per_charge_duration;
    /**
     * 计次充电消费金额
     */
    private BigDecimal pay_per_charge_amount;
    /**
     * 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
     */
    private BigDecimal pay_per_adjustment_charge_amount;
    /**
     * 充电卡充电的次数
     */
    private int card_charge_count;
    /**
     * 充电卡充电的人数
     */
    private int card_charge_users;
    /**
     * 充电卡充电的时长(秒)
     */
    private long card_charge_duration;
    /**
     * 充电卡消耗时间(秒)
     */
    private long card_charge_consume_time;
    /**
     * 充电卡消费金额
     */
    private BigDecimal card_charge_amount;
    /**
     * 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
     */
    private BigDecimal card_adjustment_charge_amount;
    /**
     * 充值订单数
     */
    private int recharge_order_count;
    /**
     * 充值金额（毛）
     */
    private BigDecimal recharge_amount;
    /**
     * 充值人数
     */
    private int recharge_users;
    /**
     * 充值退款订单数
     */
    private int recharge_refund_order_count;
    /**
     * 充值退款订单金额
     */
    private BigDecimal recharge_refund_amount;
    /**
     * 充电卡订单数
     */
    private int charge_card_order_count;
    /**
     * 充电卡金额（毛）
     */
    private BigDecimal charge_card_amount;
    /**
     * 充电卡人数
     */
    private int charge_card_users;
    /**
     * 充电卡退款订单数
     */
    private int charge_card_refund_order_count;
    /**
     * 充电卡退款订单金额
     */
    private BigDecimal charge_card_refund_amount;
    /**
     * 应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
     */
    private BigDecimal taxable_income;
    /**
     * (次/插座)次数使用率（APR）：当日充电次数 / 全平台运行中的充电端口
     */
    private BigDecimal charge_count_use_rate;
    /**
     * (%)时长使用率（APR）：当日充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
     */
    private BigDecimal charge_time_use_rate;
    /**
     * 创造的收益(元/日/插座): 每日净收入的平均值
     */
    private BigDecimal net_income;
    /**
     * 有充值或购买充电卡的用户数
     */
    private int payment_user_count;
    /**
     * 有充值或购买充电卡但没有充电的用户数
     */
    private int idle_user_count;
    /**
     * 创建时间
     */
    private long create_time;
    /**
     * 更新时间
     */
    private long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static PlatformDaySummaryV2Entity getInstance() {
        return new PlatformDaySummaryV2Entity();
    }

    private final static String TAG = "平台日汇总v2";

    /**
     * 同步任务作业
     *
     * @param date_timestamp 统计时间戳（这里泛指按日统计）
     */
    public SyncResult syncTaskJob(String organize_code, long date_timestamp) {
        if (date_timestamp == 0) date_timestamp = ChargeStationEntity.getInstance().getEarliestOnlineTime();
        if (date_timestamp == 0) date_timestamp = TimeUtil.getTimestamp();

        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM-dd");
        LogsUtil.info(TAG, "[%s] %s 正在统计数据...", organize_code, date);

        try {
            //当天凌晨时间戳
            final long startTime = TimeUtil.toTimestamp00(date_timestamp);
            //当天结束时间戳
            final long endTime = TimeUtil.toTimestamp24(date_timestamp);
            //前一天凌晨时间戳
            final long beforeDayStartTime = startTime - ECacheTime.DAY;
            //前一天结束时间戳
            final long beforeDayEndTime = TimeUtil.toDayEnd24(beforeDayStartTime);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("organize_code", organize_code);
            data.put("date", date);
            data.put("date_time", startTime);

            String CSTableName = ChargeStationEntity.getInstance().theTableName();
            String UserTableName = UserEntity.getInstance().theTableName();
            String sysOrganizeCode = SysGlobalConfigEntity.getString("System:Organize:Code");

            long total_registered_users = 0;
            //region 累计的注册用户数
            if (sysOrganizeCode.equalsIgnoreCase(organize_code)) {
                total_registered_users = UserEntity.getInstance()
                        .alias("u")
                        .leftJoin(CSTableName, "cs", "cs.CSId = u.cs_id")
                        .where("(", "cs.organize_code", "=", organize_code, "")
                        .addWhere("OR cs.organize_code IS NULL )")
                        .where("u.create_time", ">=", startTime)
                        .where("u.create_time", "<=", endTime)
                        .count("1");
            } else {
                total_registered_users = UserEntity.getInstance()
                        .alias("u")
                        .join(CSTableName, "cs", "cs.CSId = u.cs_id")
                        .where("cs.organize_code", organize_code)
                        .where("u.create_time", ">=", startTime)
                        .where("u.create_time", "<=", endTime)
                        .count("1");
            }
            //endregion
            data.put("total_registered_users", total_registered_users);

            int charge_station_count = 0;
            //region 充电桩数量
            charge_station_count = ChargeStationEntity.getInstance()
                    .where("organize_code", organize_code)
                    .where("status", 1)
                    .where("is_private", 0)
                    .where("is_restricted", 0)
                    .where("isTest", 0)
                    .where("online_time", ">", 0)
                    .where("online_time", "<=", endTime)
                    .count("1");
            //endregion
            data.put("charge_station_count", charge_station_count);

            //region 充电网络主机数、充电终端数、总电位数
            //充电网络主机数
            int total_host_device_count = 0;
            int total_charge_device_count = 0;
            int total_socket = 0;
            if (charge_station_count > 0) {
                total_host_device_count = DeviceEntity.getInstance()
                        .alias("d")
                        .join(CSTableName, "cs", "cs.CSId = d.CSId")
                        .where("cs.organize_code", organize_code)
                        .where("cs.status", 1)
                        .where("cs.is_private", 0)
                        .where("cs.is_restricted", 0)
                        .where("cs.isTest", 0)
                        .where("cs.online_time", ">", 0)
                        .where("cs.online_time", "<=", endTime)
                        .where("d.isHost", 1)
                        .count("1");

                //充电终端数
                total_charge_device_count = DeviceEntity.getInstance()
                        .alias("d")
                        .join(CSTableName, "cs", "cs.CSId = d.CSId")
                        .where("cs.organize_code", organize_code)
                        .where("cs.status", 1)
                        .where("cs.is_private", 0)
                        .where("cs.is_restricted", 0)
                        .where("cs.isTest", 0)
                        .where("cs.online_time", ">", 0)
                        .where("cs.online_time", "<=", endTime)
                        .where("d.isHost", 0)
                        .count("1");

                //总电位数
                total_socket = DeviceSocketEntity.getInstance()
                        .alias("ds")
                        .join(DeviceEntity.getInstance().theTableName(), "d", "ds.deviceId = d.id")
                        .join(CSTableName, "cs", "cs.CSId = d.CSId")
                        .where("cs.organize_code", organize_code)
                        .where("cs.status", 1)
                        .where("cs.is_private", 0)
                        .where("cs.is_restricted", 0)
                        .where("cs.isTest", 0)
                        .where("cs.online_time", ">", 0)
                        .where("cs.online_time", "<=", endTime)
                        .count("1");
            }
            data.put("total_host_device_count", total_host_device_count);
            data.put("total_charge_device_count", total_charge_device_count);
            data.put("total_socket", total_socket);
            //endregion

            long total_use_count = 0;
            //region 累计使用次数、累计耗电量（度）
            Map<String, Object> sumCount = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS total_use_count,IFNULL(SUM(powerConsumption),0) AS total_power_consumption")
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            total_use_count = MapUtil.getLong(sumCount, "total_use_count");
            //endregion
            data.put("total_use_count", total_use_count);
            //累计耗电量（度）
            data.put("total_power_consumption", sumCount.get("total_power_consumption"));

            long total_use_error = 0;
            //region累计安全预警次数
            total_use_error = ChargeOrderEntity.getInstance()
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .whereIn("stopReasonCode", "-1,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                    .countGetLong("1");
            //endregion
            data.put("total_use_error", total_use_error);

            BigDecimal pay_per_charge_amount = new BigDecimal(0);
            //region 计次充电的次数、计次充电的人数、计次充电消费金额、计次充电的时长
            Map<String, Object> pay_per_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS pay_per_charge_count"
                            + ",COUNT(DISTINCT uid) AS pay_per_charge_users"
                            + ",IFNULL(SUM(totalAmount),0) AS pay_per_charge_amount"
                    )
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();

            //计次充电的次数
            data.put("pay_per_charge_count", pay_per_charge_data.get("pay_per_charge_count"));
            //计次充电的人数
            data.put("pay_per_charge_users", pay_per_charge_data.get("pay_per_charge_users"));
            //计次充电消费金额
            pay_per_charge_amount = MapUtil.getBigDecimal(pay_per_charge_data, "pay_per_charge_amount");
            //计次充电的时长
            data.put("pay_per_charge_amount", pay_per_charge_amount);

            long pay_per_charge_duration = 0;
            //region 计次充电的时长（考虑跨天的情况）

            //计算当天不跨天充电的充电时间
            long chargeTime_ms1 = 0;
            Map<String, Object> chargeTimeData1 = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("stopTime", "<=", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long currentChargeTime1 = MapUtil.getLong(chargeTimeData1, "chargeTime");
            if (currentChargeTime1 > 0) {
                chargeTime_ms1 += currentChargeTime1;
            }

            //查询前一天跨天充电的充电时间
            Map<String, Object> beforeChargeTimeData1 = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", beforeDayStartTime)
                    .where("startTime", "<=", beforeDayEndTime)
                    .where("stopTime", ">=", startTime)
                    .where("stopTime", ">", 0)
                    .find();
            long beforeChargeTime1 = MapUtil.getLong(beforeChargeTimeData1, "chargeTime");
            if (beforeChargeTime1 > 0) {
                chargeTime_ms1 += beforeChargeTime1;
            }

            //查询当天跨天充电的充电时间
            Map<String, Object> afterChargeTimeData1 = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("startTime", "<=", endTime)
                    .where("stopTime", ">", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long afterChargeTime1 = MapUtil.getLong(afterChargeTimeData1, "chargeTime");
            if (afterChargeTime1 > 0) {
                chargeTime_ms1 += afterChargeTime1;
            }

            pay_per_charge_duration = chargeTime_ms1 / 1000;
            data.put("pay_per_charge_duration", pay_per_charge_duration);
            //endregion

            //endregion
            BigDecimal pay_per_adjustment_charge_amount = new BigDecimal(0);
            //region 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> pay_per_adjustment_charge_amount_data = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(totalAmount),0) AS pay_per_adjustment_charge_amount")
                    .alias("co")
                    .join(UserTableName, "u", "u.id = co.uid")
                    .where("co.organize_code", organize_code)
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("co.isTest", 0)
                    .where("u.is_robot", 3)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .find();
            pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(pay_per_adjustment_charge_amount_data, "pay_per_adjustment_charge_amount", 4, RoundingMode.HALF_UP, new BigDecimal(0));
            //endregion
            data.put("pay_per_adjustment_charge_amount", pay_per_adjustment_charge_amount);

            //region 充电卡充电的次数、充电卡充电的人数、充电卡消费金额、充电卡充电的时长
            Map<String, Object> card_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS card_charge_count"
                            + ",COUNT(DISTINCT uid) AS card_charge_users"
                            + ",IFNULL(SUM(chargeCardConsumeAmount),0) AS card_charge_amount"
                    )
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();

            //充电卡充电的次数
            data.put("card_charge_count", card_charge_data.get("card_charge_count"));
            //充电卡充电的人数
            data.put("card_charge_users", card_charge_data.get("card_charge_users"));
            //充电卡消费金额
            BigDecimal card_charge_amount = MapUtil.getBigDecimal(card_charge_data, "card_charge_amount", 4, RoundingMode.HALF_UP);
            data.put("card_charge_amount", card_charge_amount);

            //充电卡充电的时长
            long card_charge_duration = 0;
            //region 充电卡充电的时长（考虑跨天的情况）

            //计算当天不跨天充电的充电时间
            long chargeTime_ms2 = 0;
            Map<String, Object> chargeTimeData2 = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("stopTime", "<=", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long currentChargeTime2 = MapUtil.getLong(chargeTimeData2, "chargeTime");
            if (currentChargeTime2 > 0) {
                chargeTime_ms2 += currentChargeTime2;
            }

            //查询前一天跨天充电的充电时间
            Map<String, Object> beforeChargeTimeData2 = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", beforeDayStartTime)
                    .where("startTime", "<=", beforeDayEndTime)
                    .where("stopTime", ">=", startTime)
                    .where("stopTime", ">", 0)
                    .find();
            long beforeChargeTime2 = MapUtil.getLong(beforeChargeTimeData2, "chargeTime");
            if (beforeChargeTime2 > 0) {
                chargeTime_ms2 += beforeChargeTime2;
            }

            //查询当天跨天充电的充电时间
            Map<String, Object> afterChargeTimeData2 = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("startTime", "<=", endTime)
                    .where("stopTime", ">", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long afterChargeTime2 = MapUtil.getLong(afterChargeTimeData2, "chargeTime");
            if (afterChargeTime2 > 0) {
                chargeTime_ms2 += afterChargeTime2;
            }

            card_charge_duration = chargeTime_ms2 / 1000;
            data.put("card_charge_duration", card_charge_duration);
            //endregion

            //endregion
            BigDecimal card_adjustment_charge_amount = new BigDecimal(0);
            //region 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> card_adjustment_charge_amount_data = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(chargeCardConsumeAmount), 0) AS card_adjustment_charge_amount")
                    .alias("co")
                    .join(UserChargeCardEntity.getInstance().theTableName(), "ucc", "co.cardNumber = ucc.cardNumber")
                    .join(ChargeCardConfigEntity.getInstance().theTableName(), "ccc", "ccc.id = ucc.cardConfigId")
                    .where("co.organize_code", organize_code)
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 2)
                    .where("co.isTest", 0)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .where("ucc.end_time", ">=", startTime)
                    .whereIn("ccc.usageType", "'staff', 'partners'")
                    .find();
            card_adjustment_charge_amount = MapUtil.getBigDecimal(card_adjustment_charge_amount_data, "card_adjustment_charge_amount", 4, RoundingMode.HALF_UP, new BigDecimal(0));
            data.put("card_adjustment_charge_amount", card_adjustment_charge_amount);
            //endregion

            //region 充电卡消耗时间(秒)
            long card_charge_consume_time = ChargeOrderEntity.getInstance()
                    .where("organize_code", organize_code)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .sumGetLong("chargeCardConsumeTime");
            data.put("card_charge_consume_time", card_charge_consume_time);
            //endregion

            //region 累计充电时长
            long total_charge_time = pay_per_charge_duration + card_charge_duration;
            data.put("total_charge_time", total_charge_time);
            //endregion

            //充值金额
            BigDecimal recharge_amount = new BigDecimal(0);
            //充值退款金额
            BigDecimal recharge_refund_amount = new BigDecimal(0);

            //充电卡金额
            BigDecimal charge_card_amount = new BigDecimal(0);
            //充电卡退款金额
            BigDecimal charge_card_refund_amount = new BigDecimal(0);

            //当前全平台用户余额
            BigDecimal total_user_balance = new BigDecimal(0);

            //应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
            BigDecimal taxable_income = new BigDecimal(0);

            //region 充值订单数、充值金额、充值人数、充值退款订单数、充值退款订单金额

            //充值订单数次数、总充值金额
//            Map<String, Object> rechargeData = RechargeOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
//                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
//                            + ",IFNULL(SUM(pay_price),0) AS recharge_amount"
//                    )
//                    .where("organize_code", organize_code)
//                    .where("status", 2) //状态;1=未支付 2=已完成 -1=已取消，3=全额退款（已屏蔽），4=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> rechargeData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
                            + ",IFNULL(SUM(order_price),0) AS recharge_amount")
                    .where("organize_code", organize_code)
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充值金额
            recharge_amount = MapUtil.getBigDecimal(rechargeData, "recharge_amount");
            data.put("recharge_amount", recharge_amount);
            data.put("recharge_order_count", rechargeData.get("recharge_order_count"));//充值订单数
            data.put("recharge_users", rechargeData.get("recharge_users"));//充值人数

            //充值退款订单数、退款订单金额
//            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("organize_code", organize_code)
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> recharge_refund_data = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.organize_code", organize_code)
                    .where("co.product_type", "recharge")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            recharge_refund_amount = MapUtil.getBigDecimal(recharge_refund_data, "refund_amount");
            data.put("recharge_refund_amount", recharge_refund_amount);//充值退款订单金额
            data.put("recharge_refund_order_count", recharge_refund_data.get("refund_order_count"));//充值退款订单数

            //endregion

            //region 充电卡订单数、充电卡金额、充电卡人数、充电卡退款订单数、充电卡退款订单金额

//            Map<String, Object> chargeCardData = UserChargeCardOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
//                            + ",IFNULL(COUNT(DISTINCT uid),0) AS charge_card_users"
//                            + ",IFNULL(SUM(totalAmount),0) AS charge_card_amount"
//                    )
//                    .where("organize_code", organize_code)
//                    .where("status", 1) //状态;0=等待支付，1=支付成功，2=全额退款（已屏蔽），3=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardOrderData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS charge_card_users"
                            + ",IFNULL(SUM(order_price),0) AS charge_card_amount")
                    .where("organize_code", organize_code)
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("product_type", "charge_card")
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充电卡金额
            charge_card_amount = MapUtil.getBigDecimal(chargeCardOrderData, "charge_card_amount");
            data.put("charge_card_amount", charge_card_amount);
            data.put("charge_card_order_count", chargeCardOrderData.get("charge_card_order_count"));//充电卡订单数
            data.put("charge_card_users", chargeCardOrderData.get("charge_card_users"));//充电卡人数

            //充电卡退款订单数、退款订单金额
//            Map<String, Object> chargeCardRefundData = UserChargeCardRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("organize_code", organize_code)
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardRefundOrderData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.organize_code", organize_code)
                    .where("co.product_type", "charge_card")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            charge_card_refund_amount = MapUtil.getBigDecimal(chargeCardRefundOrderData, "refund_amount");
            data.put("charge_card_refund_amount", charge_card_refund_amount);//充电卡退款订单金额
            data.put("charge_card_refund_order_count", MapUtil.getInt(chargeCardRefundOrderData, "refund_order_count"));//充电卡退款订单数

            //endregion

            //region 当前全平台用户余额
            total_user_balance = UserSummaryEntity.getInstance()
                    .alias("a")
                    .join(UserTableName, "b", "a.uid = b.id")
                    .where("b.is_robot", 0)
                    .sumGetBigDecimal("a.balance", 2, RoundingMode.HALF_UP);
            data.put("total_user_balance", total_user_balance);
            //endregion

            //region 应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
            taxable_income = pay_per_charge_amount
                    .subtract(pay_per_adjustment_charge_amount)
                    .add(charge_card_amount)
                    .add(charge_card_refund_amount);
            data.put("taxable_income", taxable_income);
            //endregion

            //此充电桩运行中的充电端口
            if (total_socket > 0) {
                //region (次/插座)次数使用率（APR）：今天充电次数 / 全平台运行中的充电端口
                BigDecimal charge_count_use_rate = BigDecimal.valueOf(total_use_count)
                        .divide(BigDecimal.valueOf(total_socket), 6, RoundingMode.HALF_UP);
                data.put("charge_count_use_rate", charge_count_use_rate);
                //endregion

                //region (%)时长使用率（APR）：今天的充电时长「秒数」 / （不含私有桩）(充电桩A总端口今天运行时间 + 充电桩B总端口今天运行时间...)

                //端口运行时间 = 充电桩的运营时间 * 端口数
                List<Map<String, Object>> chargeStationEntityList = ChargeStationEntity.getInstance()
                        .field("id,online_time,totalSocket")
                        .where("organize_code", organize_code)
                        .where("status", 1)
                        .where("is_private", 0)
                        .where("is_restricted", 0)
                        .where("isTest", 0)
                        .order("id")
                        .select();
                //端口运行时间 = 充电桩的运营时间 * 端口数

                long summaryEndTime = Math.min(endTime, TimeUtil.getTimestamp());

                //端口运行时间
                long total_socket_run_time = 0;
                if (chargeStationEntityList != null) {
                    for (Map<String, Object> nd : chargeStationEntityList) {
                        long online_time = MapUtil.getLong(nd, "online_time");
                        int totalSocket_temp = MapUtil.getInt(nd, "totalSocket");

                        //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计
                        if (online_time > summaryEndTime) continue;

                        //表示充电桩的上线时间比统计开始时间还晚，应该以上线时间来进行计算,注意时间戳为毫秒级
                        if (online_time >= startTime) {
                            total_socket_run_time += (summaryEndTime - online_time) / 1000 * totalSocket_temp;
                        } else {
                            total_socket_run_time += (summaryEndTime - startTime) / 1000 * totalSocket_temp;
                        }
                    }
                }
                BigDecimal charge_time_use_rate = new BigDecimal(0);
                if (total_socket_run_time > 0) {
                    charge_time_use_rate = BigDecimal.valueOf(total_charge_time)
                            .divide(BigDecimal.valueOf(total_socket_run_time), 6, RoundingMode.HALF_UP);
                }
                data.put("total_socket_run_time", total_socket_run_time);
                data.put("charge_time_use_rate", charge_time_use_rate);
                //endregion

                //region [端口收益金额] 创造的收益(元/日/插座): (充值 + 充电卡 - 充值退款 - 充电卡退款) / 全平台运行中的充电端口
                BigDecimal net_income = new BigDecimal(0);
                net_income = (recharge_amount
                        .add(charge_card_amount)
                        .add(recharge_refund_amount)
                        .add(charge_card_refund_amount))
                        .divide(new BigDecimal(total_socket), 6, RoundingMode.HALF_UP);
                data.put("net_income", net_income);
                //endregion

                // region 端口消费金额(元/日/插座): (充电消费金额 + 充电卡消费金额 - 充电消费调整金额 - 充电卡消费调整金额) / 全平台运行中的充电端口
                BigDecimal socket_consumption = new BigDecimal(0);
                socket_consumption = (pay_per_charge_amount
                        .add(card_charge_amount)
                        .subtract(pay_per_adjustment_charge_amount)
                        .subtract(card_adjustment_charge_amount)
                ).divide(new BigDecimal(total_socket), 6, RoundingMode.HALF_UP);
                data.put("socket_consumption", socket_consumption);
                // endregion
            }

            //region idle用户数：充值或者购买了充电卡但是没有进行过任何充电的用户

            // 获取昨天的开始和结束时间（毫秒级别）
            long yesterdayStartTime = TimeUtil.getTime00(-1);
            long yesterdayEndTime = TimeUtil.getTime24(-1);

            // 查询有充值的用户
//            List<Object> r_user_ids = RechargeOrderEntity.getInstance()
//                    .field("uid")
//                    .where("organize_code", organize_code)
//                    .where("status", 2)
//                    .where("create_time", ">=", yesterdayStartTime)
//                    .where("create_time", "<=", yesterdayEndTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> r_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
                    .where("organize_code", organize_code)
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", yesterdayStartTime)
                    .where("create_time", "<=", yesterdayEndTime)
                    .group("uid")
                    .selectForArray("uid");

            // 查询购买了充电卡的用户
//            List<Object> c_user_ids = UserChargeCardOrderEntity.getInstance()
//                    .field("uid")
//                    .where("organize_code", organize_code)
//                    .where("status", 1)
//                    .where("create_time", ">=", yesterdayStartTime)
//                    .where("create_time", "<=", yesterdayEndTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> c_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
                    .where("organize_code", organize_code)
                    .where("product_type", "charge_card")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", yesterdayStartTime)
                    .where("create_time", "<=", yesterdayEndTime)
                    .group("uid")
                    .selectForArray("uid");

            // 合并 r_user_ids 和 c_user_ids
            Set<Object> combinedUserIds = new HashSet<>();
            combinedUserIds.addAll(r_user_ids);
            combinedUserIds.addAll(c_user_ids);

            //有充值或购买充电卡的用户数
            int payment_user_count = combinedUserIds.size();
            data.put("payment_user_count", payment_user_count);

            // 查询有充电的用户
            List<Object> chargedUserIds = ChargeOrderEntity.getInstance()
                    .field("uid")
                    .where("organize_code", organize_code)
                    .where("startTime", ">=", yesterdayStartTime)
                    .where("startTime", "<=", endTime)
                    .group("uid")
                    .selectForArray("uid");

            // 过滤出没有充电的用户
            chargedUserIds.forEach(combinedUserIds::remove);

            // 输出或处理没有充电的用户
            int idle_user_count = combinedUserIds.size();
            data.put("idle_user_count", idle_user_count);

            //endregion

            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance()
                    .where("organize_code", organize_code)
                    .where("date_time", startTime)
                    .exist()) {
                getInstance()
                        .where("organize_code", organize_code)
                        .where("date_time", startTime)
                        .update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.info(TAG, "%s 统计数据发生错误", date);
        }

        LogsUtil.info(TAG, "[%s] %s 统计数据 完成！", organize_code, date);
        return new SyncResult(0, "");
    }

    /**
     * 同步数据（非内嵌调用）
     */
    public void syncData(String organize_code, long date_timestamp, long end_time) {
        while (TimeUtil.toTimestamp00(date_timestamp) <= end_time) {
            syncTaskJob(organize_code, date_timestamp);
            date_timestamp += ECacheTime.DAY;
        }
    }

    /**
     * 开始同步任务（支持普通线程与RocketMQ两种方式）
     *
     * @param start_time  任务开始时间戳
     * @param end_time    任务结束时间戳
     * @param useRocketMQ 是否使用RocketMQ批量启动任务
     */
    public void startSyncTask(long start_time, long end_time, boolean useRocketMQ) {
        int page = 1;
        int limit = 100;

        // 标准化时间戳
        start_time = TimeUtil.toTimestamp00(start_time);
        end_time = TimeUtil.toTimestamp00(end_time);

        while (true) {
            // 分页查询组织列表
            List<Map<String, Object>> list = RBOrganizeEntity.getInstance()
                    .field("id,name,code,create_time")
                    .where("code", "!=", "")
                    .where("status", 1)
                    .page(page, limit)
                    .select();

            if (list == null || list.isEmpty()) break;

            page++;

            for (Map<String, Object> data : list) {
                String organize_name = MapUtil.getString(data, "name");
                String organize_code = MapUtil.getString(data, "code");
                long create_time = MapUtil.getLong(data, "create_time");

                if (create_time == 0 || !StringUtil.hasLength(organize_code)) continue;

                long task_start_time = Math.max(create_time, start_time);

                if (useRocketMQ) {
                    // RocketMQ批量任务
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("organize_code", organize_code);
                    rocketMQData.put("organize_name", organize_name);
                    rocketMQData.put("start_time", task_start_time);
                    rocketMQData.put("end_time", end_time);
                    XRocketMQ.getGlobal().pushSync(PlatformXRocketMQConsumerV2.TOPIC, "DaySummaryTaskV2", rocketMQData);
                } else {
                    // 普通线程任务
                    long finalEnd_time = end_time;
                    ThreadUtil.getInstance().execute(
                            String.format("[%s] %s", organize_name, TAG),
                            () -> syncData(organize_code, task_start_time, finalEnd_time)
                    );
                }
            }
        }
    }
}
