package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.platform.PlatformDaySummaryV2Entity;
import com.evcharge.entity.platform.PlatformMonthSummaryV2Entity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationDaySummaryV2Entity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ChargeStationMonthSummaryV2Entity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.utils.range.Range;
import com.evcharge.utils.range.RangeUtils;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.database.mysql.SQLBuilder;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 数据面板 - 图标
 */
public class DashboardChartService {
    public static DashboardChartService getInstance() {
        return new DashboardChartService();
    }

    /**
     * 充电桩站点数：折线图
     *
     * @param builder 查询组装器
     */
    public ISyncResult getChargeStationCount(@NonNull DashboardQueryBuilder builder, boolean month_mode) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:ChargeStationCount:%s:%s", builder.getUniqueKey(), month_mode ? "day" : "month");
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            String field = "";
            field += "FROM_UNIXTIME(date_time / 1000, '%Y-%m-%d') AS date";
            field += ",SUM(charge_station_count) AS charge_station_count"; // 站点数

            ISqlDBObject dbObject = DataService.getDB();
            if (month_mode) {
                dbObject.name(PlatformMonthSummaryV2Entity.getInstance().theTableName());
            } else {
                dbObject.name(PlatformDaySummaryV2Entity.getInstance().theTableName());
            }

            dbObject.field(field)
                    .where("date_time", ">=", builder.start_time)
                    .where("date_time", "<=", builder.end_time)
                    .group("date_time")
                    .order("date_time");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                dbObject.whereIn("organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                dbObject.whereIn("platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                dbObject.whereIn("op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                dbObject.whereIn("partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                dbObject.whereIn("street_code", street_code);
            }

            // endregion

            // region 参数获取及计算
            list = dbObject.select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            for (Map<String, Object> data : list) {
                // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
                BigDecimal gross_income_amount = BigDecimal.ZERO;
                // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
                BigDecimal refund_amount = BigDecimal.ZERO;
                // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
                BigDecimal net_income_amount = BigDecimal.ZERO;
                // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
                BigDecimal consumption_amount = BigDecimal.ZERO;
                // 电费金额
                BigDecimal electricity_fee = null;
                // 分润金额
                BigDecimal profit_share_amount = null;
                // 其他成本金额
                BigDecimal other_cost_amount = null;
                // 毛利金额 = 毛收入金额 - 电费金额 - 分润金额 - 其他成本金额
                BigDecimal gross_profit_amount = null;

                BigDecimal recharge_amount = MapUtil.getBigDecimal(data, "recharge_amount");
                BigDecimal recharge_adjust_amount = MapUtil.getBigDecimal(data, "recharge_adjust_amount");
                BigDecimal recharge_refund_amount = MapUtil.getBigDecimal(data, "recharge_refund_amount");
                BigDecimal recharge_refund_adjust_amount = MapUtil.getBigDecimal(data, "recharge_refund_adjust_amount");

                BigDecimal charge_card_amount = MapUtil.getBigDecimal(data, "charge_card_amount");
                BigDecimal charge_card_refund_amount = MapUtil.getBigDecimal(data, "charge_card_refund_amount");

                BigDecimal pay_per_charge_amount = MapUtil.getBigDecimal(data, "pay_per_charge_amount");
                BigDecimal pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(data, "pay_per_adjustment_charge_amount");

                BigDecimal card_charge_amount = MapUtil.getBigDecimal(data, "card_charge_amount");
                BigDecimal card_adjustment_charge_amount = MapUtil.getBigDecimal(data, "card_adjustment_charge_amount");

                // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
                gross_income_amount = recharge_amount
                        .subtract(recharge_adjust_amount)
                        .add(charge_card_amount);

                // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
                refund_amount = recharge_refund_amount
                        .add(recharge_refund_adjust_amount)
                        .add(charge_card_refund_amount);

                // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
                net_income_amount = recharge_amount
                        .add(charge_card_amount)
                        .add(refund_amount);

                // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
                consumption_amount = pay_per_charge_amount
                        .subtract(pay_per_adjustment_charge_amount)
                        .add(card_charge_amount)
                        .subtract(card_adjustment_charge_amount);

                // region 毛利金额 = 毛收入金额 - 电费金额 - 分润金额 - 其他成本金额
                electricity_fee = MapUtil.getBigDecimal(data, "electricity_fee", null);
                gross_profit_amount = gross_income_amount;
                if (electricity_fee != null) {
                    gross_profit_amount = gross_profit_amount.subtract(electricity_fee);
                }
                if (profit_share_amount != null) {
                    gross_profit_amount = gross_profit_amount.subtract(profit_share_amount);
                }
                if (other_cost_amount != null) {
                    gross_profit_amount = gross_profit_amount.subtract(other_cost_amount);
                }
                // endregion

                data.remove("recharge_amount");
                data.remove("recharge_refund_amount");

                data.remove("charge_card_amount");
                data.remove("charge_card_refund_amount");

                data.remove("pay_per_charge_amount");
                data.remove("pay_per_adjustment_charge_amount");

                data.remove("card_charge_amount");
                data.remove("card_adjustment_charge_amount");

                data.remove("recharge_adjust_amount");
                data.remove("recharge_refund_adjust_amount");

                data.put("gross_income_amount", gross_income_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("refund_amount", refund_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("net_income_amount", net_income_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("consumption_amount", consumption_amount.setScale(2, RoundingMode.HALF_UP));
                if (electricity_fee != null) {
                    data.put("electricity_fee", electricity_fee.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("electricity_fee", "--");
                }
                if (profit_share_amount != null) {
                    data.put("profit_share_amount", profit_share_amount.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("profit_share_amount", "--");
                }
                if (other_cost_amount != null) {
                    data.put("other_cost_amount", other_cost_amount.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("other_cost_amount", "--");
                }
                data.put("gross_profit_amount", gross_profit_amount.setScale(2, RoundingMode.HALF_UP));
            }

            DataService.getMainCache().setList(cacheKey, list, ECacheTime.MINUTE * 5);
            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 平台收益：折线图
     *
     * @param builder 查询条件
     */
    public ISyncResult getIncome(@NonNull DashboardQueryBuilder builder, boolean month_mode) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:Income:%s:%s", builder.getUniqueKey(), month_mode ? "day" : "month");
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            // region 字段
            String field = "";
            field += "date";
            field += ",SUM(recharge_amount) AS recharge_amount"; // 充值金额
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

            ISqlDBObject dbObject = DataService.getDB();
//            if (month_mode && TimeUtil.isNaturalMonthRange(builder.start_time, builder.end_time)) {
            if (month_mode) {
                dbObject.name(ChargeStationMonthSummaryV2Entity.getInstance().theTableName());
                field += ",SUM(recharge_adjust_amount) AS recharge_adjust_amount"; // 充值调整金额
                field += ",SUM(recharge_refund_adjust_amount) AS recharge_refund_adjust_amount"; // 充值退款调整金额
                field += ",SUM(electricity_fee) AS electricity_fee"; // 电费
            } else {
                dbObject.name(ChargeStationDaySummaryV2Entity.getInstance().theTableName());
            }

            dbObject.field(field)
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .group("date")
                    .order("date");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                dbObject.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                dbObject.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                dbObject.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                dbObject.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                dbObject.whereIn("cs.street_code", street_code);
            }

            // endregion

            // region 参数获取及计算
            list = dbObject.select();

//            LogsUtil.info("", "%s", dbObject.theLastSql());

            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            for (Map<String, Object> data : list) {
                // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
                BigDecimal gross_income_amount = BigDecimal.ZERO;
                // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
                BigDecimal refund_amount = BigDecimal.ZERO;
                // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
                BigDecimal net_income_amount = BigDecimal.ZERO;
                // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
                BigDecimal consumption_amount = BigDecimal.ZERO;
                // 电费金额
                BigDecimal electricity_fee = null;
                // 分润金额
                BigDecimal profit_share_amount = null;
                // 其他成本金额
                BigDecimal other_cost_amount = null;
                // 毛利金额 = 毛收入金额 - 电费金额 - 分润金额 - 其他成本金额
                BigDecimal gross_profit_amount = null;

                BigDecimal recharge_amount = MapUtil.getBigDecimal(data, "recharge_amount");
                BigDecimal recharge_adjust_amount = MapUtil.getBigDecimal(data, "recharge_adjust_amount");
                BigDecimal recharge_refund_amount = MapUtil.getBigDecimal(data, "recharge_refund_amount");
                BigDecimal recharge_refund_adjust_amount = MapUtil.getBigDecimal(data, "recharge_refund_adjust_amount");

                BigDecimal charge_card_amount = MapUtil.getBigDecimal(data, "charge_card_amount");
                BigDecimal charge_card_refund_amount = MapUtil.getBigDecimal(data, "charge_card_refund_amount");

                BigDecimal pay_per_charge_amount = MapUtil.getBigDecimal(data, "pay_per_charge_amount");
                BigDecimal pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(data, "pay_per_adjustment_charge_amount");

                BigDecimal card_charge_amount = MapUtil.getBigDecimal(data, "card_charge_amount");
                BigDecimal card_adjustment_charge_amount = MapUtil.getBigDecimal(data, "card_adjustment_charge_amount");

                // 毛收入金额 = 充值金额 - 充值调整金额 + 充电卡金额
                gross_income_amount = recharge_amount
                        .subtract(recharge_adjust_amount)
                        .add(charge_card_amount);

                // 退款金额 = 充值退款金额 - 充值退款调整金额 + 充电卡退款金额
                refund_amount = recharge_refund_amount
                        .add(recharge_refund_adjust_amount)
                        .add(charge_card_refund_amount);

                // 净收入金额 = 充值金额 + 购买充电卡金额 - 退款金额
                net_income_amount = recharge_amount
                        .add(charge_card_amount)
                        .add(refund_amount);

                // 消费金额 = 计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额
                consumption_amount = pay_per_charge_amount
                        .subtract(pay_per_adjustment_charge_amount)
                        .add(card_charge_amount)
                        .subtract(card_adjustment_charge_amount);

                // region 毛利金额 = 毛收入金额 - 电费金额 - 分润金额 - 其他成本金额
                electricity_fee = MapUtil.getBigDecimal(data, "electricity_fee", null);
                gross_profit_amount = gross_income_amount;
                if (electricity_fee != null) {
                    gross_profit_amount = gross_profit_amount.subtract(electricity_fee);
                }
                if (profit_share_amount != null) {
                    gross_profit_amount = gross_profit_amount.subtract(profit_share_amount);
                }
                if (other_cost_amount != null) {
                    gross_profit_amount = gross_profit_amount.subtract(other_cost_amount);
                }
                // endregion

                data.remove("recharge_amount");
                data.remove("recharge_refund_amount");

                data.remove("charge_card_amount");
                data.remove("charge_card_refund_amount");

                data.remove("pay_per_charge_amount");
                data.remove("pay_per_adjustment_charge_amount");

                data.remove("card_charge_amount");
                data.remove("card_adjustment_charge_amount");

                data.remove("recharge_adjust_amount");
                data.remove("recharge_refund_adjust_amount");

                data.put("gross_income_amount", gross_income_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("refund_amount", refund_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("net_income_amount", net_income_amount.setScale(2, RoundingMode.HALF_UP));
                data.put("consumption_amount", consumption_amount.setScale(2, RoundingMode.HALF_UP));
                if (electricity_fee != null) {
                    data.put("electricity_fee", electricity_fee.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("electricity_fee", "--");
                }
                if (profit_share_amount != null) {
                    data.put("profit_share_amount", profit_share_amount.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("profit_share_amount", "--");
                }
                if (other_cost_amount != null) {
                    data.put("other_cost_amount", other_cost_amount.setScale(2, RoundingMode.HALF_UP));
                } else {
                    data.put("other_cost_amount", "--");
                }
                data.put("gross_profit_amount", gross_profit_amount.setScale(2, RoundingMode.HALF_UP));
            }

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setList(cacheKey, list, ECacheTime.MINUTE * 5);
            }
            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 用户累计充电次数分布图
     *
     * @param builder 查询组装器
     */
    public ISyncResult getUserTotalChargeCount(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:UserChargeCount:%s", builder.getUniqueKey());
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            // 用于组装查询条件
            String SQL_OPTIONAL_WHERE_EXPRESSION = "";

            String cs_table = ChargeStationEntity.getInstance().theTableName();
            // 获取最大累计充电次数，然后动态计算出分布图维度
            ChargeOrderEntity maxCountEntity = ChargeOrderEntity.getInstance();
            maxCountEntity.field("uid, COUNT(*) AS count")
                    .alias("co")
                    .join(cs_table, "cs", "co.CSId = cs.CSId")
                    .whereIn("co.status", "1,2")
                    .where("co.startTime", ">=", builder.start_time)
                    .where("co.startTime", "<=", builder.end_time)
                    .group("uid")
                    .order("count DESC")
                    .limit(1);

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                maxCountEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.organize_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.organize_code.split(",")));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                maxCountEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.platform_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.platform_code.split(",")));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                maxCountEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.op_mode_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.op_mode_code.split(",")));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                maxCountEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.partner_type_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.partner_type_code.split(",")));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                maxCountEntity.whereIn("cs.street_code", street_code);
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.street_code IN (%s)", SQLBuilder.getWhereInString(street_code));
            }

            // endregion

            // region 组装SQL语句
            List<Range<Integer>> ranges = new LinkedList<>();
            ranges.add(new Range<>(0, 1, 3));
            ranges.add(new Range<>(1, 3, 10));
            if (TimeUtil.getDaysDiff(builder.start_time, builder.end_time) <= 31) {
                ranges.add(new Range<>(2, 10, 30));
            } else {
                Map<String, Object> maxCountData = maxCountEntity.find();
                int maxCount = MapUtil.getInt(maxCountData, "count", 90);
                ranges = RangeUtils.generateRangesInt(ranges, 10, maxCount / 2, 2, 100);
            }
            StringBuilder SQL_CASE_EXPRESSION = new StringBuilder();
            StringBuilder SQL_CASE_EXPRESSION_SORT = new StringBuilder();
            int maxCount = 0;
            int maxIndex = 0;
            if (!ranges.isEmpty()) {
                SQL_CASE_EXPRESSION = new StringBuilder("CASE \r");
                SQL_CASE_EXPRESSION_SORT = new StringBuilder("CASE \r");
                for (Range<Integer> range : ranges) {
                    maxCount = range.max;
                    maxIndex = range.index;
                    //检查是否是最后的
                    SQL_CASE_EXPRESSION.append(String.format("WHEN count >= %s AND count <= %s THEN '%s~%s次' \n"
                            , range.min - 1
                            , range.max
                            , range.min
                            , range.max
                    ));
                    SQL_CASE_EXPRESSION_SORT.append(String.format("WHEN count >= %s AND count <= %s THEN %s \n"
                            , range.min - 1
                            , range.max
                            , range.index
                    ));
                }
            }
            if (SQL_CASE_EXPRESSION.length() > 0 && SQL_CASE_EXPRESSION_SORT.length() > 0) {
                SQL_CASE_EXPRESSION.append(String.format("ELSE '%s次以上' END AS text \n", maxCount));
                SQL_CASE_EXPRESSION_SORT.append(String.format("ELSE %s END AS sort_order\n", maxIndex + 1));
                SQL_CASE_EXPRESSION.append(String.format(",%s", SQL_CASE_EXPRESSION_SORT));
            }

            String SQL = "SELECT text,\n" +
                    "       COUNT(*) AS count_value\n" +
                    " FROM (SELECT @SQL_CASE_EXPRESSION" +
                    "      FROM (SELECT uid, COUNT(*) AS count\n" +
                    "            FROM ChargeOrder co\n" +
                    "                     JOIN ChargeStation cs ON cs.CSId = co.CSId\n" +
                    "            WHERE co.status IN (1, 2)\n" +
                    "              AND co.startTime BETWEEN @start_time AND @end_time\n" +
                    "              @SQL_OPTIONAL_WHERE_EXPRESSION \n" +
                    "            GROUP BY uid) AS user_charge_counts) AS categorized_counts\n" +
                    " GROUP BY text, sort_order\n" +
                    " ORDER BY sort_order;";
            SQL = SQL
                    .replaceAll("@SQL_CASE_EXPRESSION", SQL_CASE_EXPRESSION.toString())
                    .replaceAll("@start_time", String.format("%s", builder.start_time))
                    .replaceAll("@end_time", String.format("%s", builder.end_time))
                    .replaceAll("@SQL_OPTIONAL_WHERE_EXPRESSION", SQL_OPTIONAL_WHERE_EXPRESSION)
            ;
            // endregion

            list = DataService.getDB().query(SQL);
            if (list == null || list.isEmpty()) return new SyncResult(1, "");
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setList(cacheKey, list, ECacheTime.HOUR);
            } else DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 用户充电时长分布图
     *
     * @param builder 查询组装器
     */
    public ISyncResult getUserChargeTime(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:UserChargeTime:%s", builder.getUniqueKey());
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            // 用于组装查询条件
            String SQL_OPTIONAL_WHERE_EXPRESSION = "";

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.organize_code IN (%s)", SQLBuilder.getWhereInString(builder.organize_code.split(",")));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.platform_code IN (%s)", SQLBuilder.getWhereInString(builder.platform_code.split(",")));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.op_mode_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.op_mode_code.split(",")));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.partner_type_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.partner_type_code.split(",")));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.street_code IN (%s)"
                        , SQLBuilder.getWhereInString(street_code));
            }

            // region 组装SQL语句
            String SQL = "SELECT CASE\n" +
                    "           WHEN chargeTime <= 3600 THEN '0~1小时'\n" +
                    "           WHEN chargeTime > 3600 AND chargeTime <= 7200 THEN '1~2小时'\n" +
                    "           WHEN chargeTime > 7200 AND chargeTime <= 10800 THEN '2~3小时'\n" +
                    "           WHEN chargeTime > 10800 AND chargeTime <= 14400 THEN '3~4小时'\n" +
                    "           WHEN chargeTime > 14400 AND chargeTime <= 21600 THEN '4~6小时'\n" +
                    "           WHEN chargeTime > 21600 AND chargeTime <= 28800 THEN '6~8小时'\n" +
                    "           ELSE '8小时以上'\n" +
                    "           END  AS text,\n" +
                    "       COUNT(*) AS count_value\n" +
                    "FROM ChargeOrder co\n" +
                    "    JOIN ChargeStation cs ON co.CSId = cs.CSId\n" +
                    "WHERE co.status IN (1, 2)" +
                    "  AND startTime BETWEEN @start_time AND @end_time\n" +
                    "  @SQL_OPTIONAL_WHERE_EXPRESSION" +
                    "GROUP BY text\n" +
                    "ORDER BY FIELD(text, '0~1小时', '1~2小时', '2~3小时', '3~4小时', '4~6小时', '6~8小时', '8小时以上');";
            SQL = SQL
                    .replaceAll("@start_time", String.format("%s", builder.start_time))
                    .replaceAll("@end_time", String.format("%s", builder.end_time))
                    .replaceAll("@SQL_OPTIONAL_WHERE_EXPRESSION", SQL_OPTIONAL_WHERE_EXPRESSION)
            ;
            // endregion

            list = DataService.getDB().query(SQL);
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setList(cacheKey, list, ECacheTime.HOUR);
            } else DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 用户充电峰值功率分布图
     *
     * @param builder 查询组装器
     */
    public ISyncResult getUserChargeMapPower(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:UserChargeMaxPower:%s", builder.getUniqueKey());
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            // 用于组装查询条件
            String SQL_OPTIONAL_WHERE_EXPRESSION = "";

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.organize_code IN (%s)", SQLBuilder.getWhereInString(builder.organize_code.split(",")));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.platform_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.platform_code.split(",")));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.op_mode_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.op_mode_code.split(",")));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.partner_type_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.partner_type_code.split(",")));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.street_code IN (%s)", SQLBuilder.getWhereInString(street_code));
            }

            // region 组装SQL语句
            String SQL = "SELECT CASE\n" +
                    "           WHEN maxPower <= 210 THEN '0~210W'\n" +
                    "           WHEN maxPower > 210 AND maxPower <= 300 THEN '210~300W'\n" +
                    "           WHEN maxPower > 300 AND maxPower <= 500 THEN '300~500W'\n" +
                    "           WHEN maxPower > 500 AND maxPower <= 1000 THEN '500~1000W'\n" +
                    "           ELSE '1000W以上'\n" +
                    "           END  AS text,\n" +
                    "       COUNT(*) AS count_value\n" +
                    "FROM ChargeOrder co\n" +
                    "    JOIN ChargeStation cs ON co.CSId = cs.CSId\n" +
                    "WHERE co.status IN (1, 2)" +
                    "  AND startTime BETWEEN @start_time AND @end_time\n" +
                    "  @SQL_OPTIONAL_WHERE_EXPRESSION" +
                    "GROUP BY text\n" +
                    "ORDER BY FIELD(text, '0-210W', '210W-300W', '300W-500W', '500W-1000W', '1000W以上');";
            SQL = SQL
                    .replace("@start_time", String.format("%s", builder.start_time))
                    .replace("@end_time", String.format("%s", builder.end_time))
                    .replace("@SQL_OPTIONAL_WHERE_EXPRESSION", SQL_OPTIONAL_WHERE_EXPRESSION)
            ;
            // endregion

            list = DataService.getDB().query(SQL);
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setList(cacheKey, list, ECacheTime.HOUR);
            } else DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 用户充电时段分布图分布图
     *
     * @param builder 查询组装器
     */
    public ISyncResult getUserChargingTimeRange(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;
        try {
            String cacheKey = String.format("DashboardV1:Chart:UserChargingTimeRange:%s", builder.getUniqueKey());
            List<Map<String, Object>> list = DataService.getMainCache().getList(cacheKey);
            if (list != null && !list.isEmpty()) {
                return new SyncResult(0, "", list);
            }

            // 用于组装查询条件
            String SQL_OPTIONAL_WHERE_EXPRESSION = "";

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.organize_code IN (%s)", SQLBuilder.getWhereInString(builder.organize_code.split(",")));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.platform_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.platform_code.split(",")));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.op_mode_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.op_mode_code.split(",")));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.partner_type_code IN (%s)"
                        , SQLBuilder.getWhereInString(builder.partner_type_code.split(",")));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format("AND cs.street_code IN (%s)"
                        , SQLBuilder.getWhereInString(street_code));
            }

            // region 组装SQL语句
            String SQL = "SELECT CASE\n" +
//                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 0 AND 5 THEN '凌晨(00:00-06:00)'\n" +
//                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 5 AND 11 THEN '上午(06:00-12:00)'\n" +
//                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 11 AND 17 THEN '下午(12:00-18:00)'\n" +
//                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 17 AND 23 THEN '晚上(18:00-24:00)'\n" +
                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 0 AND 5 THEN '凌晨'\n" +
                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 5 AND 11 THEN '上午'\n" +
                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 11 AND 17 THEN '下午'\n" +
                    "           WHEN HOUR(FROM_UNIXTIME(startTime / 1000)) BETWEEN 17 AND 23 THEN '晚上'\n" +
                    "           ELSE '未知'\n" +
                    "           END      AS text,\n" +
                    "       COUNT(co.id) AS count_value\n" +
                    "FROM ChargeOrder co\n" +
                    "         JOIN ChargeStation cs ON co.CSId = cs.CSId\n" +
                    "WHERE co.status IN (1, 2)\n" +
                    "  @SQL_OPTIONAL_WHERE_EXPRESSION" +
                    "  AND startTime BETWEEN @start_time AND @end_time\n" +
                    "GROUP BY text\n" +
//                    "ORDER BY FIELD(text, '凌晨(00:00-06:00)','上午(06:00-12:00)','下午(12:00-18:00)','晚上(18:00-24:00)');";
                    "ORDER BY FIELD(text, '凌晨','上午','下午','晚上');";
            SQL = SQL
                    .replace("@start_time", String.format("%s", builder.start_time))
                    .replace("@end_time", String.format("%s", builder.end_time))
                    .replace("@SQL_OPTIONAL_WHERE_EXPRESSION", SQL_OPTIONAL_WHERE_EXPRESSION)
            ;
            // endregion

            list = DataService.getDB().query(SQL);
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setList(cacheKey, list, ECacheTime.HOUR);
            } else DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

            return new SyncResult(0, "", list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取平台收益射线图数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取充电端口平均收益（元/插座/日或月）
     */
    public ISyncResult getSocketAvgNetIncome(@NonNull DashboardQueryBuilder builder, boolean month_mode) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String cacheKey = String.format("DashboardV1:Chart:SocketNetIncome:%s:%s", builder.getUniqueKey(), month_mode ? "month" : "day");
        List<Map<String, Object>> list;
        if (month_mode) {
            list = PlatformMonthSummaryV2Entity.getInstance()
                    .field("id,date,date_time,net_income,socket_consumption")
                    .cache(cacheKey, ECacheTime.MINUTE * 10)
                    .where("organize_code", builder.organize_code)
                    .where("date_time", ">=", builder.start_time)
                    .where("date_time", "<", builder.end_time)
                    .order("date_time")
                    .select();
        } else {
            list = PlatformDaySummaryV2Entity.getInstance()
                    .field("id,date,date_time,net_income,socket_consumption")
                    .cache(cacheKey, ECacheTime.MINUTE * 10)
                    .where("organize_code", builder.organize_code)
                    .where("date_time", ">=", builder.start_time)
                    .where("date_time", "<", builder.end_time)
                    .order("date_time")
                    .select();
        }
        if (list == null || list.isEmpty()) return new SyncResult(1, "");
        return new SyncResult(0, "", list);
    }
}
