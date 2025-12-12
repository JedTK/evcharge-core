package com.evcharge.service.Summary.Dashboard.v2.impl;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.entity.inspect.InspectContactInfo;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.service.Summary.Dashboard.v2.DashboardStationV2Service;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionQueryBuilder;
import com.evcharge.service.Summary.Dashboard.v2.helper.InspectQueryHelper;
import com.evcharge.service.Summary.Dashboard.v2.helper.SummaryQueryHelper;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardStationV2ServiceImpl implements DashboardStationV2Service {


    public JSONObject getGeneralDeviceForPage(RegionPageRequest request, String keyword, String type) {
        String cacheKey = String.format("Dashboard:V2:SprayList:%s%s%s%s:%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , type
                , request.getPage()
                , request.getLimit()
        );
        String cacheTotalKey = String.format("Dashboard:V2:SprayList:Total:%s%s%s%s:%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , type
                , request.getPage()
                , request.getLimit()
        );
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        int total = DataService.getMainCache().getInt(cacheTotalKey);
        if (!cache.isEmpty()) {
            return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), cache);

        }

        ISqlDBObject db = DataService.getDB().name("GeneralDeviceView");
        db.where("CSId", "<>", 10);
        db = new RegionQueryBuilder(request).applyTo(db);

        db.page(request.getPage(), request.getLimit());

        List<Map<String, Object>> list = db
                .where("typeCode", type)
                .select();

        if (list.isEmpty()) return common.apicb(1, "数据不存在");

        ISqlDBObject dbPageObject = DataService.getDB().name("GeneralDeviceView");

        dbPageObject = new RegionQueryBuilder(request).applyTo(dbPageObject);
        dbPageObject.where("typeCode", type);
        dbPageObject.where("CSId", "<>", 10);
        total = dbPageObject.count();

        for (Map<String, Object> map : list) {

            String csID = MapUtil.getString(map, "CSId");
            map.remove("spec");
            map.remove("brandCode");
            map.remove("spuCode");
            map.remove("mainSerialNumber");
            Map<String, Object> inspectStation = DataService.getDB("inspect").name("ChargeStation")
                    .where("CSId", csID)
                    .find();

            if (inspectStation != null) {
                map.put("station_name", MapUtil.getString(inspectStation, "name"));
                map.put("province", MapUtil.getString(inspectStation, "province"));
                map.put("city", MapUtil.getString(inspectStation, "city"));
                map.put("district", MapUtil.getString(inspectStation, "district"));
                map.put("street", MapUtil.getString(inspectStation, "street"));
                map.put("communities", MapUtil.getString(inspectStation, "communities"));
                map.put("roads", MapUtil.getString(inspectStation, "roads"));
                map.put("address", MapUtil.getString(inspectStation, "address"));
                String uuid = MapUtil.getString(inspectStation, "uuid");
                //获取紧急联系人
                InspectContactInfo inspectContactInfo = InspectQueryHelper.getInspectEmergencyContact(uuid);

                if (inspectContactInfo != null) {
                    map.put("contact_person", inspectContactInfo.getName());
                    map.put("contact_phone", inspectContactInfo.getPhone());
                    map.put("last_inspect_time", inspectContactInfo.getInspectTime());
                } else {
                    map.put("contact_person", "-");
                    map.put("contact_phone", "-");
                    map.put("last_inspect_time", "");
                }
            } else {
                map.put("contact_person", "-");
                map.put("contact_phone", "-");
                map.put("last_inspect_time", "");
            }

            ChargeStationEntity chargeStation = ChargeStationEntity.getInstance().getWithCSId(csID);
            int useSocket = chargeStation.totalSocket - chargeStation.totalIdleSocket;
            BigDecimal usingRate = safeDivide(useSocket, chargeStation.totalSocket);

            map.put("use_socket", useSocket);
            map.put("use_socket_rate", usingRate);
        }


        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        DataService.getMainCache().set(cacheTotalKey, total, ECacheTime.DAY);
        return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), list);
    }


    /**
     * 获取站点统计数据
     *
     * @param request
     * @return
     */
    public JSONObject getStationSummary(RegionRequest request) {
        String cacheKey = String.format("Dashboard:V2:StationSummary:%s%s%s%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
        );
        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);

        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        SummaryQueryHelper helper = new SummaryQueryHelper(request);
        int sysRunTotal = 0;

        long onlineTime = 1667232000000L;
        sysRunTotal = (int) ((TimeUtil.getTimestamp() - onlineTime) / ECacheTime.DAY);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stationCount", helper.stationCount());
        data.put("socketCount", helper.socketCount());
        data.put("sysRunTotal", sysRunTotal);
        data.put("safeChargeCount", helper.safeChargeCount());
        DataService.getMainCache().setMap(cacheKey, data, ECacheTime.DAY);
        return common.apicb(0, "success", data);
    }

    /**
     * 获取站点汇总数据明细
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    @Override
    public JSONObject getStationSummaryDetail(RegionRequest request) {

        // 优先检查缓存
        String cacheKey = String.format("Dashboard:V2:StationSummaryDetail:%s%s%s%s",
                request.getProvinceCode(),
                request.getCityCode(),
                request.getDistrictCode(),
                request.getStreetCode());

        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        /**
         * 昨日新增站点 new_station_yesterday
         * 7天新增站点 new_station_7d
         * 30天新增站点 new_station_30d
         * --------------
         * 昨日新增充电口  new_ports_yesterday
         * 7天新增充电口 new_ports_7d
         * 30天新增充电口 new_ports_30d
         * ---------------
         * 解决应急状况 emergency_resolved
         * 解决异常状况 anomalies_resolved
         * 维护次数 maintenance_count
         * ---------------
         * 昨日充电次数 charges_yesterday
         * 7天充电次数 charges_7d
         * 30天充电次数 charges_30d
         */

        SummaryQueryHelper helper = new SummaryQueryHelper(request);

        // 组装数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("new_station", Map.of(
                "new_station_yesterday", helper.countStationBetweenDays(-7, 0), //最近新增，改为7天
                "new_station_7day", helper.countStationBetweenDays(-30, 0), //改为30天
                "new_station_30day", helper.countStationBetweenDays(-90, 0) //改为90天
        ));

        data.put("new_ports", Map.of(
                "new_ports_yesterday", helper.sumStationSocket(-7),//最近新增，改为7天
                "new_ports_7day", helper.sumStationSocket(-30), //改为30天
                "new_ports_30day", helper.sumStationSocket(-90) //改为90天
        ));

        data.put("charges", Map.of(
                "charges_yesterday", helper.countOrder(-1),
                "charges_7day", helper.countOrder(-7),
                "charges_30day", helper.countOrder(-30)
        ));

        // 后期可扩展 system_maintenance 数据来源
        data.put("system_maintenance", Map.of(
                "emergency_resolved", 2,
                "anomalies_resolved", helper.getAnomaliesResolved(),
                "maintenance_count", 0
        ));

        // 写入缓存
        DataService.getMainCache().setMap(cacheKey, data, ECacheTime.DAY);
        return common.apicb(0, "success", data);
    }

    @Override
    public JSONObject getStationUseRate(RegionRequest request) {
        /**
         * 今天使用率
         * 昨日使用率
         * 7天使用率
         * 30天使用率
         */
        // 优先检查缓存
        String cacheKey = String.format("Dashboard:V2:Station:SocketUseRate:%s%s%s%s",
                request.getProvinceCode(),
                request.getCityCode(),
                request.getDistrictCode(),
                request.getStreetCode());

        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        SummaryQueryHelper helper = new SummaryQueryHelper(request);

        // 组装数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("socket_use_rate_today", helper.getSocketUseRateBetweenDays(0, 0));
        data.put("socket_use_rate_yesterday", helper.getSocketUseRateBetweenDays(-1, -1));
        data.put("socket_use_rate_7day", helper.getSocketUseRateBetweenDays(-7, 0));
        data.put("socket_use_rate_30day", helper.getSocketUseRateBetweenDays(-30, 0));


        return common.apicb(0, "success", data);
    }

    /**
     * 获取端口使用情况
     *
     * @param request RegionRequest
     * @return
     */
    @Override
    public JSONObject getSocketUsage(RegionRequest request) {
        /**
         * 充电中 charging
         * 空闲 idle
         * 占用 occupied
         */
        String cacheKey = String.format("Dashboard:V2:Station:SocketUsage:%s%s%s%s",
                request.getProvinceCode(),
                request.getCityCode(),
                request.getDistrictCode(),
                request.getStreetCode());

        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        SummaryQueryHelper helper = new SummaryQueryHelper(request);
//        BigDecimal usingRate = new BigDecimal(useSocket)
//                .divide(new BigDecimal(chargeStation.totalSocket), 4, RoundingMode.HALF_UP)
//                .setScale(2, RoundingMode.HALF_UP); // 然后四舍五入到2位小数; // 先计算并保留4位
        Map<String, Object> data = new LinkedHashMap<>();
        BigDecimal socketCount = new BigDecimal(helper.socketCount());
        BigDecimal chargingCount = helper.getSocketUseRateBetweenDays(0, 0)
                .multiply(socketCount)
                .setScale(0, RoundingMode.HALF_UP);
        data.put("charging_count", chargingCount);
        data.put("idle_count", helper.getSocketUsage("0"));
        data.put("occupied_count", helper.getSocketUsage("2,3"));

        return common.apicb(0, "success", data);
    }


    /**
     * 获取充电数据
     *
     * @param request RegionRequest
     * @param month
     * @return
     */
    public JSONObject getChargeChartByMonth(RegionRequest request, int month) {

        String cacheKey = String.format("Dashboard:V2:Station:ChargeChart:month:%s%s%s%s,%s"
                , request.getProvinceCode(), request.getCityCode(), request.getDistrictCode(), request.getStreetCode()
                , month
        );

        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        long monthTime = TimeUtil.getMonthBegin(-month);
        ISqlDBObject db = DataService.getDB().name("ChargeOrderView");

        db = new RegionQueryBuilder(request).applyTo(db);

        List<Map<String, Object>> list = db
                .field(" DATE_FORMAT(FROM_UNIXTIME(create_time / 1000), '%Y-%m') AS month, COUNT(*) AS charge_count")
                .where("status", 2)
                .where("create_time", ">", monthTime)
                .group("month")
                .select();
        if (list.isEmpty()) return common.apicb(1, "No data available.");
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

        return common.apicb(0, "success", list);
    }

    /**
     * 获取用户注册数量
     *
     * @param request RegionRequest
     * @param month
     * @return
     */
    public JSONObject getUserCountChartByMonth(RegionRequest request, int month) {

        String cacheKey = String.format("Dashboard:V2:Station:UserCountChart:month:%s"
                , month
        );

        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        long monthTime = TimeUtil.getMonthBegin(-month);
        ISqlDBObject db = DataService.getDB().name("User");

        List<Map<String, Object>> list = db
                .field(" DATE_FORMAT(FROM_UNIXTIME(create_time / 1000), '%Y-%m') AS month, COUNT(*) AS user_count")
                .where("status", 0)
                .where("create_time", ">", monthTime)
                .group("month")
                .select();
        if (list.isEmpty()) return common.apicb(1, "No data available.");
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

        return common.apicb(0, "success", list);
    }

    /**
     * 获取用户统计 按日
     *
     * @param request RegionRequest
     * @param days    int
     * @return JSONObject
     */
    public JSONObject getUserCountChartByDay(RegionRequest request, int days) {

        String cacheKey = String.format("Dashboard:V2:Station:UserCountChart:day:%s"
                , days
        );
        long currentTime = TimeUtil.getTime00();
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        long dayTime = TimeUtil.getTime00(-days);
        System.out.println(dayTime);
        ISqlDBObject db = DataService.getDB().name("User");

        List<Map<String, Object>> list = db
                .field(" DATE_FORMAT(FROM_UNIXTIME(create_time / 1000), '%Y-%m-%d') AS day, COUNT(*) AS user_count")
                .where("status", 0)
                .where("create_time", ">", dayTime)
                .where("create_time", "<", currentTime)
                .group("day")
                .select();
        if (list.isEmpty()) return common.apicb(1, "No data available.");

        int totalUserCount = 0;
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            if (i == 0) {
                totalUserCount = MapUtil.getInt(map, "user_count");
                continue;
            }
            totalUserCount = totalUserCount + MapUtil.getInt(map, "user_count");
            map.put("user_count", totalUserCount);
        }


        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

        return common.apicb(0, "success", list);
    }

    /**
     * 按日统计充电
     *
     * @param request RegionRequest
     * @param days    int
     * @return JSONObject
     */
    public JSONObject getChargeChartByDay(RegionRequest request, int days) {
        long currentTime = TimeUtil.getTime00();
        String cacheKey = String.format("Dashboard:V2:Station:ChargeChart:day:%s%s%s%s,%s"
                , request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , days
        );

        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        long dayTime = TimeUtil.getTime00(-days);
        ISqlDBObject db = DataService.getDB().name("ChargeOrderView");

        db = new RegionQueryBuilder(request).applyTo(db);

        List<Map<String, Object>> list = db
                .field(" DATE_FORMAT(FROM_UNIXTIME(create_time / 1000), '%Y-%m-%d') AS day, COUNT(*) AS charge_count")
                .where("status", 2)
                .where("create_time", ">", dayTime)
                .where("create_time", "<", currentTime)
                .group("day")
                .select();

        if (list.isEmpty()) return common.apicb(1, "No data available.");

        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);

        int totalChargeCount = 0;
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            if (i == 0) {
                totalChargeCount = MapUtil.getInt(map, "charge_count");
                continue;
            }
            totalChargeCount = totalChargeCount + MapUtil.getInt(map, "charge_count");
            map.put("charge_count", totalChargeCount);
        }




        return common.apicb(0, "success", list);
    }

    private BigDecimal safeDivide(int numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return new BigDecimal(numerator)
                .divide(new BigDecimal(denominator), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
