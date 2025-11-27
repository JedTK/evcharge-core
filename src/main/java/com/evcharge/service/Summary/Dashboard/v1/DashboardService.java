package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.station.ChargeStationDaySummaryV2Entity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ChargeStationMonthSummaryV2Entity;
import com.evcharge.entity.station.bill.EMeterToCStationEntity;
import com.evcharge.entity.station.bill.ElectricityPowerSupplyBillEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据仪表板
 * <p>
 * 筛选条件：组织代码、品牌、模式、合作方、区域、日期
 * 展示数据：
 * 1、平台收益：毛收入、退款、净收入、消费金额、电费、分润、毛利、折线图
 * 2、场站数据：场站总数、端口总数、电表总数、新增场站
 * 4、排行榜：充电次数最多站点、新增用户最多站点、收入金额最高的站点、消费金额最高的站点
 * 5、用户统计：新增数、活跃数、总数、平均充值金额、平均消费金额、充电卡-新增数、充电卡-总数
 * 6、用户累计充电次数分布图
 * 7、用户累计充电时长分布图
 * 8、用户充电峰值功率分布图
 * 9、用户充电时段分布图
 */
public class DashboardService {

    protected final static String TAG = "数据仪表v2.0";

    /**
     * 100的BigDecimal计算值，用于计算百分比，减少实例化
     */
    private final static BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public static DashboardService getInstance() {
        return new DashboardService();
    }

    /**
     * 平台收益：毛收入、退款、净收入、消费金额、电费、分润、毛利
     *
     * @param builder 查询条件
     */
    public ISyncResult getIncome(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        try {
            String cacheKey = String.format("DashboardV1:Income:Summary:%s", builder.getUniqueKey());
            Map<String, Object> cb_data = DataService.getMainCache().getMap(cacheKey);
            if (cb_data != null && !cb_data.isEmpty()) return new SyncResult(0, "", cb_data);

            ChargeStationDaySummaryV2Entity daySummaryV2Entity = new ChargeStationDaySummaryV2Entity();
            // region 字段
            String field = "";
            field += "SUM(recharge_amount) AS recharge_amount"; // 充值金额
//        field += ",SUM(recharge_adjust_amount) AS recharge_adjust_amount"; // 充值调整金额
            field += ",SUM(recharge_refund_amount) AS recharge_refund_amount"; // 充值退款金额
//        field += ",SUM(recharge_refund_adjust_amount) AS recharge_refund_adjust_amount"; // 充值退款调整金额

            field += ",SUM(charge_card_amount) AS charge_card_amount"; // 购买充电卡金额
            field += ",SUM(charge_card_refund_amount) AS charge_card_refund_amount";// 购买充电卡退款金额

            field += ",SUM(pay_per_charge_amount) AS pay_per_charge_amount"; // 计次消费金额
            field += ",SUM(pay_per_adjustment_charge_amount) AS pay_per_adjustment_charge_amount"; // 计次消费调整金额

            field += ",SUM(card_charge_amount) AS card_charge_amount"; // 充电卡消费金额
            field += ",SUM(card_adjustment_charge_amount) AS card_adjustment_charge_amount"; // 充电卡消费调整金额
            // endregion
            daySummaryV2Entity
                    .field(field)
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .order("date_time");

            ChargeStationMonthSummaryV2Entity monthSummaryV2Entity = new ChargeStationMonthSummaryV2Entity();
            // 判断开始时间和结束时间是否落在自然月上,否则无法统计到：电费金额、分润金额、其他成本金额
            monthSummaryV2Entity
                    // TODO 后面需要添加：分润金额、其他成本金额
                    .field("SUM(electricity_fee) AS electricity_fee")
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .order("date_time");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                daySummaryV2Entity.whereIn("cs.organize_code", builder.organize_code.split(","));
                monthSummaryV2Entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                daySummaryV2Entity.whereIn("cs.platform_code", builder.platform_code.split(","));
                monthSummaryV2Entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                daySummaryV2Entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                monthSummaryV2Entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                daySummaryV2Entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                monthSummaryV2Entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                daySummaryV2Entity.whereIn("cs.street_code", street_code);
                monthSummaryV2Entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // region 参数获取及计算

            // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
            BigDecimal gross_income_amount = BigDecimal.ZERO;
            // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
            BigDecimal refund_amount = BigDecimal.ZERO;
            // 退款率(%) = 退款金额 / 毛收入金额 * 100
            BigDecimal refund_rate = BigDecimal.ZERO;
            // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
            BigDecimal net_income_amount = BigDecimal.ZERO;
            // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
            BigDecimal consumption_amount = BigDecimal.ZERO;
            // 消费率（%） = 消费金额 / 净收入金额
            BigDecimal consumption_rate = BigDecimal.ZERO;
            // 电费金额
            BigDecimal electricity_fee = null;
            // 电费占比(%) = 电费 / 消费金额
            BigDecimal electricity_fee_rate = BigDecimal.ZERO;
            // 分润金额
            BigDecimal profit_share_amount = null;
            // 其他成本金额
            BigDecimal other_cost_amount = null;
            // 毛利金额 = 消费金额 - 电费金额 - 分润金额 - 其他成本金额
            BigDecimal gross_profit_amount = BigDecimal.ZERO;
            // 毛利率 = 毛利金额 / 消费金额
            BigDecimal gross_profit_rate = BigDecimal.ZERO;

            Map<String, Object> data = daySummaryV2Entity.find();

            // 充值金额
            BigDecimal recharge_amount = MapUtil.getBigDecimal(data, "recharge_amount");
//        BigDecimal recharge_adjust_amount = MapUtil.getBigDecimal(data, "recharge_adjust_amount");
            // 充值退款金额
            BigDecimal recharge_refund_amount = MapUtil.getBigDecimal(data, "recharge_refund_amount");
//        BigDecimal recharge_refund_adjust_amount = MapUtil.getBigDecimal(data, "recharge_refund_adjust_amount");

            // 充电卡金额
            BigDecimal charge_card_amount = MapUtil.getBigDecimal(data, "charge_card_amount");
            // 充电卡退款金额
            BigDecimal charge_card_refund_amount = MapUtil.getBigDecimal(data, "charge_card_refund_amount");

            // 计次消费金额
            BigDecimal pay_per_charge_amount = MapUtil.getBigDecimal(data, "pay_per_charge_amount");
            // 计次消费调整金额
            BigDecimal pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(data, "pay_per_adjustment_charge_amount");
            // 充电卡消费金额
            BigDecimal card_charge_amount = MapUtil.getBigDecimal(data, "card_charge_amount");
            // 充电卡消费调整金额
            BigDecimal card_adjustment_charge_amount = MapUtil.getBigDecimal(data, "card_adjustment_charge_amount");

            // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
            gross_income_amount = recharge_amount
//                .subtract(recharge_adjust_amount)
                    .add(charge_card_amount);

            // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
            refund_amount = recharge_refund_amount
//                .add(recharge_refund_adjust_amount)
                    .add(charge_card_refund_amount);

            // 退款率(%) = 退款金额 / 毛收入金额 * 100
            if (gross_income_amount.compareTo(BigDecimal.ZERO) > 0) {
                refund_rate = refund_amount.abs()
                        .divide(gross_income_amount, 8, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED);
            }

            // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
            net_income_amount = recharge_amount
                    .add(charge_card_amount)
                    .add(refund_amount);

            // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
            consumption_amount = pay_per_charge_amount
                    .subtract(pay_per_adjustment_charge_amount)
                    .add(card_charge_amount)
                    .subtract(card_adjustment_charge_amount);

            // 消费率（%） = 消费金额 / 净收入金额
            if (net_income_amount.compareTo(BigDecimal.ZERO) > 0) {
                consumption_rate = consumption_amount
                        .divide(net_income_amount, 8, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED);
            }

            // region 毛利金额 = 消费金额 - 电费金额 - 分润金额 - 其他成本金额

            Map<String, Object> month_data = monthSummaryV2Entity.find();
//                LogsUtil.warn(TAG, "电费计算：%s", monthSummaryV2Entity.theLastSql());

            electricity_fee = MapUtil.getBigDecimal(month_data, "electricity_fee", null);
            gross_profit_amount = consumption_amount;
            if (electricity_fee != null) {
                gross_profit_amount = gross_profit_amount.subtract(electricity_fee);

                // 电费占比(%) = 电费 / 消费金额
                if (electricity_fee.compareTo(BigDecimal.ZERO) > 0) {
                    electricity_fee_rate = electricity_fee
                            .divide(consumption_amount, 8, RoundingMode.HALF_UP)
                            .multiply(ONE_HUNDRED);
                }
            }
            if (profit_share_amount != null) {
                gross_profit_amount = gross_profit_amount.subtract(profit_share_amount);
            }
            if (other_cost_amount != null) {
                gross_profit_amount = gross_profit_amount.subtract(other_cost_amount);
            }

            // 毛利率 = 毛利金额 / 消费金额
            if (gross_profit_amount.compareTo(BigDecimal.ZERO) > 0) {
                gross_profit_rate = gross_profit_amount
                        .divide(consumption_amount, 8, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED);
            }

            // endregion

            // endregion

            cb_data = new LinkedHashMap<>();
            cb_data.put("gross_income_amount", gross_income_amount.setScale(2, RoundingMode.HALF_UP));
            cb_data.put("refund_amount", refund_amount.setScale(2, RoundingMode.HALF_UP));
            cb_data.put("refund_rate", refund_rate);
            cb_data.put("net_income_amount", net_income_amount.setScale(2, RoundingMode.HALF_UP));
            cb_data.put("consumption_amount", consumption_amount.setScale(2, RoundingMode.HALF_UP));
            cb_data.put("consumption_rate", consumption_rate);
            if (electricity_fee != null) {
                cb_data.put("electricity_fee", electricity_fee.setScale(2, RoundingMode.HALF_UP));
                cb_data.put("electricity_fee_rate", electricity_fee_rate);
            } else {
                cb_data.put("electricity_fee", "--");
            }
            if (profit_share_amount != null) {
                cb_data.put("profit_share_amount", profit_share_amount.setScale(2, RoundingMode.HALF_UP));
            } else {
                cb_data.put("profit_share_amount", "--");
            }
            if (other_cost_amount != null) {
                cb_data.put("other_cost_amount", other_cost_amount.setScale(2, RoundingMode.HALF_UP));
            } else {
                cb_data.put("other_cost_amount", "--");
            }
            cb_data.put("gross_profit_amount", gross_profit_amount.setScale(2, RoundingMode.HALF_UP));
            cb_data.put("gross_profit_rate", gross_profit_rate);

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setMap(cacheKey, cb_data, ECacheTime.MINUTE * 10);
            }
            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 使用数据：累计充电次数、累计充电时长、累计耗电量
     *
     * @param builder 查询条件
     */
    public ISyncResult getUsageData(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        try {
            String cacheKey = String.format("DashboardV1:UsageData:%s", builder.getUniqueKey());
            Map<String, Object> cb_data = DataService.getMainCache().getMap(cacheKey);
            if (cb_data != null && !cb_data.isEmpty()) return new SyncResult(0, "", cb_data);

            String cs_table_name = ChargeStationEntity.getInstance().theTableName();

            // 用于查询累计使用次数和累计使用时长
            ChargeStationDaySummaryV2Entity daySummaryV2Entity = new ChargeStationDaySummaryV2Entity();
            daySummaryV2Entity.field("SUM(total_use_count) AS total_use_count"            // 累计使用次数
                            + ",SUM(total_charge_time) AS total_charge_time"     // 累计使用时长(秒)
                    )
                    .alias("css")
                    .join(cs_table_name, "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .order("date_time");

            // 用于查询耗电量
            ElectricityPowerSupplyBillEntity billEntity = new ElectricityPowerSupplyBillEntity();
            billEntity.field("SUM(power_consumption) AS power_consumption")
                    .alias("bill")
                    .join(EMeterToCStationEntity.getInstance().theTableName(), "mtc", "mtc.meter_id = bill.meter_id")
                    .join(cs_table_name, "cs", "mtc.cs_id = cs.CSId")
                    .where("bill_date_time", ">=", builder.start_time)
                    .where("bill_date_time", "<=", builder.end_time);

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                daySummaryV2Entity.whereIn("cs.organize_code", builder.organize_code.split(","));
                billEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                daySummaryV2Entity.whereIn("cs.platform_code", builder.platform_code.split(","));
                billEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                daySummaryV2Entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                billEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                daySummaryV2Entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                billEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                daySummaryV2Entity.whereIn("cs.street_code", street_code);
                billEntity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // region 参数获取及计算
            Map<String, Object> data = daySummaryV2Entity.find();
            long total_use_count = MapUtil.getLong(data, "total_use_count");
            long total_charge_time = MapUtil.getLong(data, "total_charge_time");

            Map<String, Object> bill_data = billEntity.find();
            double power_consumption = MapUtil.getDouble(bill_data, "power_consumption");

            // endregion

            cb_data = new LinkedHashMap<>();
            cb_data.put("total_use_count", total_use_count);
            cb_data.put("total_charge_time", total_charge_time);
            cb_data.put("power_consumption", power_consumption);
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setMap(cacheKey, cb_data, ECacheTime.MINUTE * 10);
            }
            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }
}

