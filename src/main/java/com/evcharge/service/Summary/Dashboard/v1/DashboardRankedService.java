package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.station.ChargeStationDaySummaryV2Entity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据面板 - 排行榜
 */
public class DashboardRankedService {
    public static DashboardRankedService getInstance() {
        return new DashboardRankedService();
    }

    /**
     * 获取排行榜最Top的数据：充电次数最多、新增用户最多、收入金额最高、消费金额最高
     *
     * @param builder 查询组装器
     */
    public ISyncResult getAllRankFirstPlaces(@NonNull DashboardQueryBuilder builder) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        try {
            String cacheKey = String.format("DashboardV1:Ranked:TopRank:%s", builder.getUniqueKey());
            Map<String, Object> cb_data = DataService.getMainCache().getMap(cacheKey);
            if (cb_data != null && !cb_data.isEmpty()) {
                return new SyncResult(0, "", cb_data);
            }

            String cs_table = ChargeStationEntity.getInstance().theTableName();

            // 充电桩-充电次数最多
            ChargeStationDaySummaryV2Entity csUseCountTopRankEntity = ChargeStationDaySummaryV2Entity.getInstance();
            csUseCountTopRankEntity.field("css.CSId, cs.name, SUM(total_use_count) AS count_value")
                    .alias("css")
                    .join(cs_table, "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .group("css.CSId, cs.name")
                    .order("count_value DESC")
                    .limit(1);

            // 充电桩-新增用户最多
            ChargeStationDaySummaryV2Entity csNewUserTopRankEntity = ChargeStationDaySummaryV2Entity.getInstance();
            csNewUserTopRankEntity.field("css.CSId, cs.name, SUM(total_registered_users) AS count_value")
                    .alias("css")
                    .join(cs_table, "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .group("css.CSId, cs.name")
                    .order("count_value DESC")
                    .limit(1);

            // 充电桩-收入最高
            ChargeStationDaySummaryV2Entity csIncomeTopRankEntity = ChargeStationDaySummaryV2Entity.getInstance();
            csIncomeTopRankEntity.field("css.CSId, cs.name, SUM("
                            + "recharge_amount "
                            + "+ recharge_refund_amount "
                            + "+ charge_card_amount "
                            + "+ charge_card_refund_amount"
                            + ") AS count_value")
                    .alias("css")
                    .join(cs_table, "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .group("css.CSId, cs.name")
                    .order("count_value DESC")
                    .limit(1);

            // 充电桩-消费金额最高
            ChargeStationDaySummaryV2Entity csConsumptionTopRankEntity = ChargeStationDaySummaryV2Entity.getInstance();
            csConsumptionTopRankEntity.field("css.CSId, cs.name, SUM("
                            + "pay_per_charge_amount "
                            + "- pay_per_adjustment_charge_amount "
                            + "+ card_charge_amount "
                            + "+ card_adjustment_charge_amount"
                            + ") AS count_value")
                    .alias("css")
                    .join(cs_table, "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .group("css.CSId, cs.name")
                    .order("count_value DESC")
                    .limit(1);

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                csUseCountTopRankEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
                csNewUserTopRankEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
                csIncomeTopRankEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
                csConsumptionTopRankEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                csUseCountTopRankEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
                csNewUserTopRankEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
                csIncomeTopRankEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
                csConsumptionTopRankEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                csUseCountTopRankEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                csNewUserTopRankEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                csIncomeTopRankEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
                csConsumptionTopRankEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                csUseCountTopRankEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                csNewUserTopRankEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                csIncomeTopRankEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
                csConsumptionTopRankEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                csUseCountTopRankEntity.whereIn("cs.street_code", street_code);
                csNewUserTopRankEntity.whereIn("cs.street_code", street_code);
                csIncomeTopRankEntity.whereIn("cs.street_code", street_code);
                csConsumptionTopRankEntity.whereIn("cs.street_code", street_code);
            }

            // endregion

            cb_data = new LinkedHashMap<>();
            cb_data.put("cs_use_count_top_rank", csUseCountTopRankEntity.find());
            cb_data.put("cs_new_user_top_rank", csNewUserTopRankEntity.find());
            cb_data.put("cs_income_top_rank", csIncomeTopRankEntity.find());
            cb_data.put("cs_consumption_top_rank", csConsumptionTopRankEntity.find());

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
     * 排行榜 - 充电桩充电次数
     *
     * @param builder 查询组装器
     */
    public ISyncResult getChargeStationUseCountRanked(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String countCacheKey = String.format("DashboardV1:Ranked:ChargeStationUseCount:%s:TotalCount", builder.getUniqueKey());
        String listCacheKey = String.format("DashboardV1:Ranked:ChargeStationUseCount:%s:%s_%s", builder.getUniqueKey(), limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationDaySummaryV2Entity entity = ChargeStationDaySummaryV2Entity.getInstance();
            entity.field("RANK() OVER (ORDER BY SUM(total_use_count) DESC) AS ranking"
                            + ",css.CSId, cs.name, SUM(total_use_count) AS count_value")
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .page(page, limit)
                    .group("css.CSId, cs.name")
                    .order("ranking");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE * 10);
                DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE * 10);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 排行榜 - 充电桩新增用户
     *
     * @param builder 查询组装器
     */
    public ISyncResult getChargeStationNewUserRanked(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String countCacheKey = String.format("DashboardV1:Ranked:ChargeStationNewUser:%s:TotalCount", builder.getUniqueKey());
        String listCacheKey = String.format("DashboardV1:Ranked:ChargeStationNewUser:%s:%s_%s", builder.getUniqueKey(), limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationDaySummaryV2Entity entity = ChargeStationDaySummaryV2Entity.getInstance();
            entity.field("RANK() OVER (ORDER BY SUM(total_registered_users) DESC) AS ranking"
                            + ",css.CSId, cs.name, SUM(total_registered_users) AS count_value")
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .page(page, limit)
                    .group("css.CSId, cs.name")
                    .order("ranking");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE * 10);
                DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE * 10);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 排行榜 - 充电桩收入
     *
     * @param builder 查询组装器
     */
    public ISyncResult getChargeStationIncomeRanked(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String countCacheKey = String.format("DashboardV1:Ranked:ChargeStationNewUser:%s:TotalCount", builder.getUniqueKey());
        String listCacheKey = String.format("DashboardV1:Ranked:ChargeStationNewUser:%s:%s_%s", builder.getUniqueKey(), limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationDaySummaryV2Entity entity = ChargeStationDaySummaryV2Entity.getInstance();
            entity.field("RANK() OVER (ORDER BY SUM("
                            + "recharge_amount "
                            + "+ recharge_refund_amount "
                            + "+ charge_card_amount "
                            + "+ charge_card_refund_amount"
                            + ") DESC) AS ranking"

                            + ",css.CSId, cs.name, SUM("
                            + "recharge_amount "
                            + "+ recharge_refund_amount "
                            + "+ charge_card_amount "
                            + "+ charge_card_refund_amount"
                            + ") AS count_value")
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .page(page, limit)
                    .group("css.CSId, cs.name")
                    .order("ranking");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                entity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE * 10);
                DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE * 10);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 排行榜 - 充电桩消费
     *
     * @param builder 查询组装器
     */
    public ISyncResult getChargeStationConsumptionRanked(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        SyncResult checkResult = builder.check();
        if (checkResult.code != 0) return checkResult;

        String countCacheKey = String.format("DashboardV1:Ranked:ChargeStationConsumption:%s:TotalCount", builder.getUniqueKey());
        String listCacheKey = String.format("DashboardV1:Ranked:ChargeStationConsumption:%s:%s_%s", builder.getUniqueKey(), limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationDaySummaryV2Entity entity = ChargeStationDaySummaryV2Entity.getInstance();
            entity.field("RANK() OVER (ORDER BY SUM("
                            + "pay_per_charge_amount "
                            + "- pay_per_adjustment_charge_amount "
                            + "+ card_charge_amount "
                            + "+ card_adjustment_charge_amount"
                            + ") DESC) AS ranking"

                            + ",css.CSId, cs.name, SUM("
                            + "pay_per_charge_amount "
                            + "- pay_per_adjustment_charge_amount "
                            + "+ card_charge_amount "
                            + "+ card_adjustment_charge_amount"
                            + ") AS count_value")
                    .alias("css")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "css.CSId = cs.CSId")
                    .where("css.date_time", ">=", builder.start_time)
                    .where("css.date_time", "<=", builder.end_time)
                    .page(page, limit)
                    .group("css.CSId, cs.name")
                    .order("ranking");

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                entity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                entity.whereIn("cs.op_mode_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.select();

            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE * 10);
                DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE * 10);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }
}
