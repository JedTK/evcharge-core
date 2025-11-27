package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.bill.EMeterToCStationEntity;
import com.evcharge.entity.station.bill.ElectricityPowerSupplyBillEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.rocketmq.consumer.v2.ChargeStationXRocketMQConsumerV2;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 充电桩月汇总v2;
 *
 * @author : JED
 * @date : 2024-6-26
 */
@Getter
@Setter
public class ChargeStationMonthSummaryV2Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期，格式：yyyy-MM
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 累计的注册用户数
     */
    public int total_registered_users;
    /**
     * 充电网络主机数
     */
    public int total_host_device_count;
    /**
     * 充电终端数
     */
    public int total_charge_device_count;
    /**
     * 总的插座
     */
    public int total_socket;
    /**
     * 端口运行时间(秒)
     */
    public long total_socket_run_time;
    /**
     * 累计使用次数
     */
    public long total_use_count;
    /**
     * 累计安全预警次数
     */
    public long total_use_error;
    /**
     * 累计耗电量（度）
     */
    public BigDecimal total_power_consumption;
    /**
     * 累计充电时长(秒)
     */
    public long total_charge_time;
    /**
     * 计次充电的次数
     */
    public int pay_per_charge_count;
    /**
     * 计次充电的人数
     */
    public int pay_per_charge_users;
    /**
     * 计次充电的时长(秒)
     */
    public long pay_per_charge_duration;
    /**
     * 计次充电消费金额
     */
    public BigDecimal pay_per_charge_amount;
    /**
     * 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
     */
    public BigDecimal pay_per_adjustment_charge_amount;
    /**
     * 计次消费占比(用于计算充值摊分) = (计次消费金额 - 计次消费调整金额) / (所有站点计次消费金额 - 所有站点计次消费调整金额)
     */
    public BigDecimal pay_per_charge_ratio;
    /**
     * 充电卡充电的次数
     */
    public int card_charge_count;
    /**
     * 充电卡充电的人数
     */
    public int card_charge_users;
    /**
     * 充电卡充电的时长(秒)
     */
    public long card_charge_duration;
    /**
     * 充电卡消耗时间(秒)
     */
    public long card_charge_consume_time;
    /**
     * 充电卡消费金额
     */
    public BigDecimal card_charge_amount;
    /**
     * 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
     */
    public BigDecimal card_adjustment_charge_amount;
    /**
     * 充值订单数
     */
    public int recharge_order_count;
    /**
     * 充值金额（毛）
     */
    public BigDecimal recharge_amount;
    /**
     * 充值调整金额（用于摊分未知站点充值金额数）= 充值金额 * 消费金额占比率
     */
    public BigDecimal recharge_adjust_amount;
    /**
     * 充值人数
     */
    public int recharge_users;
    /**
     * 充值退款订单数
     */
    public int recharge_refund_order_count;
    /**
     * 充值退款订单金额
     */
    public BigDecimal recharge_refund_amount;
    /**
     * 充值退款调整金额
     */
    public BigDecimal recharge_refund_adjust_amount;
    /**
     * 充电卡订单数
     */
    public int charge_card_order_count;
    /**
     * 充电卡金额（毛）
     */
    public BigDecimal charge_card_amount;
    /**
     * 充电卡人数
     */
    public int charge_card_users;
    /**
     * 充电卡退款订单数
     */
    public int charge_card_refund_order_count;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal charge_card_refund_amount;
    /**
     * (次/插座)次数使用率（APR）：当月充电次数 / 全平台运行中的充电端口
     */
    public BigDecimal charge_count_use_rate;
    /**
     * (%)时长使用率（APR）：当月充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
     */
    public BigDecimal charge_time_use_rate;
    /**
     * 创造的收益(元/日/插座): 每日净收入的平均值
     */
    public BigDecimal net_income;
    /**
     * 累计创造的收益(元/插座): 每日净收入的合计
     */
    public BigDecimal total_net_income;
    /**
     * 有充值或购买充电卡的用户数
     */
    public int payment_user_count;
    /**
     * 有充值或购买充电卡但没有充电的用户数
     */
    public int idle_user_count;
    /**
     * 最小功率
     */
    public BigDecimal min_power;
    /**
     * 最大功率
     */
    public BigDecimal max_power;
    /**
     * 平均功率
     */
    public BigDecimal avg_power;
    /**
     * 电费
     */
    public BigDecimal electricity_fee;
    /**
     * 电费占比
     */
    public BigDecimal electricity_fee_ratio;
    /**
     * 额外数据，可以用于记录一些复杂计算出来的x系数数据等等
     */
    public String extra_data;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeStationMonthSummaryV2Entity getInstance() {
        return new ChargeStationMonthSummaryV2Entity();
    }

    private final static String TAG = "充电桩月汇总v2";

    /**
     * 同步数据
     */
    public SyncResult syncTaskJob(String CSId, long date_timestamp) {
        if (!StringUtil.hasLength(CSId)) return new SyncResult(2, "缺少充电站ID");
        // 充电桩信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().where("CSId", CSId).findEntity();
        if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(1, "");

        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM");
        LogsUtil.info(TAG, "[%s-%s][%s] 正在统计数据..."
                , chargeStationEntity.CSId
                , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                , date
        );

        try {
            //当月凌晨时间戳
            final long startTime = TimeUtil.toMonthBegin00(date_timestamp);
            //当月结束时间戳
            final long endTime = TimeUtil.toMonthEnd24(date_timestamp);
            //前一天凌晨时间戳
            final long beforeDayStartTime = startTime - ECacheTime.DAY;
            //前一天结束时间戳
            final long beforeDayEndTime = TimeUtil.toDayEnd24(beforeDayStartTime);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("date", date);
            data.put("date_time", startTime);
            data.put("CSId", CSId);
            data.put("organize_code", chargeStationEntity.organize_code);

            Map<String, Object> extra_data = new LinkedHashMap<>();

            String UserTableName = UserEntity.getInstance().theTableName();
            String DeviceTableName = DeviceEntity.getInstance().theTableName();

            //region 累计的注册用户数
            long total_registered_users = UserEntity.getInstance()
                    .where("cs_id", CSId)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .count("1");
            data.put("total_registered_users", total_registered_users);
            //endregion

            //region 充电主机数、充电终端数、总的插座
            //充电网络主机数
            int total_host_device_count = 0;
            int total_charge_device_count = 0;
            int total_socket = 0;
            //充电主机数
            total_host_device_count = DeviceEntity.getInstance()
                    .where("CSId", CSId)
                    .where("isHost", 1)
                    .count("1");

            //充电终端数
            total_charge_device_count = DeviceEntity.getInstance()
                    .where("CSId", CSId)
                    .where("isHost", 0)
                    .count("1");

            //总电位数
            total_socket = DeviceSocketEntity.getInstance()
                    .alias("ds")
                    .join(DeviceTableName, "d", "ds.deviceId = d.id")
                    .where("d.CSId", CSId)
                    .count("1");
            data.put("total_host_device_count", total_host_device_count);
            data.put("total_charge_device_count", total_charge_device_count);
            data.put("total_socket", total_socket);
            //endregion

            //累计使用次数
            long total_use_count = 0;
            //region 累计使用次数、累计耗电量（度）
            Map<String, Object> sumCount = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS total_use_count,IFNULL(SUM(powerConsumption),0) AS total_power_consumption")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            total_use_count = MapUtil.getLong(sumCount, "total_use_count");
            //累计使用次数
            data.put("total_use_count", total_use_count);
            //累计耗电量（度）
            data.put("total_power_consumption", sumCount.get("total_power_consumption"));
            //endregion

            //region累计安全预警次数
            long total_use_error = ChargeOrderEntity.getInstance()
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .whereIn("stopReasonCode", "-1,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .countGetLong("1");
            data.put("total_use_error", total_use_error);
            //endregion

            //端口运行时间（秒级）
            long total_socket_run_time = 0;

            BigDecimal pay_per_charge_amount;
            //region 计次充电的次数、计次充电的人数、计次充电消费金额、计次充电的时长
            Map<String, Object> pay_per_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS pay_per_charge_count"
                            + ",COUNT(DISTINCT uid) AS pay_per_charge_users"
                            + ",IFNULL(SUM(totalAmount),0) AS pay_per_charge_amount")
                    .where("CSId", CSId)
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
            data.put("pay_per_charge_amount", pay_per_charge_amount);

            //计次充电的时长(秒)
            long pay_per_charge_duration = 0;
            //region 计次充电的时长（考虑跨天的情况）

            //计算当天不跨天充电的充电时间
            long chargeTime_ms1 = 0;
            Map<String, Object> chargeTimeData1 = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("CSId", CSId)
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
                    .where("CSId", CSId)
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
                    .where("CSId", CSId)
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
            BigDecimal pay_per_adjustment_charge_amount;
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
            pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(pay_per_adjustment_charge_amount_data
                    , "pay_per_adjustment_charge_amount"
                    , 4
                    , RoundingMode.HALF_UP
                    , new BigDecimal(0));
            data.put("pay_per_adjustment_charge_amount", pay_per_adjustment_charge_amount);
            //endregion

            BigDecimal card_charge_amount;
            //region 充电卡充电的次数、充电卡充电的人数、充电卡消费金额、充电卡充电的时长
            Map<String, Object> card_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS card_charge_count"
                            + ",COUNT(DISTINCT uid) AS card_charge_users"
                            + ",IFNULL(SUM(chargeCardConsumeAmount),0) AS card_charge_amount")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime).find();

            //充电卡充电的次数
            data.put("card_charge_count", card_charge_data.get("card_charge_count"));
            //充电卡充电的人数
            data.put("card_charge_users", card_charge_data.get("card_charge_users"));
            //充电卡消费金额
            card_charge_amount = MapUtil.getBigDecimal(card_charge_data, "card_charge_amount", 4, RoundingMode.HALF_UP);
            data.put("card_charge_amount", card_charge_amount);

            //充电卡充电的时长
            long card_charge_duration = 0;
            //region 充电卡充电的时长（考虑跨天的情况）

            //计算当天不跨天充电的充电时间
            long chargeTime_ms2 = 0;
            Map<String, Object> chargeTimeData2 = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("CSId", CSId)
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
                    .where("CSId", CSId)
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
                    .where("CSId", CSId)
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
            BigDecimal card_adjustment_charge_amount;
            //region 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> card_adjustment_charge_amount_data = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(chargeCardConsumeAmount), 0) AS card_adjustment_charge_amount")
                    .alias("co")
                    .join(UserChargeCardEntity.getInstance().theTableName(), "ucc", "co.cardNumber = ucc.cardNumber")
                    .join(ChargeCardConfigEntity.getInstance().theTableName(), "ccc", "ccc.id = ucc.cardConfigId")
                    .where("co.CSId", CSId)
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
            BigDecimal recharge_amount;
            //充值调整金额（用于摊分未知站点充值金额数）= 充值金额 * 消费金额占比率
            BigDecimal recharge_adjust_amount;
            //充值退款金额
            BigDecimal recharge_refund_amount;
            //充值退款调整金额
            BigDecimal recharge_refund_adjust_amount;
            //充电卡金额
            BigDecimal charge_card_amount;
            //充电卡退款金额
            BigDecimal charge_card_refund_amount;
            //应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
            BigDecimal taxable_income = new BigDecimal(0);
            //电费
            BigDecimal electricity_fee;
            //电费调整金额(一表多站的情况) = 电费 * 消费占比
            BigDecimal electricity_fee_ratio = BigDecimal.ONE;

            //region 充值订单数、充值金额、充值人数、充值退款订单数、充值退款订单金额

            //充值订单数次数、总充值金额
//            Map<String, Object> rechargeData = RechargeOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
//                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
//                            + ",IFNULL(SUM(pay_price),0) AS recharge_amount")
//                    .where("CSId", CSId)
//                    .where("status", 2) //状态;1=未支付 2=已完成 -1=已取消，3=全额退款（已屏蔽），4=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> rechargeData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
                            + ",IFNULL(SUM(order_price),0) AS recharge_amount")
                    .where("CSId", CSId)
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
//                    .where("CSId", CSId)
//                    .where("refund_status", 2) //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> rechargeRefundData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.CSId", CSId)
                    .where("co.product_type", "recharge")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            recharge_refund_amount = MapUtil.getBigDecimal(rechargeRefundData, "refund_amount");
            data.put("recharge_refund_amount", recharge_refund_amount);//充值退款订单金额
            data.put("recharge_refund_order_count", rechargeRefundData.get("refund_order_count"));//充值退款订单数

            //endregion

            // region 充值调整金额 = 无站点充值金额 * 计次消费占比  |  充值退款调整金额 = 无站点充值退款金额 * 计次消费占比

            //region 无站点 - 充值金额、充值退款订单金额
            // 无站点-充值金额
//            BigDecimal none_cs_recharge_amount = RechargeOrderEntity.getInstance()
//                    .where("CSId", 0)
//                    .where("status", 2) //状态;1=未支付 2=已完成 -1=已取消，3=全额退款（已屏蔽），4=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .sumGetBigDecimal("pay_price");
            BigDecimal none_cs_recharge_amount = ConsumeOrdersEntity.getInstance()
                    .whereBuilder("", "(","CSId", "=", "", "")
                    .whereBuilder("OR", "","CSId", "=", "0", ")")
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .sumGetBigDecimal("order_price");

            // 无站点-退款订单金额
//            BigDecimal none_cs_recharge_refund_amount = RechargeRefundOrderEntity.getInstance()
//                    .where("CSId", 0)
//                    .where("refund_status", 2) //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .sumGetBigDecimal("refund_amount");
            BigDecimal none_cs_recharge_refund_amount = ConsumeOrderRefundsEntity.getInstance()
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .whereBuilder("", "(","co.CSId", "=", "", "")
                    .whereBuilder("OR", "","co.CSId", "=", "0", ")")
                    .where("co.product_type", "recharge")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .sumGetBigDecimal("refund_amount");
            //endregion

            // region 计算所有站点 - 计次充电消费总金额 、计次充电消费总调整金额
            // 所有站点-计次充电消费金额
            BigDecimal all_cs_pay_per_charge_amount = ChargeOrderEntity.getInstance()
                    .where("organize_code", chargeStationEntity.organize_code)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .sumGetBigDecimal("totalAmount");
            // 所有站点-计次充电消费调整金额（计算内部用户、合作用户之类的金额）
            BigDecimal all_cs_pay_per_adjustment_charge_amount = ChargeOrderEntity.getInstance()
                    .alias("co")
                    .join(UserTableName, "u", "u.id = co.uid")
                    .where("co.organize_code", chargeStationEntity.organize_code)
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("co.isTest", 0)
                    .where("u.is_robot", 3)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .sumGetBigDecimal("totalAmount");
            all_cs_pay_per_charge_amount = all_cs_pay_per_charge_amount.subtract(all_cs_pay_per_adjustment_charge_amount);
            //endregion

            // 计次消费占比(用于计算充值摊分) = (计次消费金额 - 计次消费调整金额) / (所有站点计次消费金额 - 所有站点计次消费调整金额)
            BigDecimal pay_per_charge_ratio = BigDecimal.ZERO;
            if (all_cs_pay_per_charge_amount.compareTo(BigDecimal.ZERO) > 0) {
                pay_per_charge_ratio = (pay_per_charge_amount.subtract(pay_per_adjustment_charge_amount))
                        .divide(all_cs_pay_per_charge_amount // 已经减去调整金额
                                , 8, RoundingMode.HALF_UP);
            }

            data.put("pay_per_charge_ratio", pay_per_charge_ratio);
            extra_data.put("none_cs_recharge_amount", none_cs_recharge_amount);
            extra_data.put("none_cs_recharge_refund_amount", none_cs_recharge_refund_amount);
            extra_data.put("all_cs_pay_per_charge_amount", all_cs_pay_per_charge_amount);

            LogsUtil.info(TAG, "[%s-%s][%s] 计次消费比率：%s %s"
                    , chargeStationEntity.CSId
                    , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                    , date
                    , pay_per_charge_ratio
                    , new JSONObject(extra_data).toJSONString()
            );

            // 充值调整金额 = 无站点充值金额 * 计次消费占比  |  充值退款调整金额 = 无站点充值退款金额 * 计次消费占比
            recharge_adjust_amount = none_cs_recharge_amount.multiply(pay_per_charge_ratio);
            data.put("recharge_adjust_amount", recharge_adjust_amount);

            // 充值退款调整金额 = 无站点充值退款金额 * 计次消费占比
            recharge_refund_adjust_amount = none_cs_recharge_refund_amount.multiply(pay_per_charge_ratio);
            data.put("recharge_refund_adjust_amount", recharge_refund_adjust_amount);
            // endregion

            //region 充电卡订单数、充电卡金额、充电卡人数、充电卡退款订单数、充电卡退款订单金额

//            Map<String, Object> chargeCardOrderData = UserChargeCardOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
//                            + ",IFNULL(COUNT(DISTINCT uid),0) AS charge_card_users"
//                            + ",IFNULL(SUM(totalAmount),0) AS charge_card_amount")
//                    .where("CSId", CSId)
//                    .where("status", 1) //状态;0=等待支付，1=支付成功，2=全额退款（已屏蔽），3=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardOrderData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS charge_card_users"
                            + ",IFNULL(SUM(order_price),0) AS charge_card_amount")
                    .where("CSId", CSId)
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
//            Map<String, Object> chargeCardRefundOrderData = UserChargeCardRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("CSId", CSId)
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardRefundOrderData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(),"co","cor.order_sn = co.order_sn")
                    .where("co.CSId", CSId)
                    .where("co.product_type", "charge_card")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            charge_card_refund_amount = MapUtil.getBigDecimal(chargeCardRefundOrderData, "refund_amount");
            data.put("charge_card_refund_amount", charge_card_refund_amount);//充电卡退款订单金额
            data.put("charge_card_refund_order_count", MapUtil.getInt(chargeCardRefundOrderData, "refund_order_count"));//充电卡退款订单数

            //endregion

            // region 电费

            // 查询当月电费单
            ElectricityPowerSupplyBillEntity billEntity = ElectricityPowerSupplyBillEntity.getInstance().getWithCSId(CSId, startTime);
            if (billEntity != null && billEntity.id != 0) {
                LogsUtil.info(TAG, "[%s-%s][%s] 电费账单信息：%s"
                        , chargeStationEntity.CSId
                        , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                        , date
                        , JSONObject.toJSONString(billEntity)
                );

                electricity_fee = new BigDecimal(billEntity.electricity_fee);
                // 检查是否存在一表多站点的情况
                String[] meterCSIdList = EMeterToCStationEntity.getInstance()
                        .field("cs_id")
                        .where("meter_id", billEntity.meter_id)
                        .selectForStringArray("cs_id");
                if (meterCSIdList.length > 1) {
                    // region 计算该电表的站点 - 计次充电消费总金额 、计次充电消费总调整金额、充电卡消费总金额、充电卡消费总调整金额
                    // 所有站点-计次充电消费金额
                    BigDecimal meter_cs_pay_per_charge_amount = ChargeOrderEntity.getInstance()
                            .whereIn("CSId", meterCSIdList)
                            .where("status", 2)
                            .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                            .where("isTest", 0)
                            .where("create_time", ">=", startTime)
                            .where("create_time", "<=", endTime)
                            .sumGetBigDecimal("totalAmount");
                    // 所有站点-计次充电消费调整金额（计算内部用户、合作用户之类的金额）
                    BigDecimal meter_cs_pay_per_adjustment_charge_amount = ChargeOrderEntity.getInstance()
                            .alias("co")
                            .join(UserTableName, "u", "u.id = co.uid")
                            .whereIn("CSId", meterCSIdList)
                            .where("co.status", 2)
                            .where("co.paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                            .where("co.isTest", 0)
                            .where("u.is_robot", 3)
                            .where("co.create_time", ">=", startTime)
                            .where("co.create_time", "<=", endTime)
                            .sumGetBigDecimal("totalAmount");
                    meter_cs_pay_per_charge_amount = meter_cs_pay_per_charge_amount.subtract(meter_cs_pay_per_adjustment_charge_amount);

                    // 所有站点-充电卡充电的次数、充电卡充电的人数、充电卡消费金额、充电卡充电的时长
                    BigDecimal meter_cs_card_charge_amount = ChargeOrderEntity.getInstance()
                            .whereIn("CSId", meterCSIdList)
                            .where("status", 2)
                            .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                            .where("isTest", 0)
                            .where("create_time", ">=", startTime)
                            .where("create_time", "<=", endTime)
                            .sumGetBigDecimal("chargeCardConsumeAmount");

                    // 所有站点-充电卡消费调整金额（计算内部用户、合作用户之类的金额）
                    BigDecimal meter_cs_card_adjustment_charge_amount = ChargeOrderEntity.getInstance()
                            .field("IFNULL(SUM(chargeCardConsumeAmount), 0) AS card_adjustment_charge_amount")
                            .alias("co")
                            .join(UserChargeCardEntity.getInstance().theTableName(), "ucc", "co.cardNumber = ucc.cardNumber")
                            .join(ChargeCardConfigEntity.getInstance().theTableName(), "ccc", "ccc.id = ucc.cardConfigId")
                            .whereIn("CSId", meterCSIdList)
                            .where("co.status", 2)
                            .where("co.paymentTypeId", 2)
                            .where("co.isTest", 0)
                            .where("co.create_time", ">=", startTime)
                            .where("co.create_time", "<=", endTime)
                            .where("ucc.end_time", ">=", startTime)
                            .whereIn("ccc.usageType", "'staff', 'partners'")
                            .sumGetBigDecimal("chargeCardConsumeAmount");
                    meter_cs_card_charge_amount = meter_cs_card_charge_amount.subtract(meter_cs_card_adjustment_charge_amount);
                    //endregion
                    // 同一个电表下充电消费比率 = 此站点计次消费金额 - 此站点计次调整金额 + 此站点充电卡消费金额 - 此站点充电卡消费调整金额
                    if (meter_cs_pay_per_charge_amount.compareTo(BigDecimal.ZERO) > 0 || meter_cs_card_charge_amount.compareTo(BigDecimal.ZERO) > 0) {
                        electricity_fee_ratio =
                                (pay_per_charge_amount.subtract(pay_per_adjustment_charge_amount)
                                        .add(card_charge_amount.subtract(card_adjustment_charge_amount)))
                                        .divide(meter_cs_pay_per_charge_amount.add(meter_cs_card_charge_amount) // 已经减去调整金额
                                                , 8, RoundingMode.HALF_UP);
                    }

                    // 电费调整金额(一表多站的情况) = 电费 * 充电消费占比
                    electricity_fee = electricity_fee.multiply(electricity_fee_ratio);
                    extra_data.put("meter_cs_pay_per_charge_amount", meter_cs_pay_per_charge_amount);
                    extra_data.put("meter_cs_card_charge_amount", meter_cs_card_charge_amount);
                }
                data.put("electricity_fee", electricity_fee);
                data.put("electricity_fee_ratio", electricity_fee_ratio);
            } else {
                LogsUtil.info(TAG, "[%s-%s][%s] 缺少当月电费账单！! time=%s"
                        , chargeStationEntity.CSId
                        , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                        , date
                        , startTime
                );
            }

            // endregion

            //此充电桩运行中的充电端口
            if (total_socket > 0) {
                //region (次/插座)次数使用率（APR）：所有充电次数 / 全平台运行中的充电端口（不含私有桩）
                BigDecimal charge_count_use_rate = BigDecimal.valueOf(total_use_count)
                        .divide(BigDecimal.valueOf(total_socket), 6, RoundingMode.HALF_UP);
                data.put("charge_count_use_rate", charge_count_use_rate);
                //endregion

                long lastTime = Math.min(endTime, TimeUtil.getTimestamp());

                //region (%)时长使用率（APR）：所有充电时长「秒数」 / (充电桩的运营时间 * 端口数)
                //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计
                if (chargeStationEntity.online_time <= lastTime) {
                    //表示充电桩的上线时间比统计开始时间还晚，应该以上线时间来进行计算,注意时间戳为毫秒级
                    if (chargeStationEntity.online_time >= startTime) {
                        total_socket_run_time += (lastTime - chargeStationEntity.online_time) / 1000 * total_socket;
                    } else {
                        total_socket_run_time += (lastTime - startTime) / 1000 * total_socket;
                    }
                }
                //  时长使用率（APR） = 所有充电时长「秒数」 / 端口运行时间「秒数」
                BigDecimal charge_time_use_rate = new BigDecimal(0);
                if (total_charge_time > 0 && total_socket_run_time > 0) {
                    charge_time_use_rate = new BigDecimal(total_charge_time)
                            .divide(new BigDecimal(total_socket_run_time), 6, RoundingMode.HALF_UP);
                }
                data.put("total_socket_run_time", total_socket_run_time);
                data.put("charge_time_use_rate", charge_time_use_rate);
                //endregion

                //region 端口平均收益金额(元/插座) | 端口平均消费金额(元/插座)
                Map<String, Object> daySummaryData = ChargeStationDaySummaryV2Entity.getInstance()
                        .field("IFNULL(AVG(net_income),0) AS net_income,IFNULL(AVG(socket_consumption),0) AS socket_consumption")
                        .where("CSId", CSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", lastTime)
                        .find();
                data.put("net_income", MapUtil.getBigDecimal(daySummaryData, "net_income"));
                data.put("socket_consumption", MapUtil.getBigDecimal(daySummaryData, "socket_consumption"));
                //endregion

                //region 端口累计收益金额(元/月/插座) | 端口累计消费金额(元/月/插座)
                Map<String, Object> totalNetIncomeData = ChargeStationDaySummaryV2Entity.getInstance()
                        .field("IFNULL(SUM(net_income),0) AS total_net_income,IFNULL(SUM(socket_consumption),0) AS total_socket_consumption")
                        .where("CSId", CSId)
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", lastTime)
                        .find();
                data.put("total_net_income", MapUtil.getBigDecimal(totalNetIncomeData, "total_net_income"));
                data.put("total_socket_consumption", MapUtil.getBigDecimal(totalNetIncomeData, "total_socket_consumption"));
                //endregion
            }

            //region idle用户数：充值或者购买了充电卡但是没有进行过任何充电的用户

            // 查询有充值的用户
//            List<Object> r_user_ids = RechargeOrderEntity.getInstance()
//                    .field("uid")
//                    .where("CSId", CSId)
//                    .where("status", 2)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> r_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
                    .where("CSId", CSId)
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .group("uid")
                    .selectForArray("uid");

            // 查询购买了充电卡的用户
//            List<Object> c_user_ids = UserChargeCardOrderEntity.getInstance()
//                    .field("uid")
//                    .where("CSId", CSId)
//                    .where("status", 1)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> c_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
                    .where("CSId", CSId)
                    .where("product_type", "charge_card")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
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
                    .where("CSId", CSId)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .group("uid")
                    .selectForArray("uid");

            // 过滤出没有充电的用户
            chargedUserIds.forEach(combinedUserIds::remove);
            // 输出或处理没有充电的用户
            int idle_user_count = combinedUserIds.size();

            data.put("idle_user_count", idle_user_count);
            //endregion

            //region 最小功率、最大功率、平均功率
            Map<String, Object> powerComplexData = ChargeOrderEntity.getInstance()
                    .field("IFNULL(MAX(maxPower),0) as maxPower ,IFNULL(MIN(maxPower),0) as minPower,IFNULL(AVG(maxPower),0) as avgPower")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //最小功率
            data.put("min_power", powerComplexData.get("minPower"));
            //最大功率
            data.put("max_power", powerComplexData.get("maxPower"));
            //平均功率
            data.put("avg_power", powerComplexData.get("avgPower"));
            //endregion

            data.put("extra_data", new JSONObject(extra_data).toJSONString());
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("CSId", CSId).where("date_time", startTime).exist()) {
                getInstance().where("CSId", CSId).where("date_time", startTime).update(data);
            } else {
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s-%s][%s] 汇总数据发生错误"
                    , chargeStationEntity.CSId
                    , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                    , date
            );
        }

        LogsUtil.info(TAG, "[%s-%s][%s] 统计数据 完成！"
                , chargeStationEntity.CSId
                , StringUtil.removeLineBreaksAndSpaces(chargeStationEntity.name)
                , date
        );
        return new SyncResult(0, "");
    }

    /**
     * 同步数据（迭代调用）
     */
    public void syncData(String CSId, long date_timestamp, long end_time) {
        long startTime = TimeUtil.toMonthBegin00(date_timestamp);
        long month = TimeUtil.toMonthBegin00(end_time);

        // 使用循环代替递归
        while (startTime <= month) {
            syncTaskJob(CSId, startTime);
            startTime = TimeUtil.getAddMonthTimestamp(startTime, 1);  // 增加一个月
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

        // 标准化时间戳（使用月初时间）
        start_time = TimeUtil.toMonthBegin00(start_time);
        end_time = TimeUtil.toMonthBegin00(end_time);

        while (true) {
            // 分页查询充电站列表
            List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                    .field("id,CSId,name,online_time")
                    .where("status", 1)
                    .where("online_time", ">", 0)
                    .where("isTest", 0) // 如果适用非测试条件
                    .page(page, limit)
                    .select();

            if (list == null || list.isEmpty()) break;

            page++;

            for (Map<String, Object> nd : list) {
                String CSId = MapUtil.getString(nd, "CSId");
                String name = StringUtil.removeLineBreaksAndSpaces(MapUtil.getString(nd, "name"));
                long online_time = TimeUtil.toMonthBegin00(MapUtil.getLong(nd, "online_time"));
                long task_start_time = Math.max(online_time, start_time);

                if (useRocketMQ) {
                    // RocketMQ批量任务
                    JSONObject rocketMQData = new JSONObject();
                    rocketMQData.put("CSId", CSId);
                    rocketMQData.put("name", name);
                    rocketMQData.put("start_time", task_start_time);
                    rocketMQData.put("end_time", end_time);
                    XRocketMQ.getGlobal().pushSync(ChargeStationXRocketMQConsumerV2.TOPIC, "MonthSummaryTaskV2", rocketMQData);
                } else {
                    // 普通线程任务
                    long finalEnd_time = end_time;
                    ThreadUtil.getInstance().execute(
                            String.format("[%s-%s] %s", CSId, name, TAG),
                            () -> syncData(CSId, task_start_time, finalEnd_time)
                    );
                }
            }
        }
    }
}
