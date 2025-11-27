package com.evcharge.service.Summary.Dashboard.v1;

import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.platform.EvPlatformEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.station.ChargeStationSummaryV2Entity;
import com.evcharge.entity.station.ElectricityMeterEntity;
import com.evcharge.entity.station.bill.EMeterToCStationEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据面板 - 站点信息
 */
public class DashboardChargeStationService {
    public static DashboardChargeStationService getInstance() {
        return new DashboardChargeStationService();
    }

    /**
     * 场站数据：场站总数、端口总数、电表总数、新增场站
     *
     * @param builder 查询条件
     */
    public ISyncResult getSummary(@NonNull DashboardQueryBuilder builder) {
        // 确保结束时间不能早于开始时间
        if (builder.start_time > 0 || builder.end_time > 0) {
            // 将开始时间设定为当天凌晨，结束时间设定为当天结束
            builder.start_time = TimeUtil.toDayBegin00(builder.start_time);
            builder.end_time = TimeUtil.toDayEnd24(builder.end_time);
            if (builder.end_time <= builder.start_time) return new SyncResult(2, "结束时间不能小于开始时间");

            // 将开始时间设定为当天凌晨，结束时间设定为当天结束
            builder.start_time = TimeUtil.toDayBegin00(builder.start_time);
            builder.end_time = TimeUtil.toDayEnd24(builder.end_time);
        }
        try {
            String cacheKey = String.format("DashboardV1:ChargeStation:Summary:%s", builder.getUniqueKey());
            Map<String, Object> cb_data = DataService.getMainCache().getMap(cacheKey);
            if (cb_data != null && !cb_data.isEmpty()) {
                return new SyncResult(0, "", cb_data);
            }

            // 查询总站点和总端口数
            ChargeStationEntity totalSummaryEntity = new ChargeStationEntity();
            totalSummaryEntity.field("COUNT(CSId) AS total_stations_count,SUM(totalSocket) AS total_socket")
                    .where("status", 1); // 状态：0=删除，1=运营中，2=建设中

            // 查询新增站点和新增端口
            ChargeStationEntity newSummaryEntity = new ChargeStationEntity();
            newSummaryEntity.field("COUNT(CSId) AS new_stations_count,SUM(totalSocket) AS new_socket")
                    .where("status", 1); // 状态：0=删除，1=运营中，2=建设中;

            // 查询电表数
            EMeterToCStationEntity meterSummaryEntity = new EMeterToCStationEntity();
            meterSummaryEntity.field("COUNT(DISTINCT meter_id) AS total_meter_count")
                    .alias("m")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "m.cs_id = cs.CSId")
                    .where("cs.status", 1); // 状态：0=删除，1=运营中，2=建设中;

            // region 注入可选参数
            if (builder.start_time != 0 && builder.end_time != 0) {
                totalSummaryEntity.where("online_time", "<=", builder.end_time);

                newSummaryEntity.where("online_time", ">=", builder.start_time)
                        .where("online_time", "<=", builder.end_time);

                meterSummaryEntity.where("cs.online_time", "<=", builder.end_time);
            }

            if (!StringUtil.isEmpty(builder.organize_code)) {
                totalSummaryEntity.whereIn("organize_code", builder.organize_code.split(","));
                newSummaryEntity.whereIn("organize_code", builder.organize_code.split(","));
                meterSummaryEntity.whereIn("cs.organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                totalSummaryEntity.whereIn("platform_code", builder.platform_code.split(","));
                newSummaryEntity.whereIn("platform_code", builder.platform_code.split(","));
                meterSummaryEntity.whereIn("cs.platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                totalSummaryEntity.whereIn("op_mode_code", builder.op_mode_code.split(","));
                newSummaryEntity.whereIn("op_mode_code", builder.op_mode_code.split(","));
                meterSummaryEntity.whereIn("cs.op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                totalSummaryEntity.whereIn("partner_type_code", builder.partner_type_code.split(","));
                newSummaryEntity.whereIn("partner_type_code", builder.partner_type_code.split(","));
                meterSummaryEntity.whereIn("cs.partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                totalSummaryEntity.whereIn("street_code", street_code);
                newSummaryEntity.whereIn("street_code", street_code);
                meterSummaryEntity.whereIn("cs.street_code", street_code);
            }

            // endregion

            // region 参数获取及计算
            Map<String, Object> totalSummaryData = totalSummaryEntity.find();
            int total_stations_count = MapUtil.getInt(totalSummaryData, "total_stations_count");
            int total_socket = MapUtil.getInt(totalSummaryData, "total_socket");

            Map<String, Object> newSummaryData = newSummaryEntity.find();
            int new_stations_count = MapUtil.getInt(newSummaryData, "new_stations_count");
            int new_socket = MapUtil.getInt(newSummaryData, "new_socket");

            Map<String, Object> totalMeterSummaryData = meterSummaryEntity.find();
            int total_meter_count = MapUtil.getInt(totalMeterSummaryData, "total_meter_count");

            // endregion

            cb_data = new LinkedHashMap<>();
            cb_data.put("total_stations_count", total_stations_count);
            cb_data.put("total_socket", total_socket);
            cb_data.put("total_meter_count", total_meter_count);
            cb_data.put("new_stations_count", new_stations_count);
            cb_data.put("new_socket", new_socket);

            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().setMap(cacheKey, cb_data, ECacheTime.MINUTE * 5);
            }

            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "统计数据发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 附近场站位置
     *
     * @param builder 查询条件
     * @param lon     经度坐标
     * @param lat     纬度坐标
     * @param radius  搜索半径（单位：米）
     * @param page    第几页
     * @param limit   每页限制
     */
    public ISyncResult getNearData(@NonNull DashboardQueryBuilder builder
            , double lon
            , double lat
            , double radius
            , int page
            , int limit) {

        String cacheKey = common.md5(String.format("%s_%s_%s_%s", builder.getUniqueKey(), lon, lat, radius));
        String countCacheKey = String.format("DashboardV1:MAP:%s:TotalCount", cacheKey);
        String listCacheKey = String.format("DashboardV1:MAP:%s:%s_%s", cacheKey, limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationEntity countEntity = ChargeStationEntity.getInstance();
            ChargeStationEntity entity = ChargeStationEntity.getInstance();

            countEntity.where(String.format("ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))", lon, lat), "<=", radius);

            entity.field(String.format("CSId,name,status,lon,lat,ROUND(ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))) AS distance", lon, lat))
                    .where(String.format("ST_Distance_Sphere(POINT(lon, lat), POINT(%s, %s))", lon, lat), "<=", radius);

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                countEntity.whereIn("organize_code", builder.organize_code.split(","));
                entity.whereIn("organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                countEntity.whereIn("platform_code", builder.platform_code.split(","));
                entity.whereIn("platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                countEntity.whereIn("op_mode_code", builder.op_mode_code.split(","));
                entity.whereIn("op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                countEntity.whereIn("partner_type_code", builder.partner_type_code.split(","));
                entity.whereIn("partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                countEntity.whereIn("street_code", street_code);
                entity.whereIn("street_code", street_code);
            }

            // endregion

            count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.page(page, limit).select();

            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count);
                DataService.getMainCache().setList(listCacheKey, list);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取附近站点信息发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 站点列表
     *
     * @param builder 条件查询
     * @param page    第几页
     * @param limit   每页显示
     */
    public ISyncResult getList(@NonNull DashboardQueryBuilder builder, int page, int limit) {
        String countCacheKey = String.format("DashboardV1:ChargeStationList:%s:TotalCount", builder.getUniqueKey());
        String listCacheKey = String.format("DashboardV1:ChargeStationList:%s:%s_%s", builder.getUniqueKey(), limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            ChargeStationEntity countEntity = ChargeStationEntity.getInstance();
            ChargeStationEntity entity = ChargeStationEntity.getInstance();

            entity.field("CSId,name,status,province,city,district,street,communities,roads,address")
                    .where("status", 1); // 状态：0=删除，1=运营中，2=建设中

            // region 注入可选参数
            if (!StringUtil.isEmpty(builder.organize_code)) {
                countEntity.whereIn("organize_code", builder.organize_code.split(","));
                entity.whereIn("organize_code", builder.organize_code.split(","));
            }
            if (!StringUtil.isEmpty(builder.platform_code)) {
                countEntity.whereIn("platform_code", builder.platform_code.split(","));
                entity.whereIn("platform_code", builder.platform_code.split(","));
            }

            // 运营模式代码：直投直营、代理、直管（代管理）（这个字段暂时没有设计，后面可添加上去，目前会报错）
            if (!StringUtil.isEmpty(builder.op_mode_code)) {
                countEntity.whereIn("op_mode_code", builder.op_mode_code.split(","));
                entity.whereIn("op_mode_code", builder.op_mode_code.split(","));
            }

            // 合作角色：物业、街道、商务渠道、投资人  （这个目前还没有设计，需要后继添加上去）
            if (!StringUtil.isEmpty(builder.partner_type_code)) {
                // 这里需要通过第二个表来查询到对应的CSId集合，然后再通过IN来查询对应的
                countEntity.whereIn("partner_type_code", builder.partner_type_code.split(","));
                entity.whereIn("partner_type_code", builder.partner_type_code.split(","));
            }

            // 通过对应的省市区街道代码查询对应的街道代码集合
            String[] street_code = builder.getStreetCodeList();
            if (street_code != null && street_code.length > 0) {
                countEntity.whereIn("street_code", street_code);
                entity.whereIn("street_code", street_code);
            }

            // endregion

            count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            // 查询数据库中符合条件的分页数据
            List<Map<String, Object>> list = entity.page(page, limit).select();

            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            // 将查询结果存入缓存
            if (builder.end_time < TimeUtil.getTime24()) {
                DataService.getMainCache().set(countCacheKey, count);
                DataService.getMainCache().setList(listCacheKey, list);
            }

            // 返回包含分页信息和站点列表的JSON对象
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取站点列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 站点简短详情
     *
     * @param CSId 站点ID
     */
    public ISyncResult getBriefDetail(String CSId) {
        if (StringUtil.isEmpty(CSId)) return new SyncResult(2, "请选择充电桩");

        try {
            ChargeStationEntity entity = ChargeStationEntity.getInstance();
            entity.cache(String.format("DashboardV1:ChargeStation:%s:BriefDetail", CSId), ECacheTime.MINUTE * 10)
                    .field("cs.CSId,name,status,arch,totalSocket AS total_socket"
                            + ",province,city,district,street,communities,roads,address"
                            + ",charge_time_use_rate"
                    )
                    .alias("cs")
                    .join(ChargeStationSummaryV2Entity.getInstance().theTableName(), "css", "cs.CSId = css.CSId")
                    .where("cs.CSId", CSId);
            Map<String, Object> data = entity.find();

            if (data == null || data.isEmpty()) return new SyncResult(1, "");
            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "查询站点简短详情发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 站点详细详情
     *
     * @param CSId 站点id
     */
    public ISyncResult getDetail(String CSId) {
        if (StringUtil.isEmpty(CSId)) return new SyncResult(2, "请选择充电桩");

        try {
            String cacheKey = String.format("DashboardV1:ChargeStation:%s:Detail", CSId);
            Map<String, Object> data = DataService.getMainCache().getMap(cacheKey);
            if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);

            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId);
            if (chargeStationEntity == null || chargeStationEntity.id == 0) return new SyncResult(1, "");

            data = new LinkedHashMap<>();
            data.put("CSId", chargeStationEntity.CSId);
            data.put("name", chargeStationEntity.name);
            data.put("status", chargeStationEntity.status);
            data.put("province", chargeStationEntity.province);
            data.put("city", chargeStationEntity.city);
            data.put("district", chargeStationEntity.district);
            data.put("street", chargeStationEntity.street);
            data.put("communities", chargeStationEntity.communities);
            data.put("roads", chargeStationEntity.roads);
            data.put("address", chargeStationEntity.address);
            data.put("arch", chargeStationEntity.arch);
            data.put("station_attr", chargeStationEntity.station_attr);
            data.put("station_level", chargeStationEntity.station_level);
            data.put("totalSocket", chargeStationEntity.totalSocket);
            data.put("ad_panel_count", chargeStationEntity.ad_panel_count);
            data.put("online_time", chargeStationEntity.online_time);

            // 查询平台信息
            EvPlatformEntity platformEntity = EvPlatformEntity.getInstance().getByCode(chargeStationEntity.platform_code);
            if (platformEntity != null) {
                data.put("platform", new LinkedHashMap<>() {{
                    put("code", platformEntity.code);
                    put("name", platformEntity.name);
                }});
            }

            // 查询电表信息
            ElectricityMeterEntity meterEntity = ElectricityMeterEntity.getInstance().getByCSId(chargeStationEntity.CSId);
            if (meterEntity != null) {
                data.put("meter", new LinkedHashMap<>() {{
                    put("title", meterEntity.title);
                    put("meterNo", meterEntity.meterNo);
                    put("meter_code", meterEntity.meter_code);
                    put("province", meterEntity.province);
                    put("city", meterEntity.city);
                    put("district", meterEntity.district);
                    put("street", meterEntity.street);
                    put("address", meterEntity.address);
                    put("meter_attr", meterEntity.meter_attr);
                    put("meter_type", meterEntity.meter_type);
                    put("maxPower", meterEntity.maxPower);
                }});
            }

            // 查询收费标准
            List<Map<String, Object>> chargeStandardItemList = ChargeStandardItemEntity.getInstance().getListByCSId(chargeStationEntity.CSId, true);
            if (chargeStandardItemList != null && !chargeStandardItemList.isEmpty()) {
                data.put("charge_standard", chargeStandardItemList);
            }

            DataService.getMainCache().setMap(cacheKey, data, ECacheTime.MINUTE * 10);

            return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "查询站点详细详情发生错误");
        }
        return new SyncResult(1, "");
    }
}
