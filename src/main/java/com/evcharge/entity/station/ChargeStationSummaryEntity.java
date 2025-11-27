package com.evcharge.entity.station;


import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.device.MonitorDeviceEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSourceInfoEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 充电桩数据汇总;
 *
 * @author : JED
 * @date : 2022-11-22
 */
public class ChargeStationSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public long CSId;
    /**
     * 累计充电时长
     */
    public long totalChargeTime;
    /**
     * 当月充电时长
     */
    public long currentMonthChargeTime;
    /**
     * 近3个月充电时长
     */
    public long threeMonthChargeTime;
    /**
     * 近6个月充电时长
     */
    public long sixMonthChargeTime;
    /**
     * 累计使用次数
     */
    public long totalUseCount;
    /**
     * 累计使用故障次数
     */
    public int totalUseError;
    /**
     * 运行效率
     */
    public double operationEfficiency;
    /**
     * 累计耗电量（度）
     */
    public double totalPowerConsumption;
    /**
     * 累计消费数（元）
     */
    public double totalAmount;
    /**
     * 计次充电消费金额
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
     * 监视器设备数
     */
    public int monitorTotalCount;
    /**
     * 监视器在线数
     */
    public int monitorOnlineCount;
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
     * 充值全额退款订单数
     */
    public long rechargeFullRefundOrderCount;
    /**
     * 充值全额退款订单金额
     */
    public BigDecimal rechargeFullRefundAmount;
    public long rechargePartialRefundOrderCount;
    public BigDecimal rechargePartialRefundAmount;
    public long chargeCardFullRefundOrderCount;
    public BigDecimal chargeCardFullRefundAmount;
    public long chargeCardPartialRefundOrderCount;
    public BigDecimal chargeCardPartialRefundAmount;
    public BigDecimal chargeCountMonthUseRate;
    public BigDecimal chargeTimeMonthUseRate;
    public BigDecimal chargeCountDayUseRate;
    public BigDecimal chargeTimeDayUseRate;
    public BigDecimal incomeRate;

    /**
     * (次/插座)次数使用率（APR）：所有充电次数 / 全平台运行中的充电端口（不含私有桩）
     */
    public BigDecimal chargeCountUseRate;
    /**
     * (%)时长使用率（APR）：所有充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
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
     *
     * @return
     */
    public static ChargeStationSummaryEntity getInstance() {
        return new ChargeStationSummaryEntity();
    }

    /**
     * 运行统计任务
     *
     * @param CSId
     * @return
     */
    public SyncResult runTask(long CSId) {
        try {
            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().findModel(CSId);
            ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance();

            int isTest = chargeStationEntity.isTest;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("CSId", CSId);

            //累计充电时长
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            long totalChargeTime = orderEntity.sumGetLong("totalChargeTime");

            //当月充电时长
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            long currentMonthChargeTime = orderEntity.where("create_time", ">=", TimeUtil.getMonthBegin00()).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");

            //近3个月充电时长
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            long threeMonthChargeTime = orderEntity.where("create_time", ">=", TimeUtil.getMonthBegin00(-3)).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");

            //近6个月充电时长
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            long sixMonthChargeTime = orderEntity.where("create_time", ">=", TimeUtil.getMonthBegin00(-6)).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");

            //累计使用次数
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            long totalUseCount = orderEntity.countGetLong("1");

            //累计耗电量（度）
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            double totalPowerConsumption = orderEntity.sumGetDouble("powerConsumption");

            //累计消费数（元）
            orderEntity.where("CSId", CSId).where("status", 2);
            if (isTest == 0) orderEntity.where("isTest", 0);
            double totalAmount = orderEntity.where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .sumGetDouble("totalAmount");

            //运行效率 = 累计充电时间 / 正式上线时间的小时数
            double operationEfficiency = 0.0;
            if (chargeStationEntity.online_time > 0) {
                double totalHour = TimeUtil.convertToHour(TimeUtil.getTimestamp() / 1000 - chargeStationEntity.online_time / 1000).doubleValue();
                double totalChargeTimeHour = TimeUtil.convertToHour(totalChargeTime).doubleValue();
                operationEfficiency = totalChargeTimeHour / totalHour;
            }

            data.put("totalChargeTime", totalChargeTime);
            data.put("currentMonthChargeTime", currentMonthChargeTime);
            data.put("threeMonthChargeTime", threeMonthChargeTime);
            data.put("sixMonthChargeTime", sixMonthChargeTime);
            data.put("totalUseCount", totalUseCount);
            data.put("operationEfficiency", operationEfficiency);
            data.put("totalPowerConsumption", totalPowerConsumption);
            data.put("totalAmount", totalAmount);
            data.put("update_time", TimeUtil.getTimestamp());

            if (this.where("CSId", CSId).exist()) {
                this.where("CSId", CSId).update(data);
            } else {
                this.insert(data);
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "ChargeStationSummary 统计充电数据发生错误，CSId=%s", CSId);
        }
        return new SyncResult(0, "");
    }

    /**
     * 同步数据
     * 更新日志：2023-09-27 充电时长更改为stopTime - startTime
     *
     * @param CSId 充电桩ID
     * @return
     */
    public SyncResult syncData(long CSId) {
        try {
            if (CSId == 20) {
                System.out.println("111");
            }
            ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance();

            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().findModel(CSId);
            if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(1, "");

            Map<String, Object> data = new LinkedHashMap<>();

            //region 用户注册数

            long userRegisterCount = UserSourceInfoEntity.getInstance().where("cs_id", CSId).count();
            data.put("userRegisterCount", userRegisterCount);

            //endregion


            long totalChargeTime = 0;
            long totalUseCount = 0;
            //充值金额
            BigDecimal rechargeAmount = new BigDecimal(0);
            //充值退款金额
            BigDecimal rechargeRefundAmount = new BigDecimal(0);
            //充电卡金额
            BigDecimal chargeCardAmount = new BigDecimal(0);
            //充电卡退款金额
            BigDecimal chargeCardRefundAmount = new BigDecimal(0);

            //region 充电时长(考虑跨天的情况)

            //通过stopTime - startTime来统计充电时长
            Map<String, Object> chargeTimeData = orderEntity.field("IFNULL(SUM(stopTime - startTime),0) AS chargeTime")
                    .where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("stopTime", ">", 0)
                    .find();
            long currentChargeTime = MapUtil.getLong(chargeTimeData, "chargeTime");
            if (currentChargeTime > 0) {
                totalChargeTime = currentChargeTime / 1000;

//                LogsUtil.info("充电桩数据汇总", "CSId=%s 总充电时间 %s 毫秒", CSId, currentChargeTime);
            }

            data.put("totalChargeTime", totalChargeTime);
            //endregion

            //region 累计使用次数、累计耗电量（度）
            Map<String, Object> sumcount = orderEntity.field("COUNT(1) AS totalUseCount,IFNULL(SUM(powerConsumption),0) AS totalPowerConsumption").where("CSId", CSId).where("status", 2).where("isTest", 0).find();
            totalUseCount = MapUtil.getLong(sumcount, "totalUseCount");

            //累计使用次数
            data.put("totalUseCount", totalUseCount);
            //累计耗电量（度）
            data.put("totalPowerConsumption", sumcount.get("totalPowerConsumption"));
            //endregion

            //region 当月充电时长、近3个月充电时长、近6个月充电时长
            //当月充电时长
            long currentMonthChargeTime = ChargeStationMonthlySummaryEntity.getInstance().where("CSId", CSId).where("create_time", ">=", TimeUtil.getMonthBegin00()).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");
            data.put("currentMonthChargeTime", currentMonthChargeTime);

            //近3个月充电时长
            long threeMonthChargeTime = ChargeStationMonthlySummaryEntity.getInstance().where("CSId", CSId).where("create_time", ">=", TimeUtil.getMonthBegin00(-3)).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");
            data.put("threeMonthChargeTime", threeMonthChargeTime);

            //近6个月充电时长
            long sixMonthChargeTime = ChargeStationMonthlySummaryEntity.getInstance().where("CSId", CSId).where("create_time", ">=", TimeUtil.getMonthBegin00(-6)).where("create_time", "<=", TimeUtil.getMonthEnd24()).sumGetLong("totalChargeTime");
            data.put("sixMonthChargeTime", sixMonthChargeTime);

            //endregion

            //region累计安全预警次数
            long totalUseError = orderEntity.where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .whereIn("stopReasonCode", "-1,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20")
                    .countGetLong("1");
            data.put("totalUseError", totalUseError);
            //endregion

            //region 累计消费数（元）

            //充电订单按次消费金额
            double totalCountConsumeAmount = ChargeOrderEntity.getInstance().where("CSId", CSId).where("status", 2).where("isTest", 0).where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .sumGetDouble("totalAmount");

            //充电卡实际消费金额
            double totalChargeCardConsumeAmount = ChargeOrderEntity.getInstance().where("CSId", CSId).where("status", 2).where("isTest", 0).where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .sumGetDouble("chargeCardConsumeAmount");

            //累计充电卡订单金额
//            double chargeCardOrderTotalAmount = UserChargeCardOrderEntity.getInstance()
//                    .where("CSId", CSId)
//                    .where("status", 1)//0=等待支付，1=支付成功
//                    .where("isTest", 0)
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
                    .whereIn("ccc.usageType", "'staff', 'partners'")
                    .find();
            card_adjustment_charge_amount = MapUtil.getBigDecimal(card_adjustment_charge_amount_data, "card_adjustment_charge_amount", 4, RoundingMode.HALF_UP, new BigDecimal(0));
            data.put("card_adjustment_charge_amount", card_adjustment_charge_amount);

//            LogsUtil.info("平台月汇总", "%s - 充电卡消费调整金额：¥ %s", date, card_adjustment_charge_amount);
            //endregion

            //region 充电卡消耗时间
            double totalChargeCardConsumeTime = orderEntity.where("CSId", CSId)
                    .where("status", 2)
                    .where("isTest", 0)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .sumGetDouble("chargeCardConsumeTime");
            data.put("totalChargeCardConsumeTime", totalChargeCardConsumeTime);
            //endregion

            //region 充电网络主机数、充电终端数、总电位数
            //充电网络主机数
            long totalHostDeviceCount = 0;
            //充电终端数
            long totalChargeDeviceCount = 0;
            //总电位数
            int totalSocket = 0;
            //充电位使用中数量
            int deviceSocketUsingCount = 0;
            //充电位空闲中数量
            int deviceSocketIdleCount = 0;
            //**/充电位占用中数量
            int deviceSocketOccupiedCount = 0;
            //充电位异常数量
            int deviceSocketErrorCount = 0;
            //监视器设备数
            int monitorTotalCount = 0;
            //监视器在线数
            int monitorOnlineCount = 0;

            //统计主机数量
            totalHostDeviceCount = DeviceEntity.getInstance().where("CSId", CSId).where("isHost", 1).count("1");

            //统计从机数量
            String[] deviceIds = DeviceEntity.getInstance().where("CSId", CSId).where("isHost", 0)//主机：0=否，1=是
                    .selectForStringArray("id");
            //统计从机充电端口数量
            if (deviceIds.length > 0) {
                totalChargeDeviceCount = deviceIds.length;

                //总的插座
                totalSocket = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).count();

                //充电位使用中数量
                deviceSocketUsingCount = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).whereIn("status", "1,5")//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位空闲中数量
                deviceSocketIdleCount = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).where("status", 0)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位占用中数量
                deviceSocketOccupiedCount = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).whereIn("status", "2,3")//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
                //充电位异常数量
                deviceSocketErrorCount = DeviceSocketEntity.getInstance().whereIn("deviceId", deviceIds).where("status", 4)//状态：0=空闲，1=充电中，2=未启动充电，3=已充满电，4=故障，5=浮充
                        .count();
            }
            data.put("totalHostDeviceCount", totalHostDeviceCount);
            data.put("totalChargeDeviceCount", totalChargeDeviceCount);
            data.put("totalSocket", totalSocket);

            data.put("deviceSocketUsingCount", deviceSocketUsingCount);
            data.put("deviceSocketIdleCount", deviceSocketIdleCount);
            data.put("deviceSocketOccupiedCount", deviceSocketOccupiedCount);
            data.put("deviceSocketErrorCount", deviceSocketErrorCount);

            //endregion

            //region 监视器设备数、监视器在线数

            monitorTotalCount = MonitorDeviceEntity.getInstance().where("CSId", CSId).where("status", ">=", 0)//状态：-1-删除，0-离线，1-在线
                    .count();

            monitorOnlineCount = MonitorDeviceEntity.getInstance().where("CSId", CSId).where("status", 1)//状态：-1-删除，0-离线，1-在线
                    .count();

            data.put("monitorTotalCount", monitorTotalCount);
            data.put("monitorOnlineCount", monitorOnlineCount);
            //endregion

            //region 最小功率、最大功率、平均功率
            Map<String, Object> minmaxavg = orderEntity.field("IFNULL(MAX(maxPower),0) as maxPower ,IFNULL(MIN(maxPower),0) as minPower,IFNULL(AVG(maxPower),0) as avgPower").where("CSId", CSId).where("status", 2).where("isTest", 0).find();
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
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
//                    .find();
            Map<String, Object> rechargeData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as recharge_order_count"
                            + ",IFNULL(COUNT(DISTINCT uid),0) AS recharge_users"
                            + ",IFNULL(SUM(order_price),0) AS recharge_amount")
                    .where("CSId", CSId)
                    .where("product_type", "recharge")
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .find();
            rechargeAmount = MapUtil.getBigDecimal(rechargeData, "recharge_amount");
            data.put("rechargeOrderCount", rechargeData.get("recharge_order_count"));
            data.put("rechargeAmount", rechargeAmount);

            //充值退款订单数、退款订单金额
//            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("CSId", CSId).where("isTest", 0)
//                    .find();
            Map<String, Object> rechargeRefundData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(), "co", "cor.order_sn = co.order_sn")
                    .where("co.CSId", CSId)
                    .where("co.product_type", "recharge")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .find();
            rechargeRefundAmount = MapUtil.getBigDecimal(rechargeRefundData, "refund_amount");
            data.put("rechargeRefundOrderCount", rechargeRefundData.get("refund_order_count"));
            data.put("rechargeRefundAmount", rechargeRefundAmount);

            //endregion

            //region 充电卡订单总数、充电卡订单金额、退款订单数、退款订单金额         【废弃】- 全额退款订单数、全额退款金额、部分退款订单数、部分退款金额

//            Map<String, Object> chargecard_data = UserChargeCardOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as chargeCardOrderCount,IFNULL(SUM(totalAmount),0) AS chargeCardAmount")
//                    .whereIn("status", "1,2,3") //状态;0=等待支付，1=支付成功，2=全额退款，3=部分退款
//                    .where("CSId", CSId)
//                    .where("isTest", 0)
//                    .find();
            Map<String, Object> chargeCardOrderData = ConsumeOrdersEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as charge_card_order_count"
                            + ",IFNULL(SUM(order_price),0) AS charge_card_amount")
                    .where("CSId", CSId)
                    .where("payment_status", 2) // 支付状态;1=未支付 2=已完成 -1=已取消
                    .where("product_type", "charge_card")
                    .find();
            chargeCardAmount = MapUtil.getBigDecimal(chargeCardOrderData, "charge_card_amount");
            data.put("chargeCardOrderCount", chargeCardOrderData.get("charge_card_order_count"));
            data.put("chargeCardAmount", chargeCardAmount);

            //充电卡退款订单数、退款订单金额
//            Map<String, Object> chargecard_refund_data = UserChargeCardRefundOrderEntity.getInstance()
//                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
//                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
//                    .where("CSId", CSId).where("isTest", 0)
//                    .find();
            Map<String, Object> chargeCardRefundOrderData = ConsumeOrderRefundsEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as refund_order_count,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .alias("cor")
                    .join(ConsumeOrdersEntity.getInstance().theTableName(),"co","cor.order_sn = co.order_sn")
                    .where("co.CSId", CSId)
                    .where("co.product_type", "charge_card")
                    .where("cor.status", "SUCCESS") // 退款状态 PENDING SUCCESS FAILED;
                    .find();
            chargeCardRefundAmount = MapUtil.getBigDecimal(chargeCardRefundOrderData, "refund_amount");
            data.put("chargeCardRefundOrderCount", MapUtil.getInt(chargeCardRefundOrderData, "refund_order_count", 0));
            data.put("chargeCardRefundAmount", chargeCardRefundAmount);

            //endregion

            //此充电桩运行中的充电端口
            if (totalSocket > 0) {
                //region (次/插座)次数使用率（APR）：所有充电次数 / 全平台运行中的充电端口（不含私有桩）
                BigDecimal chargeCountUseRate = BigDecimal.valueOf(totalUseCount)
                        .divide(BigDecimal.valueOf(totalSocket), 6, RoundingMode.HALF_UP);
                data.put("chargeCountUseRate", chargeCountUseRate);
                //endregion


                //region (%)时长使用率（APR）：所有充电时长「秒数」 / （不含私有桩）(充电桩A总端口运行时间 + 充电桩B总端口运行时间...)
                //此充电桩总端口所有的运行时间 = 充电桩的运营时间 * 端口数

                long totalSocketRunTime = 0;
                //先判断此充电桩上线是否在统计时间内，不在的话，则不进行统计(这里实际永远不会出现，除非有人设定好上线时间)
                if (chargeStationEntity.online_time <= TimeUtil.getTimestamp()) {
                    //应该以上线时间来进行计算,注意时间戳为毫秒级
                    totalSocketRunTime += (TimeUtil.getTimestamp() - chargeStationEntity.online_time) / 1000 * totalSocket;
                }

                BigDecimal chargeTimeUseRate = BigDecimal.valueOf(totalChargeTime).divide(BigDecimal.valueOf(totalSocketRunTime), 6, RoundingMode.HALF_UP);

                data.put("totalSocketRunTime", totalSocketRunTime);
                data.put("chargeTimeUseRate", chargeTimeUseRate);
                //endregion


                //region 创造的收益(元/日/插座): 每日净收入的平均值
                BigDecimal netIncome = new BigDecimal(0);
                Map<String, Object> dailySummaryData = ChargeStationDailySummaryEntity.getInstance().field("IFNULL(AVG(netIncome),0) AS netIncome").where("CSId", CSId).where("date_time", "<=", TimeUtil.getTimestamp()).find();
                if (dailySummaryData != null && dailySummaryData.size() > 0) {
                    netIncome = MapUtil.getBigDecimal(dailySummaryData, "netIncome");
                }
                data.put("netIncome", netIncome);
                //endregion

                //region 累计创造的收益(元/插座): 每日净收入的合计
                BigDecimal totalNetIncome = new BigDecimal(0);
                Map<String, Object> totalNetIncomeData = ChargeStationDailySummaryEntity.getInstance()
                        .field("IFNULL(SUM(netIncome),0) AS totalNetIncome")
                        .where("CSId", CSId)
                        .where("date_time", "<=", TimeUtil.getTimestamp())
                        .find();
                if (totalNetIncomeData != null && totalNetIncomeData.size() > 0) {
                    totalNetIncome = MapUtil.getBigDecimal(totalNetIncomeData, "totalNetIncome");
                }
                data.put("totalNetIncome", totalNetIncome);
                //endregion
            }

            //region 同步充电桩的部分信息
//            Map<String, Object> cs_data = new LinkedHashMap<>();
//            cs_data.put("totalSocket", totalSocket);
//            int noquery = ChargeStationEntity.getInstance()
//                    .where("CSId", CSId)
//                    .update(cs_data);
//            if (noquery > 0) {
//                System.out.println(String.format("CSId=%s 插座数：%s 同步成功", CSId, totalSocket));
//            } else {
//                System.out.println(String.format("CSId=%s 插座数：%s 同步失败", CSId, totalSocket));
//            }
            ChargeStationEntity.getInstance().syncSocketCount(chargeStationEntity.CSId);
            //endregion

            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("CSId", CSId).exist()) {
                getInstance().where("CSId", CSId).update(data);
            } else {
                data.put("CSId", CSId);
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "充电桩数据汇总", "汇总数据发生错误，CSId=%s", CSId);
        }
        return new SyncResult(0, "");
    }

    /**
     * 这个用来跑充电桩修复汇总数据的（辅助使用）
     */
    public void repairData() {
        List<Map<String, Object>> list = ChargeStationEntity.getInstance().field("id,name,online_time").where("status", 1).where("online_time", ">", 0).select();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map<String, Object> nd = (Map<String, Object>) it.next();
            long CSId = MapUtil.getLong(nd, "id");
            String name = MapUtil.getString(nd, "name");
            long online_time = MapUtil.getLong(nd, "online_time");
            String date = TimeUtil.toTimeString(online_time, "yyyy-MM");

            LogsUtil.info("充电桩数据汇总 - 修复数据任务", "CSId = %s - %s 新增任务从 %s 开始修复", CSId, name, date);
            ThreadUtil.getInstance().execute("", () -> syncData(CSId));
        }
    }
}
