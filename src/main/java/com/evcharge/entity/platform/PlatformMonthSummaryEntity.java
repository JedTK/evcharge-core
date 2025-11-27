package com.evcharge.entity.platform;

import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSourceInfoEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 平台月汇总;
 *
 * @author : JED
 * @date : 2023-8-14
 */
public class PlatformMonthSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 累计的注册用户数
     */
    public long total_registered_users;
    /**
     * 充电桩数量
     */
    public int chargeStationCount;
    /**
     * 充电网络主机数
     */
    public int totalHostDeviceCount;
    /**
     * 充电终端数
     */
    public int totalChargeDeviceCount;
    /**
     * 总端口数
     */
    public int totalSocket;
    /**
     * 端口运行时间
     */
    public long totalSocketRunTime;
    /**
     * 累计使用次数
     */
    public long totalUseCount;
    /**
     * 累计安全预警次数
     */
    public long totalUseError;
    /**
     * 累计耗电量（度）
     */
    public BigDecimal totalPowerConsumption;
    /**
     * 累计充电时长
     */
    public long totalChargeTime;
    /**
     * 当前用户余额
     */
    public BigDecimal totalUserBalance;
    /**
     * 计次充电的次数
     */
    public int pay_per_charge_count;
    /**
     * 计次充电的人数
     */
    public int pay_per_charge_users;
    /**
     * 计次充电的时长
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
     * 充电卡充电的次数
     */
    public int card_charge_count;
    /**
     * 充电卡充电的人数
     */
    public int card_charge_users;
    /**
     * 充电卡充电的时长
     */
    public long card_charge_duration;
    /**
     * 充电卡消费金额
     */
    public BigDecimal card_charge_amount;
    /**
     * 充电卡消费调整金额（计算员工卡、合作卡之类的金额）
     */
    public BigDecimal card_adjustment_charge_amount;

    /**
     * 充值订单数
     */
    public int rechargeOrderCount;
    /**
     * 充值金额
     */
    public BigDecimal rechargeAmount;
    /**
     * 充值人数
     */
    public int rechargeUsers;
    /**
     * 充值退款订单数
     */
    public int rechargeRefundOrderCount;
    /**
     * 充值退款订单金额
     */
    public BigDecimal rechargeRefundAmount;
    /**
     * 充电卡订单数
     */
    public int chargeCardOrderCount;
    /**
     * 充电卡金额
     */
    public BigDecimal chargeCardAmount;
    /**
     * 充电卡人数
     */
    public int chargeCardUsers;
    /**
     * 充电卡退款订单数
     */
    public int chargeCardRefundOrderCount;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal chargeCardRefundAmount;
    /**
     * (次/插座)次数使用率（APR）：当月充电次数 / 全平台运行中的充电端口
     */
    public BigDecimal chargeCountUseRate;
    /**
     * (%)时长使用率（APR）：当月充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
     */
    public BigDecimal chargeTimeUseRate;
    /**
     * 创造的收益(元/日/插座): 每日净收入的平均值
     */
    public BigDecimal netIncome;
    /**
     * 累计创造的收益(元/插座): 每日净收入的合计
     */
    public BigDecimal totalNetIncome;
    /**
     * 有充值或购买充电卡的用户数
     */
    public int paymentUserCount;
    /**
     * 有充值或购买充电卡但没有充电的用户数
     */
    public int idleUserCount;
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
    public static PlatformMonthSummaryEntity getInstance() {
        return new PlatformMonthSummaryEntity();
    }

    /**
     * 同步数据
     *
     * @param date_timestamp 统计时间戳（这里泛指按月统计）
     * @return 同步结果
     */
    public SyncResult syncData(long date_timestamp) {
        if (date_timestamp == 0) {
            date_timestamp = ChargeStationEntity.getInstance().getEarliestOnlineTime();
            if (date_timestamp == 0) date_timestamp = TimeUtil.getTimestamp();
        }
        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM");
        try {
            //当月凌晨时间戳
            long startTime = TimeUtil.toMonthBegin00(date_timestamp);
            //当月结束时间戳
            long endTime = TimeUtil.toMonthEnd24(date_timestamp);
            //前一天凌晨时间戳
            long beforeDayStartTime = startTime - ECacheTime.DAY;
            //前一天结束时间戳
            long beforeDayEndTime = TimeUtil.toDayEnd24(beforeDayStartTime);

            Map<String, Object> data = new LinkedHashMap<>();

            //region 累计的注册用户数
            long total_registered_users = UserSourceInfoEntity.getInstance()
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .count();
            data.put("total_registered_users", total_registered_users);
            //endregion

            //region 充电桩数量

            String[] CSIds = ChargeStationEntity.getInstance()
                    .where("status", ">", 0)
                    .where("online_time", ">", 0)
                    .where("online_time", "<=", endTime)
                    .where("is_private", 0)
                    .where("is_restricted", 0)
                    .where("isTest", 0)
                    .selectForStringArray("id");
            data.put("chargeStationCount", CSIds.length);

            //endregion

            //region 充电网络主机数、充电终端数、总电位数
            //充电网络主机数
            long totalHostDeviceCount = 0;
            //充电终端数
            long totalChargeDeviceCount = 0;
            //总电位数（不含私有桩）
            int totalSocket = 0;
            if (CSIds.length > 0) {
                //统计主机数量
                totalHostDeviceCount = DeviceEntity.getInstance()
                        .whereIn("CSId", CSIds)
                        .where("isHost", 1)
                        .count("1");

                //统计从机数量
                String[] deviceIds = DeviceEntity.getInstance()
                        .whereIn("CSId", CSIds)
                        .where("isHost", 0)//主机：0=否，1=是
                        .selectForStringArray("id");
                if (deviceIds.length > 0) {
                    totalChargeDeviceCount = deviceIds.length;

                    //统计从机充电端口数量
                    totalSocket = DeviceSocketEntity.getInstance()
                            .whereIn("deviceId", deviceIds)
                            .count();
                }
            }
            data.put("totalHostDeviceCount", totalHostDeviceCount);
            data.put("totalChargeDeviceCount", totalChargeDeviceCount);
            data.put("totalSocket", totalSocket);
            //endregion

            //累计使用次数
            long totalUseCount = 0;
            //region 累计使用次数、累计耗电量（度）
            if (totalUseCount == 0) {
                Map<String, Object> sumcount = ChargeOrderEntity.getInstance()
                        .field("COUNT(1) AS totalUseCount,IFNULL(SUM(powerConsumption),0) AS totalPowerConsumption")
                        .where("status", 2)
                        .where("isTest", 0)
                        .where("create_time", ">=", startTime)
                        .where("create_time", "<=", endTime)
                        .find();
                totalUseCount = MapUtil.getLong(sumcount, "totalUseCount");
                //累计使用次数
                data.put("totalUseCount", totalUseCount);
                //累计耗电量（度）
                data.put("totalPowerConsumption", sumcount.get("totalPowerConsumption"));
            }
            //endregion

            //region累计安全预警次数
            long totalUseError = ChargeOrderEntity.getInstance()
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .whereIn("stopReasonCode", "-1,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                    .countGetLong("1");
            data.put("totalUseError", totalUseError);
            //endregion

            //累计充电时长
            long totalChargeTime = 0;
//            //region 累计充电时长(考虑跨天的情况)
//            if (totalChargeTime == 0) {
//                //计算当天不跨天充电的充电时间
//                long chargeTime_ms = 0;
//                Map<String, Object> chargeTimeData = ChargeOrderEntity.getInstance()
//                        .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
//                        .where("status", 2)
//                        .where("isTest", 0)
//                        .where("startTime", ">", startTime)
//                        .where("stopTime", "<=", endTime)
//                        .where("stopTime", ">", 0)
//                        .find();
//                long currentChargeTime = MapUtil.getLong(chargeTimeData, "chargeTime");
//                if (currentChargeTime > 0) {
//                    chargeTime_ms += currentChargeTime;
////                    LogsUtil.info("平台月汇总", "%s - 当天 总充电时间 %s 毫秒", date, currentChargeTime);
//                }
//
//                //查询前一天跨天充电的充电时间
//                Map<String, Object> beforeChargeTimeData = ChargeOrderEntity.getInstance()
//                        .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
//                        .where("status", 2)
//                        .where("isTest", 0)
//                        .where("startTime", ">", beforeDayStartTime)
//                        .where("startTime", "<=", beforeDayEndTime)
//                        .where("stopTime", ">=", startTime)
//                        .where("stopTime", ">", 0)
//                        .find();
//                long beforeChargeTime = MapUtil.getLong(beforeChargeTimeData, "chargeTime");
//                if (beforeChargeTime > 0) {
//                    chargeTime_ms += beforeChargeTime;
////                    LogsUtil.info("平台月汇总", "%s - 前一天跨天 总充电时间 %s 毫秒", date, beforeChargeTime);
//                }
//
//                //查询当天跨天充电的充电时间
//                Map<String, Object> afterChargeTimeData = ChargeOrderEntity.getInstance()
//                        .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
//                        .where("status", 2)
//                        .where("isTest", 0)
//                        .where("startTime", ">", startTime)
//                        .where("startTime", "<=", endTime)
//                        .where("stopTime", ">", endTime)
//                        .where("stopTime", ">", 0)
//                        .find();
//                long afterChargeTime = MapUtil.getLong(afterChargeTimeData, "chargeTime");
//                if (afterChargeTime > 0) {
//                    chargeTime_ms += afterChargeTime;
////                    LogsUtil.info("平台月汇总", "%s - 当天跨天 总充电时间 %s 毫秒", date, afterChargeTime);
//                }
//
//                //充电时长
//                totalChargeTime = chargeTime_ms / 1000;
//                data.put("totalChargeTime", totalChargeTime);
//            }
//            //endregion

            BigDecimal pay_per_charge_amount = new BigDecimal(0);
            //region 计次充电的次数、计次充电的人数、计次充电消费金额、计次充电的时长

            Map<String, Object> pay_per_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS pay_per_charge_count" +
                            ",COUNT(DISTINCT uid) AS pay_per_charge_users" +
                            ",IFNULL(SUM(totalAmount),0) AS pay_per_charge_amount"
                    )
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

            //计次充电的时长
            long pay_per_charge_duration = 0;
            if (pay_per_charge_duration == 0) {
                //region 计次充电的时长（考虑跨天的情况）

                //计算当天不跨天充电的充电时间
                long chargeTime_ms1 = 0;
                Map<String, Object> chargeTimeData1 = ChargeOrderEntity.getInstance()
                        .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
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
//                    LogsUtil.info("平台月汇总 - 计次充电的时长", "%s - 当天 总充电时间 %s 毫秒", date, currentChargeTime);
                }

                //查询前一天跨天充电的充电时间
                Map<String, Object> beforeChargeTimeData1 = ChargeOrderEntity.getInstance()
                        .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
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
//                    LogsUtil.info("平台月汇总 - 计次充电的时长", "%s - 前一天跨天 总充电时间 %s 毫秒", date, beforeChargeTime);
                }

                //查询当天跨天充电的充电时间
                Map<String, Object> afterChargeTimeData1 = ChargeOrderEntity.getInstance()
                        .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
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
//                    LogsUtil.info("平台月汇总 - 计次充电的时长", "%s - 当天跨天 总充电时间 %s 毫秒", date, afterChargeTime);
                }

                pay_per_charge_duration = chargeTime_ms1 / 1000;
                data.put("pay_per_charge_duration", pay_per_charge_duration);
                //endregion
            }
            //endregion

            BigDecimal pay_per_adjustment_charge_amount = new BigDecimal(0);
            //region 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> pay_per_adjustment_charge_amount_data = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(totalAmount),0) AS pay_per_adjustment_charge_amount")
                    .alias("co")
                    .join(UserEntity.getInstance().theTableName(), "u", "u.id = co.uid")
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("co.isTest", 0)
                    .where("u.is_robot", 3)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .find();
            pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(pay_per_adjustment_charge_amount_data, "pay_per_adjustment_charge_amount", 4, RoundingMode.HALF_UP, new BigDecimal(0));
            data.put("pay_per_adjustment_charge_amount", pay_per_adjustment_charge_amount);
            //endregion

            //region 充电卡充电的次数、充电卡充电的人数、充电卡消费金额、充电卡充电的时长

            Map<String, Object> card_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS card_charge_count" +
                            ",COUNT(DISTINCT uid) AS card_charge_users" +
                            ",IFNULL(SUM(chargeCardConsumeAmount),0) AS card_charge_amount"
                    )
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
            data.put("card_charge_amount", card_charge_data.get("card_charge_amount"));

            //充电卡充电的时长
            long card_charge_duration = 0;
            if (card_charge_duration == 0) {
                //region 充电卡充电的时长（考虑跨天的情况）

                //计算当天不跨天充电的充电时间
                long chargeTime_ms2 = 0;
                Map<String, Object> chargeTimeData2 = ChargeOrderEntity.getInstance()
                        .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
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
//                    LogsUtil.info("平台月汇总 - 充电卡充电的时长", "%s - 当天 总充电时间 %s 毫秒", date, currentChargeTime);
                }

                //查询前一天跨天充电的充电时间
                Map<String, Object> beforeChargeTimeData2 = ChargeOrderEntity.getInstance()
                        .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
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
//                    LogsUtil.info("平台月汇总 - 充电卡充电的时长", "%s - 前一天跨天 总充电时间 %s 毫秒", date, beforeChargeTime);
                }

                //查询当天跨天充电的充电时间
                Map<String, Object> afterChargeTimeData2 = ChargeOrderEntity.getInstance()
                        .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
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
//                    LogsUtil.info("平台月汇总 - 充电卡充电的时长", "%s - 当天跨天 总充电时间 %s 毫秒", date, afterChargeTime);
                }

                card_charge_duration = chargeTime_ms2 / 1000;
                data.put("card_charge_duration", card_charge_duration);
                //endregion
            }

            //endregion

            BigDecimal card_adjustment_charge_amount = new BigDecimal(0);
            //region 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> card_adjustment_charge_amount_data = ChargeCardConfigEntity.getInstance()
                    .field("IFNULL(SUM(chargeCardConsumeAmount), 0) AS card_adjustment_charge_amount")
                    .alias("ccc")
                    .join(UserChargeCardEntity.getInstance().theTableName(), "ucc", "ccc.id = ucc.cardConfigId")
                    .join(ChargeOrderEntity.getInstance().theTableName(), "co", "ucc.cardNumber = co.cardNumber")
                    .where("co.status", 2)
                    .where("co.paymentTypeId", 2)
                    .where("co.isTest", 0)
                    .where("ucc.end_time", ">=", startTime)
                    .where("co.create_time", ">=", startTime)
                    .where("co.create_time", "<=", endTime)
                    .whereIn("ccc.usageType", "'staff', 'partners'")
                    .find();
            card_adjustment_charge_amount = MapUtil.getBigDecimal(card_adjustment_charge_amount_data, "card_adjustment_charge_amount", 4, RoundingMode.HALF_UP, new BigDecimal(0));
            data.put("card_adjustment_charge_amount", card_adjustment_charge_amount);

//            LogsUtil.info("平台月汇总", "%s - 充电卡消费调整金额：¥ %s", date, card_adjustment_charge_amount);
            //endregion

            totalChargeTime = pay_per_charge_duration + card_charge_duration;
            data.put("totalChargeTime", totalChargeTime);

            //充值金额
            BigDecimal rechargeAmount = new BigDecimal(0);
            //充值退款金额
            BigDecimal rechargeRefundAmount = new BigDecimal(0);
            //充电卡金额
            BigDecimal chargeCardAmount = new BigDecimal(0);
            //充电卡退款金额
            BigDecimal chargeCardRefundAmount = new BigDecimal(0);
            //当前全平台用户余额
            BigDecimal totalUserBalance = new BigDecimal(0);
            //应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
            BigDecimal taxableIncome = new BigDecimal(0);

            //region 充值订单数、充值金额、充值人数、充值退款订单数、充值退款订单金额

            //充值订单数次数、总充值金额
//            Map<String, Object> recarge_data = RechargeOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as rechargeOrderCount" +
//                            ",IFNULL(COUNT(DISTINCT uid),0) AS rechargeUsers" +
//                            ",IFNULL(SUM(pay_price),0) AS rechargeAmount"
//                    )
//                    .where("status", 2) //状态;1=未支付 2=已完成 -1=已取消，3=全额退款（已屏蔽），4=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> rechargeData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
                            + ",IFNULL(SUM(order_price),0) AS recharge_amount")
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充值金额
            rechargeAmount = MapUtil.getBigDecimal(rechargeData, "recharge_amount");
            data.put("rechargeAmount", rechargeAmount);
            data.put("rechargeOrderCount", rechargeData.get("recharge_order_count"));//充值订单数
            data.put("rechargeUsers", rechargeData.get("recharge_users"));//充值人数

            //充值退款订单数、退款订单金额
//            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> recharge_refund_data = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.product_type", "recharge")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            rechargeRefundAmount = MapUtil.getBigDecimal(recharge_refund_data, "refund_amount");
            data.put("rechargeRefundAmount", rechargeRefundAmount);//充值退款订单金额
            data.put("rechargeRefundOrderCount", recharge_refund_data.get("refund_order_count"));//充值退款订单数

            //endregion

            //region 充电卡订单数、充电卡金额、充电卡人数、充电卡退款订单数、充电卡退款订单金额

//            Map<String, Object> chargecard_data = UserChargeCardOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as chargeCardOrderCount" +
//                            ",IFNULL(COUNT(DISTINCT uid),0) AS chargeCardUsers" +
//                            ",IFNULL(SUM(totalAmount),0) AS chargeCardAmount"
//                    )
//                    .where("status", 1) //状态;0=等待支付，1=支付成功，2=全额退款（已屏蔽），3=部分退款（已屏蔽）
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardOrderData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS charge_card_users"
                            + ",IFNULL(SUM(order_price),0) AS charge_card_amount")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("product_type", "charge_card")
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充电卡金额
            chargeCardAmount = MapUtil.getBigDecimal(chargeCardOrderData, "charge_card_amount");
            data.put("chargeCardAmount", chargeCardAmount);
            data.put("chargeCardOrderCount", chargeCardOrderData.get("charge_card_order_count"));//充电卡订单数
            data.put("chargeCardUsers", chargeCardOrderData.get("charge_card_users"));//充电卡人数

            //充电卡退款订单数、退款订单金额
//            Map<String, Object> chargecard_refund_data = UserChargeCardRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .find();
            Map<String, Object> chargeCardRefundOrderData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.product_type", "charge_card")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .where("cor.create_time", ">=", startTime)
                    .where("cor.create_time", "<=", endTime)
                    .find();
            chargeCardRefundAmount = MapUtil.getBigDecimal(chargeCardRefundOrderData, "refund_amount");
            data.put("chargeCardRefundAmount", chargeCardRefundAmount);//充电卡退款订单金额
            data.put("chargeCardRefundOrderCount", MapUtil.getInt(chargeCardRefundOrderData, "refund_order_count"));//充电卡退款订单数

            //endregion

            //region 当前全平台用户余额
            totalUserBalance = UserSummaryEntity.getInstance()
                    .alias("a")
                    .join(UserEntity.getInstance().theTableName(), "b", "a.uid = b.id")
                    .where("b.is_robot", 0)
                    .sumGetBigDecimal("a.balance", 2, RoundingMode.HALF_UP);
            data.put("totalUserBalance", totalUserBalance);
            //endregion

            //region 应税收入（总充电消费金额），公式：计次充电消费+充电卡金额-充电卡退款金额
            taxableIncome = pay_per_charge_amount
                    .subtract(pay_per_adjustment_charge_amount)
                    .add(chargeCardAmount)
                    .add(chargeCardRefundAmount);
            data.put("taxableIncome", taxableIncome);
            //endregion

            //此充电桩运行中的充电端口
            if (totalSocket > 0) {
                //region (次/插座)次数使用率（APR）：当月充电次数 / 全平台运行中的充电端口
                BigDecimal chargeCountUseRate = BigDecimal.valueOf(totalUseCount)
                        .divide(BigDecimal.valueOf(totalSocket), 6, RoundingMode.HALF_UP);
                data.put("chargeCountUseRate", chargeCountUseRate);
                //endregion


                //region (%)时长使用率（APR）：当月充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
                List<Map<String, Object>> chargeStationEntityList = ChargeStationEntity.getInstance()
                        .field("id,online_time,totalSocket")
                        .where("status", 1)
                        .where("is_private", 0)
                        .where("is_restricted", 0)
                        .where("isTest", 0)
                        .order("id")
                        .select();
                //端口运行时间 = 充电桩的运营时间 * 端口数

                long summaryStartTime = startTime;
                long summaryEndTime = endTime;
                if (endTime > TimeUtil.getTimestamp()) summaryEndTime = TimeUtil.getTimestamp();

                long totalSocketRunTime = 0;
                if (chargeStationEntityList != null) {
                    Iterator it = chargeStationEntityList.iterator();
                    while (it.hasNext()) {
                        Map<String, Object> nd = (Map<String, Object>) it.next();
                        long online_time = MapUtil.getLong(nd, "online_time");
                        int totalSocket_temp = MapUtil.getInt(nd, "totalSocket");

                        //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计
                        if (online_time > summaryEndTime) continue;

                        //表示充电桩的上线时间比统计开始时间还晚，应该以上线时间来进行计算,注意时间戳为毫秒级
                        if (online_time >= summaryStartTime) {
                            totalSocketRunTime += (summaryEndTime - online_time) / 1000 * totalSocket_temp;
                        } else {
                            totalSocketRunTime += (summaryEndTime - summaryStartTime) / 1000 * totalSocket_temp;
                        }
                    }
                }
                BigDecimal chargeTimeUseRate = new BigDecimal(0);
                if (totalSocketRunTime > 0) {
                    chargeTimeUseRate = BigDecimal.valueOf(totalChargeTime)
                            .divide(BigDecimal.valueOf(totalSocketRunTime), 6, RoundingMode.HALF_UP);
                }

                data.put("totalSocketRunTime", totalSocketRunTime);
                data.put("chargeTimeUseRate", chargeTimeUseRate);
//                System.out.println(String.format("%s / %s = %s ", totalChargeTime, totalSocketRunTime, chargeTimeUseRate));
                //endregion


                long lastTime = TimeUtil.getTimestamp();
                if (endTime < TimeUtil.getTimestamp()) {
                    //表示统计历史数据
                    lastTime = endTime;
                }

                //region 插座净收入(元/日/插座): 每日净收入的平均值

                BigDecimal netIncome = new BigDecimal(0);
                Map<String, Object> dailySummaryData = PlatformDailySummaryEntity.getInstance()
                        .field("IFNULL(AVG(netIncome),0) AS netIncome")
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", lastTime)
                        .find();
                if (dailySummaryData != null && dailySummaryData.size() > 0) {
                    netIncome = MapUtil.getBigDecimal(dailySummaryData, "netIncome");
                }
                data.put("netIncome", netIncome);
                //endregion

                //region 累计插座净收入(元/插座): 每日净收入的合计
                BigDecimal totalNetIncome = new BigDecimal(0);
                Map<String, Object> totalNetIncomeData = PlatformDailySummaryEntity.getInstance()
                        .field("IFNULL(SUM(netIncome),0) AS totalNetIncome")
                        .where("date_time", ">=", startTime)
                        .where("date_time", "<=", lastTime)
                        .find();
                if (totalNetIncomeData != null && totalNetIncomeData.size() > 0) {
                    totalNetIncome = MapUtil.getBigDecimal(totalNetIncomeData, "totalNetIncome");
                }
                data.put("totalNetIncome", totalNetIncome);
                //endregion
            }

            //region idle用户数：充值或者购买了充电卡但是没有进行过任何充电的用户

            // 查询有充值的用户
//            List<Object> r_user_ids = RechargeOrderEntity.getInstance()
//                    .field("uid")
//                    .where("status", 2)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> r_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .group("uid")
                    .selectForArray("uid");

            // 查询购买了充电卡的用户
//            List<Object> c_user_ids = UserChargeCardOrderEntity.getInstance()
//                    .field("uid")
//                    .where("status", 1)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .group("uid")
//                    .selectForArray("uid");
            List<Object> c_user_ids = ConsumeOrdersEntity.getInstance()
                    .field("uid")
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
            int paymentUserCount = combinedUserIds.size();

            // 查询有充电的用户
            List<Object> chargedUserIds = ChargeOrderEntity.getInstance()
                    .field("uid")
                    .where("startTime", ">=", startTime)
                    .where("startTime", "<=", endTime)
                    .group("uid")
                    .selectForArray("uid");

            // 过滤出没有充电的用户
            combinedUserIds.removeAll(chargedUserIds);

            // 输出或处理没有充电的用户
            int idleUserCount = combinedUserIds.size();

//            System.out.println("有充值或购买充电卡的用户数：" + paymentUserCount);
//            System.out.println("没有充电的用户：" + idleUserCount);

            data.put("paymentUserCount", paymentUserCount);
            data.put("idleUserCount", idleUserCount);

            //endregion

            data.put("date_time", startTime);
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("date", date).exist()) {
                getInstance().where("date", date).update(data);
            } else {
                data.put("date", date);
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "平台月数据汇总", "汇总数据发生错误，date=%s", date);
        }
        return new SyncResult(0, "");
    }

    /**
     * 内嵌自己来重复修复（辅助使用）
     */
    public SyncResult repairSyncData(long date_timestamp) {
        long startTime = TimeUtil.toMonthBegin00(date_timestamp);

        String date = TimeUtil.toTimeString(startTime, "yyyy-MM");
        LogsUtil.info("平台月数据汇总 - 修复数据任务", "%s 开始修复数据", date);

        long month = TimeUtil.getMonthBegin00();
        if (startTime <= month) {
            SyncResult r = syncData(startTime);
            if (r.code == 0) {
                startTime = TimeUtil.getAddMonthTimestamp(startTime, 1);
            }
            repairSyncData(startTime);
        }
        LogsUtil.info("平台月数据汇总 - 修复数据任务", "%s 修复结束！！！", date);
        return new SyncResult(1, "");
    }

    /**
     * 这个用来跑充电桩修复每日数据的（辅助使用）
     */
    public void repairData(long start_time) {
        ThreadUtil.getInstance().execute("", () -> {
            long date_timestamp = start_time;
            if (date_timestamp <= 0) date_timestamp = ChargeStationEntity.getInstance().getEarliestOnlineTime();
            repairSyncData(date_timestamp);
        });
    }
}