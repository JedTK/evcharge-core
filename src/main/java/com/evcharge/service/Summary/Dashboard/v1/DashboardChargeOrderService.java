package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 数据仪表板-充电订单
 */
public class DashboardChargeOrderService {

    public static DashboardChargeOrderService getInstance() {
        return new DashboardChargeOrderService();
    }

    /**
     * 充电订单列表
     *
     * @param builder 查询组装器
     * @param page    第几页
     * @param limit   每页显示
     */
    public ISyncResult getList(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        try {
            String countCacheKey = String.format("DashboardV1:ChargeOrderList:%s:TotalCount", builder.getUniqueKey());
            String listCacheKey = String.format("DashboardV1:ChargeOrderList:%s:%s_%s", builder.getUniqueKey(), limit, page);
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeOrderEntity countEntity = ChargeOrderEntity.getInstance();
            ChargeOrderEntity entity = ChargeOrderEntity.getInstance();

            countEntity.alias("co")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = co.CSId")
                    .whereIn("co.status", "1,2"); // 状态,-1=错误，0=待启动，1=充电中，2=已完成

            entity.field("OrderSN,co.CSId,cs.name,paymentTypeId,totalChargeTime,totalAmount,chargeCardConsumeAmount,startTime")
                    .alias("co")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = co.CSId")
                    .whereIn("co.status", "1,2"); // 状态,-1=错误，0=待启动，1=充电中，2=已完成

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                countEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
                entity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                countEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
                entity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                countEntity.whereIn("cs.street_code", street_code);
                entity.whereIn("cs.street_code", street_code);
            }

            // endregion

            count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.page(page, limit).select();

            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE * 10);
            DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE * 10);

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取充电订单列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 充电订单详情
     *
     * @param OrderSN 订单号
     * @return 订单详情
     */
    public ISyncResult getDetail(String OrderSN) {
        try {
            if (StringUtil.isEmpty(OrderSN)) return new SyncResult(2, "请选择充电订单");
            Map<String, Object> data = ChargeOrderEntity.getInstance()
                    .field("OrderSN,co.CSId,cs.name,paymentTypeId,totalChargeTime,totalAmount,chargeCardConsumeAmount,startTime,endTime"
                            + ",province,city,district,street,communities,roads,address")
                    .cache(String.format("DashboardV1:ChargeStation:%s:Detail", OrderSN), ECacheTime.MINUTE * 10)
                    .alias("co")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = co.CSId")
                    .where("OrderSN", OrderSN)
                    .find();
            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取充电订单详情发生错误");
        }
        return new SyncResult(1, "");
    }
}
