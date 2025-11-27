package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.station.ChargeStationDaySummaryV2Entity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.mysql.SQLBuilder;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 数据面板 - 用户统计相关
 */
public class DashboardUserService {
    public static DashboardUserService getInstance() {
        return new DashboardUserService();
    }

    // region 查询活跃用户SQL语句
    /**
     * 查询活跃用户SQL语句
     */
    private final static String SQL_ACTIVE_USER_COUNT = "SELECT COUNT(DISTINCT uid) AS active_user_count\n" +
            "FROM (\n" +
            "         SELECT ro.uid\n" +
            "         FROM RechargeOrder ro\n" +
            "                  JOIN ChargeStation cs\n" +
            "                       ON ro.CSId = cs.CSId\n" +
            "         WHERE ro.create_time BETWEEN @start_time AND @end_time\n" +
            "           AND ro.status = 2\n" +
            "           @SQL_OPTIONAL_WHERE_EXPRESSION\n" +
            "\n" +
            "         UNION\n" +
            "\n" +
            "         SELECT ucco.uid\n" +
            "         FROM UserChargeCardOrder ucco\n" +
            "                  JOIN ChargeStation cs\n" +
            "                       ON ucco.CSId = cs.CSId\n" +
            "         WHERE ucco.create_time BETWEEN @start_time AND @end_time\n" +
            "           AND ucco.status = 1\n" +
            "           @SQL_OPTIONAL_WHERE_EXPRESSION\n" +
            "\n" +
            "         UNION\n" +
            "\n" +
            "         SELECT co.uid\n" +
            "         FROM ChargeOrder co\n" +
            "                  JOIN ChargeStation cs\n" +
            "                       ON co.CSId = cs.CSId\n" +
            "         WHERE co.create_time BETWEEN @start_time AND @end_time\n" +
            "           AND co.status = 2\n" +
            "           @SQL_OPTIONAL_WHERE_EXPRESSION\n" +
            ") AS active_users;";
    // endregion

    /**
     * 用户数据：新增、活跃、总数、人均净收入金额、人均消费金额、充电卡-新增、充电卡-总数
     *
     * @param builder 查询条件
     */
    public ISyncResult getSummary(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String cacheKey = String.format("DashboardV1:User:Summary:%s", builder.getUniqueKey());
        Map<String, Object> cb_data = DataService.getMainCache().getMap(cacheKey);
        if (cb_data != null && !cb_data.isEmpty()) {
            return new SyncResult(0, "", cb_data);
        }

        try {
            ChargeStationDaySummaryV2Entity daySummaryV2Entity = ChargeStationDaySummaryV2Entity.getInstance();
            // region 字段
            String field = "";
            field += "SUM(total_registered_users) AS new_user_count"; // 新增用户数
//            field += ",SUM(payment_user_count) AS active_user_count"; // 活跃用户数，因为是日统计出来的，会出现重复的用户数

            field += ",SUM(recharge_amount) AS recharge_amount"; // 充值金额（计算人均净收入金额）
            field += ",SUM(recharge_refund_amount) AS recharge_refund_amount"; // 充值退款金额（计算人均净收入金额）
            field += ",SUM(charge_card_amount) AS charge_card_amount"; // 购买充电卡金额（计算人均净收入金额）
            field += ",SUM(charge_card_refund_amount) AS charge_card_refund_amount";// 购买充电卡退款金额（计算人均净收入金额）

            field += ",SUM(charge_card_order_count) AS charge_card_order_count";// 购买充电卡订单数（计算充电卡新增数）
            field += ",SUM(charge_card_refund_order_count) AS charge_card_refund_order_count";// 购买充电卡退款订单数（计算充电卡新增数）

            field += ",SUM(pay_per_charge_amount) AS pay_per_charge_amount"; // 计次消费金额（计算人均消费金额）
            field += ",SUM(pay_per_adjustment_charge_amount) AS pay_per_adjustment_charge_amount"; // 计次消费调整金额（计算人均消费金额）

            field += ",SUM(card_charge_amount) AS card_charge_amount"; // 充电卡消费金额（计算人均消费金额）
            field += ",SUM(card_adjustment_charge_amount) AS card_adjustment_charge_amount"; // 充电卡消费调整金额（计算人均消费金额）
            // endregion
            daySummaryV2Entity
                    .field(field)
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = css.CSId")
                    .where("date_time", ">=", builder.start_time)
                    .where("date_time", "<=", builder.end_time);

            //计算用户总数和充电卡总数
            ChargeStationDaySummaryV2Entity summaryV2Entity = ChargeStationDaySummaryV2Entity.getInstance();
            summaryV2Entity
                    .field("SUM(total_registered_users) AS total_user_count" // 用户总数
                            + ",SUM(charge_card_order_count) AS total_charge_card_order_count"  // 购买充电卡订单数（计算充电卡总数）
                            + ",SUM(charge_card_refund_order_count) AS total_charge_card_refund_order_count" // 购买充电卡退款订单数（计算充电卡总数）
                    )
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = css.CSId")
                    .where("date_time", "<=", builder.end_time);

            String SQL = SQL_ACTIVE_USER_COUNT
                    .replaceAll("@start_time", String.format("%s", builder.start_time))
                    .replaceAll("@end_time", String.format("%s", builder.end_time));
            String SQL_OPTIONAL_WHERE_EXPRESSION = "";

            // region 注入可选参数

            if (!StringUtil.isEmpty(builder.organize_code)) {
                daySummaryV2Entity.whereIn("cs.organize_code", builder.organize_code.split(","));
                summaryV2Entity.whereIn("cs.organize_code", builder.organize_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format(" AND cs.organize_code IN (%s)\n"
                        , SQLBuilder.getWhereInString(builder.organize_code.split(",")));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                daySummaryV2Entity.whereIn("cs.platform_code", builder.platform_code.split(","));
                summaryV2Entity.whereIn("cs.platform_code", builder.platform_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format(" AND cs.platform_code IN (%s)\n"
                        , SQLBuilder.getWhereInString(builder.platform_code.split(",")));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                daySummaryV2Entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                summaryV2Entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format(" AND cs.op_mode_code IN (%s)\n"
                        , SQLBuilder.getWhereInString(builder.op_mode_code.split(",")));
            }
            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                daySummaryV2Entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                summaryV2Entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format(" AND cs.partner_type_code IN (%s)\n"
                        , SQLBuilder.getWhereInString(builder.partner_type_code.split(",")));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                daySummaryV2Entity.whereIn("cs.street_code", street_code);
                summaryV2Entity.whereIn("cs.street_code", street_code);
                SQL_OPTIONAL_WHERE_EXPRESSION += String.format(" AND cs.street_code IN (%s)\n"
                        , SQLBuilder.getWhereInString(street_code));
            }
            // endregion

            // region 计算及获取参数
            Map<String, Object> data1 = daySummaryV2Entity.find();
            Map<String, Object> data2 = summaryV2Entity.find();

            // 用户总数
            int total_user_count = MapUtil.getInt(data2, "total_user_count");
            // 充电卡总订单数（有多少订单就买了多少张卡）
            int total_charge_card_order_count = MapUtil.getInt(data2, "total_charge_card_order_count");
            // 充电卡总订单退款数
            int total_charge_card_refund_order_count = MapUtil.getInt(data2, "total_charge_card_refund_order_count");

            // 用户新增数
            int new_user_count = MapUtil.getInt(data1, "new_user_count");

            // 充值金额
            BigDecimal recharge_amount = MapUtil.getBigDecimal(data1, "recharge_amount");
            // 充值退款金额
            BigDecimal recharge_refund_amount = MapUtil.getBigDecimal(data1, "recharge_refund_amount");
            // 充电卡金额
            BigDecimal charge_card_amount = MapUtil.getBigDecimal(data1, "charge_card_amount");
            // 充电卡退款金额
            BigDecimal charge_card_refund_amount = MapUtil.getBigDecimal(data1, "charge_card_refund_amount");

            // 充电卡订单数（有多少订单就买了多少张卡）
            int charge_card_order_count = MapUtil.getInt(data1, "charge_card_order_count");
            // 充电卡订单退款数
            int charge_card_refund_order_count = MapUtil.getInt(data1, "charge_card_refund_order_count");

            // 计次消费金额
            BigDecimal pay_per_charge_amount = MapUtil.getBigDecimal(data1, "pay_per_charge_amount");
            // 计次消费调整金额
            BigDecimal pay_per_adjustment_charge_amount = MapUtil.getBigDecimal(data1, "pay_per_adjustment_charge_amount");
            // 充电卡消费金额
            BigDecimal card_charge_amount = MapUtil.getBigDecimal(data1, "card_charge_amount");
            // 充电卡消费调整金额
            BigDecimal card_adjustment_charge_amount = MapUtil.getBigDecimal(data1, "card_adjustment_charge_amount");

            BigDecimal user_avg_net_income_amount = BigDecimal.ZERO;
            BigDecimal user_avg_consumption_amount = BigDecimal.ZERO;
            if (new_user_count > 0) {
                // 人均净收入金额 = (充值金额 - 充值退款金额 + 购买充电卡金额 - 充电卡退款金额) / 用户新增数
                user_avg_net_income_amount = (recharge_amount
                        .add(recharge_refund_amount)
                        .add(charge_card_amount)
                        .add(charge_card_refund_amount)
                ).divide(new BigDecimal(new_user_count), 2, RoundingMode.HALF_UP);

                // 人均消费金额 = (计次消费金额 - 计次消费调整金额 + 充电卡消费金额 - 充电卡消费调整金额) / 用户新增数
                user_avg_consumption_amount = (pay_per_charge_amount.subtract(pay_per_adjustment_charge_amount)
                        .add(card_charge_amount.subtract(card_adjustment_charge_amount)))
                        .divide(new BigDecimal(new_user_count), 2, RoundingMode.HALF_UP);
            }

            // 充电卡新增数
            int new_charge_card_count = charge_card_order_count - charge_card_refund_order_count;
            // 充电卡总数
            int total_charge_card_count = total_charge_card_order_count - total_charge_card_refund_order_count;

            // endregion

            // 用户活跃数
            long active_user_count = 0;
            // region 计算活跃用户数
            SQL = SQL.replaceAll("@SQL_OPTIONAL_WHERE_EXPRESSION", SQL_OPTIONAL_WHERE_EXPRESSION);
            List<Map<String, Object>> queryList = DataService.getDB().query(SQL);

//            LogsUtil.warn("活跃用户统计SQL", SQL);

            if (queryList != null && !queryList.isEmpty()) {
                active_user_count = MapUtil.getLong(queryList.get(0), "active_user_count", 0);
            }
            // endregion

            cb_data = new LinkedHashMap<>();
            cb_data.put("new_user_count", new_user_count); // 用户新增数
            cb_data.put("active_user_count", active_user_count); // 用户活跃数
            cb_data.put("total_user_count", total_user_count); // 用户总数

            cb_data.put("user_avg_net_income_amount", user_avg_net_income_amount); // 人均净收入金额
            cb_data.put("user_avg_consumption_amount", user_avg_consumption_amount); // 人均消费金额

            cb_data.put("new_charge_card_count", new_charge_card_count); // 充电卡新增数
            cb_data.put("total_charge_card_count", total_charge_card_count); // 充电卡总数

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
