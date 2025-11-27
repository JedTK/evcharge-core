package com.evcharge.entity.station;

import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSourceInfoEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 充电桩日数据汇总
 */
public class ChargeStationDailySummaryEntity extends BaseEntity implements Serializable {
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
     * 充电站ID
     */
    public long CSId;
    /**
     * 累计充电时长
     */
    public long totalChargeTime;
    /**
     * 累计使用次数
     */
    public int totalUseCount;
    /**
     * 累计使用故障次数
     */
    public int totalUseError;
    /**
     * 累计耗电量（度）
     */
    public double totalPowerConsumption;
    /**
     * 累计消费数（元）
     */
    public double totalAmount;
    /**
     * 按次充电消费金额
     */
    public double totalCountConsumeAmount;
    /**
     * 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
     */
    public BigDecimal pay_per_adjustment_charge_amount;

    /**
     * 充电卡消费金额
     */
    public double totalChargeCardConsumeAmount;
    /**
     * 充电卡消费调整金额（计算员工卡、合作卡之类的金额）
     */
    public BigDecimal card_adjustment_charge_amount;

    /**
     * 充电卡消耗时间
     */
    public long totalChargeCardConsumeTime;
    /**
     * 充电网络主机数
     */
    public int totalHostDeviceCount;
    /**
     * 充电终端数
     */
    public int totalChargeDeviceCount;
    /**
     * 总的插座
     */
    public int totalSocket;
    /**
     * 最小功率
     */
    public double minPower;
    /**
     * 最大功率
     */
    public double maxPower;
    /**
     * 平均功率
     */
    public double avgPower;
    /**
     * 充值订单数次数
     */
    public int rechargeOrderCount;
    /**
     * 总充值金额
     */
    public double rechargeAmount;

    /**
     * 充电卡订单总数
     */
    public int chargeCardOrderCount;
    /**
     * 充电卡订单金额
     */
    public double chargeCardAmount;

    /**
     * 用户注册数
     */
    public int userRegisterCount;

    /**
     * 充值退款订单数
     */
    public int rechargeRefundOrderCount;
    /**
     * 充值退款订单金额
     */
    public BigDecimal rechargeRefundAmount;
    /**
     * 充电卡退款订单数
     */
    public int chargeCardRefundOrderCount;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal chargeCardRefundAmount;

    /**
     * (次/插座)次数使用率（APR）：今天充电次数 / 全平台运行中的充电端口
     */
    public BigDecimal chargeCountUseRate;
    /**
     * (%)时长使用率（APR）：今天的充电时长「秒数」 / （不含私有桩）(充电桩A总端口今天运行时间 + 充电桩B总端口今天运行时间...)
     */
    public BigDecimal chargeTimeUseRate;
    /**
     * (元/日/插座)净收入：(充值 + 充电卡 - 充值退款 - 充电卡退款) /  全平台运行中的充电端口
     */
    public BigDecimal netIncome;

    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationDailySummaryEntity getInstance() {
        return new ChargeStationDailySummaryEntity();
    }

    /**
     * 同步数据
     *
     * @param CSId           充电桩ID
     * @param date_timestamp 统计时间戳（这里泛指按日统计）
     * @return
     */
    public SyncResult syncData(long CSId, long date_timestamp) {
        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM-dd");
        try {
            //当天凌晨时间戳
            long startTime = TimeUtil.toTimestamp00(date_timestamp);
            //当天结束时间戳
            long endTime = TimeUtil.toTimestamp24(date_timestamp);
            //前一天凌晨时间戳
            long beforeDayStartTime = startTime - ECacheTime.DAY;
            //前一天结束时间戳
            long beforeDayEndTime = TimeUtil.toDayEnd24(beforeDayStartTime);

            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().findModel(CSId);
            if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(1, "");

            Map<String, Object> data = new LinkedHashMap<>();

            //region 用户注册数

            long userRegisterCount = UserSourceInfoEntity.getInstance()
                    .where("cs_id", CSId)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .count();
            data.put("userRegisterCount", userRegisterCount);

            //endregion

            long totalChargeTime;
            long totalUseCount;
            //充值金额
            BigDecimal rechargeAmount;
            //充值退款金额
            BigDecimal rechargeRefundAmount;
            //充电卡金额
            BigDecimal chargeCardAmount;
            //充电卡退款金额
            BigDecimal chargeCardRefundAmount;

            //region 充电时长(考虑跨天的情况)

            //计算当天不跨天充电的充电时间
            long chargeTime_ms = 0;
            Map<String, Object> chargeTimeData = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("stopTime", "<=", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long currentChargeTime = MapUtil.getLong(chargeTimeData, "chargeTime");
            if (currentChargeTime > 0) {
                chargeTime_ms += currentChargeTime;
//                LogsUtil.info("充电桩日数据汇总", "CSId=%s %s - 当天 总充电时间 %s 毫秒", CSId, date, currentChargeTime);
            }

            //查询前一天跨天充电的充电时间
            Map<String, Object> beforeChargeTimeData = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(stopTime - %s),0) AS chargeTime", startTime))
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("startTime", ">", beforeDayStartTime)
                    .where("startTime", "<=", beforeDayEndTime)
                    .where("stopTime", ">=", startTime)
                    .where("stopTime", ">", 0)
                    .find();
            long beforeChargeTime = MapUtil.getLong(beforeChargeTimeData, "chargeTime");
            if (beforeChargeTime > 0) {
                chargeTime_ms += beforeChargeTime;
//                LogsUtil.info("充电桩日数据汇总", "CSId=%s %s - 前一天跨天 总充电时间 %s 毫秒", CSId, date, beforeChargeTime);
            }

            //查询当天跨天充电的充电时间
            Map<String, Object> afterChargeTimeData = ChargeOrderEntity.getInstance()
                    .field(String.format("IFNULL(SUM(%s - startTime),0) AS chargeTime", endTime))
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("startTime", ">", startTime)
                    .where("startTime", "<=", endTime)
                    .where("stopTime", ">", endTime)
                    .where("stopTime", ">", 0)
                    .find();
            long afterChargeTime = MapUtil.getLong(afterChargeTimeData, "chargeTime");
            if (afterChargeTime > 0) {
                chargeTime_ms += afterChargeTime;
//                LogsUtil.info("充电桩日数据汇总", "CSId=%s %s - 当天跨天 总充电时间 %s 毫秒", CSId, date, afterChargeTime);
            }

            //充电时长
            totalChargeTime = chargeTime_ms / 1000;
            data.put("totalChargeTime", totalChargeTime);
            //endregion

            //region 累计使用次数、累计耗电量（度）
            Map<String, Object> sumcount = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS totalUseCount,IFNULL(SUM(powerConsumption),0) AS totalPowerConsumption")
                    .where("CSId", CSId)
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
            //endregion

            //region累计安全预警次数
            long totalUseError = ChargeOrderEntity.getInstance()
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .whereIn("stopReasonCode", "-1,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                    .countGetLong("1");
            data.put("totalUseError", totalUseError);
            //endregion

            //region 累计消费数（元）

            //充电订单按次消费金额
            double totalCountConsumeAmount = ChargeOrderEntity.getInstance()
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .sumGetDouble("totalAmount");

            //充电卡实际消费金额
            double totalChargeCardConsumeAmount = ChargeOrderEntity.getInstance()
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .sumGetDouble("chargeCardConsumeAmount");

            //累计充电卡订单金额
//            double chargeCardOrderTotalAmount = UserChargeCardOrderEntity.getInstance()
//                    .where("CSId", CSId)
//                    .where("status", 1)//0=等待支付，1=支付成功
//                    .where("isTest", 0)
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .sumGetDouble("totalAmount");

            //累计消费数：充电订单按次消费金额 + 充电卡实际消费金额
            double totalAmount = totalCountConsumeAmount + totalChargeCardConsumeAmount;
            data.put("totalAmount", totalAmount);
            data.put("totalCountConsumeAmount", totalCountConsumeAmount);
            data.put("totalChargeCardConsumeAmount", totalChargeCardConsumeAmount);

            //endregion

            BigDecimal pay_per_adjustment_charge_amount = new BigDecimal(0);
            //region 计次充电消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> pay_per_adjustment_charge_amount_data = ChargeOrderEntity.getInstance()
                    .field("IFNULL(SUM(totalAmount),0) AS pay_per_adjustment_charge_amount")
                    .alias("co")
                    .join(UserEntity.getInstance().theTableName(), "u", "u.id = co.uid")
                    .where("CSId", CSId)
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

            BigDecimal card_adjustment_charge_amount = new BigDecimal(0);
            //region 充电卡消费调整金额（计算内部用户、合作用户之类的金额）
            Map<String, Object> card_adjustment_charge_amount_data = ChargeCardConfigEntity.getInstance()
                    .field("IFNULL(SUM(chargeCardConsumeAmount), 0) AS card_adjustment_charge_amount")
                    .alias("ccc")
                    .join(UserChargeCardEntity.getInstance().theTableName(), "ucc", "ccc.id = ucc.cardConfigId")
                    .join(ChargeOrderEntity.getInstance().theTableName(), "co", "ucc.cardNumber = co.cardNumber")
                    .where("co.CSId", CSId)
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

            //region 充电卡消耗时间
            double totalChargeCardConsumeTime = ChargeOrderEntity.getInstance()
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .sumGetDouble("chargeCardConsumeTime");
            data.put("totalChargeCardConsumeTime", totalChargeCardConsumeTime);
            //endregion

            //region 充电网络主机数、充电终端数、总电位数
            //充电网络主机数
            long totalHostDeviceCount = 0;
            //充电终端数
            long totalChargeDeviceCount = 0;
            //总电位数（不含私有桩）
            int totalSocket = 0;
            //统计主机数量
            totalHostDeviceCount = DeviceEntity.getInstance()
                    .where("CSId", CSId)
                    .where("isHost", 1)
                    .count("1");

            //统计从机数量
            String[] deviceIds = DeviceEntity.getInstance()
                    .where("CSId", CSId)
                    .where("isHost", 0)//主机：0=否，1=是
                    .selectForStringArray("id");
            //统计从机充电端口数量
            if (deviceIds.length > 0) {
                totalChargeDeviceCount = deviceIds.length;

                totalSocket = DeviceSocketEntity.getInstance()
                        .whereIn("deviceId", deviceIds)
                        .count();
            }
            data.put("totalHostDeviceCount", totalHostDeviceCount);
            data.put("totalChargeDeviceCount", totalChargeDeviceCount);
            data.put("totalSocket", totalSocket);
            //endregion

            //region 最小功率、最大功率、平均功率
            Map<String, Object> minmaxavg = ChargeOrderEntity.getInstance()
                    .field("IFNULL(MAX(maxPower),0) as maxPower ,IFNULL(MIN(maxPower),0) as minPower,IFNULL(AVG(maxPower),0) as avgPower")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //最小功率
            data.put("minPower", minmaxavg.get("minPower"));
            //最大功率
            data.put("maxPower", minmaxavg.get("maxPower"));
            //平均功率
            data.put("avgPower", minmaxavg.get("avgPower"));
            //endregion

            //region 充值订单数次数、总充值金额、退款订单数、退款订单金额         【废弃】- 全额退款订单数、全额退款金额、部分退款订单数、部分退款金额

            //充值订单数次数、总充值金额
//            Map<String, Object> recarge_data = RechargeOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as rechargeOrderCount,IFNULL(SUM(pay_price),0) AS rechargeAmount")
//                    .whereIn("status", "2,3,4") //状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
//                    .find();
            Map<String, Object> rechargeData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
                            + ",IFNULL(SUM(order_price),0) AS recharge_amount")
                    .where("CSId", CSId)
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            rechargeAmount = MapUtil.getBigDecimal(rechargeData, "recharge_amount");
            data.put("rechargeOrderCount", rechargeData.get("recharge_order_count"));
            data.put("rechargeAmount", rechargeAmount);


            //充值退款订单数、退款订单金额
//            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
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
            rechargeRefundAmount = MapUtil.getBigDecimal(rechargeRefundData, "refund_amount");
            data.put("rechargeRefundOrderCount", rechargeRefundData.get("refund_order_count"));
            data.put("rechargeRefundAmount", rechargeRefundAmount);

            //endregion

            //region 充电卡订单总数、充电卡订单金额、退款订单数、退款订单金额         【废弃】- 全额退款订单数、全额退款金额、部分退款订单数、部分退款金额

//            Map<String, Object> chargecard_data = UserChargeCardOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as chargeCardOrderCount,IFNULL(SUM(totalAmount),0) AS chargeCardAmount")
//                    .whereIn("status", "1,2,3") //状态;0=等待支付，1=支付成功，2=全额退款，3=部分退款
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
//                    .find();
            Map<String, Object> chargeCardOrderData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
                            + ",IFNULL(SUM(order_price),0) AS charge_card_amount")
                    .where("CSId", CSId)
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("product_type", "charge_card")
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            chargeCardAmount = MapUtil.getBigDecimal(chargeCardOrderData, "charge_card_amount");
            data.put("chargeCardOrderCount", chargeCardOrderData.get("charge_card_order_count"));
            data.put("chargeCardAmount", chargeCardAmount);


            //充电卡退款订单数、退款订单金额
//            Map<String, Object> chargecard_refund_data = UserChargeCardRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("create_time", ">=", startTime)
//                    .where("create_time", "<=", endTime)
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
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
            chargeCardRefundAmount = MapUtil.getBigDecimal(chargeCardRefundOrderData, "refund_amount");
            data.put("chargeCardRefundOrderCount", MapUtil.getInt(chargeCardRefundOrderData, "refund_order_count"));
            data.put("chargeCardRefundAmount", chargeCardRefundAmount);

            //endregion

            //此充电桩运行中的充电端口
            Map<String, Object> chargeStationSummaryData = new LinkedHashMap<>();
            //此充电桩运行中的充电端口
            if (totalSocket > 0) {
                //region (次/插座)次数使用率（APR）：今天充电次数 / 全平台运行中的充电端口
                BigDecimal chargeCountUseRate = BigDecimal.valueOf(totalUseCount)
                        .divide(BigDecimal.valueOf(totalSocket), 6, RoundingMode.HALF_UP);
                data.put("chargeCountUseRate", chargeCountUseRate);
                chargeStationSummaryData.put("chargeCountDayUseRate", chargeCountUseRate);
                //endregion


                //region (%)时长使用率（APR）：今天的充电时长「秒数」 / （不含私有桩）(充电桩A总端口今天运行时间 + 充电桩B总端口今天运行时间...)
                long summaryStartTime = startTime;
                long summaryEndTime = endTime;
                if (endTime > TimeUtil.getTimestamp()) summaryEndTime = TimeUtil.getTimestamp();

                //此充电桩总端口今天运行时间 = 充电桩的运营时间 * 端口数
                long totalSocketRunTime = 0;
                //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计
                if (chargeStationEntity.online_time <= summaryEndTime) {
                    //表示充电桩的上线时间比统计开始时间还晚，应该以上线时间来进行计算,注意时间戳为毫秒级
                    if (chargeStationEntity.online_time >= summaryStartTime) {
                        totalSocketRunTime += (summaryEndTime - chargeStationEntity.online_time) / 1000 * totalSocket;
                    } else {
                        totalSocketRunTime += (summaryEndTime - summaryStartTime) / 1000 * totalSocket;
                    }
                }

                BigDecimal chargeTimeUseRate = new BigDecimal(0);
                if (totalSocketRunTime > 0) {
                    chargeTimeUseRate = BigDecimal.valueOf(totalChargeTime)
                            .divide(BigDecimal.valueOf(totalSocketRunTime), 6, RoundingMode.HALF_UP);
                }

                data.put("totalSocketRunTime", totalSocketRunTime);
                data.put("chargeTimeUseRate", chargeTimeUseRate);

                chargeStationSummaryData.put("chargeTimeDayUseRate", chargeTimeUseRate);
                //endregion


                //region (元/日/插座)净收入：(充值 + 充电卡 - 充值退款 - 充电卡退款) /  全平台运行中的充电端口
                BigDecimal netIncome = new BigDecimal(0);
                if (totalSocket > 0) {
                    netIncome = (rechargeAmount
                            .add(chargeCardAmount)
                            .add(rechargeRefundAmount)
                            .add(chargeCardRefundAmount))
                            .divide(new BigDecimal(totalSocket), 6, RoundingMode.HALF_UP);
                }
                data.put("netIncome", netIncome);
                //endregion
            }

            data.put("date_time", startTime);
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("CSId", CSId).where("date", date).exist()) {
                getInstance().where("CSId", CSId)
                        .where("date", date)
                        .update(data);
            } else {
                data.put("CSId", CSId);
                data.put("date", date);
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }

            if (chargeStationSummaryData.size() > 0) {
                ChargeStationSummaryEntity.getInstance().where("CSId", CSId).update(chargeStationSummaryData);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "充电桩日数据汇总", "汇总数据发生错误，CSId=%s date=%s", CSId, date);
        }

        return new SyncResult(0, "");
    }

    /**
     * 内嵌自己来重复修复（辅助使用）
     */
    public SyncResult repairSyncData(long CSId, long date_timestamp) {
        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM-dd");
        LogsUtil.info("充电桩日数据汇总 - 修复数据任务", "CSId = %s 开始修复 %s 的数据", CSId, date);

        long startTime = TimeUtil.toTimestamp00(date_timestamp);
        long today = TimeUtil.getTime00();
        if (startTime <= today) {
            SyncResult r = syncData(CSId, date_timestamp);
            if (r.code == 0) {
                date_timestamp += ECacheTime.DAY;
            }
            repairSyncData(CSId, date_timestamp);
        }
        LogsUtil.info("充电桩日数据汇总 - 修复数据任务", "CSId = %s 修复结束！！！截止时间：%s", CSId, date);
        return new SyncResult(1, "");
    }

    /**
     * 这个用来跑充电桩修复每日数据的（辅助使用）
     */
    public void repairData(long start_time) {
        List<Map<String, Object>> list = ChargeStationEntity.getInstance()
                .field("id,name,online_time")
                .where("status", 1)
                .where("online_time", ">", 0)
                .select();
        for (Map<String, Object> nd : list) {
            long CSId = MapUtil.getLong(nd, "id");
            long online_time = MapUtil.getLong(nd, "online_time");
            ThreadUtil.getInstance().execute("", () -> {
                //从指定日期开始修复
                if (start_time > 0) {
                    repairSyncData(CSId, start_time);
                    return;
                }
                repairSyncData(CSId, online_time);
            });
        }
    }
}
