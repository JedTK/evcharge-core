package com.evcharge.service.Summary.Dashboard.v2.impl;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.entity.inspect.InspectContactInfo;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.service.Summary.Dashboard.v2.DashboardInspectV2Service;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionQueryBuilder;
import com.evcharge.service.Summary.Dashboard.v2.helper.InspectQueryHelper;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class DashboardInspectV2ServiceImpl implements DashboardInspectV2Service {

    /**
     * 获取消防耗材
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    public JSONObject getFireSupplies(RegionRequest request) {

        String cacheKey = String.format("Dashboard:V2:Inspect:FireSupplies:%s%s%s%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
        );

        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        InspectQueryHelper inspectQueryHelper = new InspectQueryHelper(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("water_fire_extinguisher", inspectQueryHelper.getWaterBasedFireExtinguisher());
        data.put("smoke_detector", inspectQueryHelper.getSmokeDetector());
        data.put("handing_fire_extinguisher", inspectQueryHelper.getHangingFireExtinguisher());
        data.put("fire_emergency_tools", inspectQueryHelper.getFireEmergencyTools());

        DataService.getMainCache().setMap(cacheKey, data, ECacheTime.DAY);

        return common.apicb(0, "success", data);
    }

    /**
     * 获取组织列表
     *
     * @return JSONObject
     */
    public JSONObject getOrganizeList() {

        String cacheKey = "Dashboard:V2:Inspect:OrganizeList";
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        List<Map<String, Object>> list = DataService.getDB("inspect_rbac").name("RBOrganize")
                .field("id,name,code,remark as icon")
                .where("type_id", 2)
                .select();

        if (list.isEmpty()) return common.apicb(1, "暂无组织信息");
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        return common.apicb(0, "success", list);

    }

    public JSONObject getInspectSummary(RegionRequest request) {
        String cacheKey = String.format("Dashboard:V2:InspectSummary:%s%s%s%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
        );

        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        InspectQueryHelper inspectQueryHelper = new InspectQueryHelper(request);
        int systemInspect = 0;
        int weatherSystem = 0;

        Map<String, Object> data = new HashMap<>();
        data.put("manual_inspect_count", inspectQueryHelper.getManualInspect());
        data.put("system_inspect_count", inspectQueryHelper.getSystemInspect());
        data.put("fire_monitor_count", inspectQueryHelper.getFireMonitor());
        data.put("monitor_count", inspectQueryHelper.getMonitor());
        data.put("sprinklers", inspectQueryHelper.getSprinkler());
        data.put("weather_system", weatherSystem);
        DataService.getMainCache().setMap(cacheKey, data, ECacheTime.DAY);
        return common.apicb(0, "success", data);
    }

    /**
     * 获取巡检的站点列表，地图需要用到
     *
     * @param request JSONObject
     * @return JSONObject
     */

    public JSONObject getInspectStationForMap(RegionRequest request) {
        String cacheKey = String.format("Dashboard:V2:InspectStation:%s%s%s%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
        );
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        InspectQueryHelper inspectQueryHelper = new InspectQueryHelper(request);
        List<Map<String, Object>> list = inspectQueryHelper.getInspectStation(request.getOrganizeCode());
        if (list.isEmpty()) return common.apicb(1, "数据不存在");

        for (Map<String, Object> map : list) {
            String uuid = MapUtil.getString(map, "uuid");
            int type = 0; // 0=正常运营 1=AI火灾监控 2=AI自动喷淋 3=AI智慧消防

            type = inspectQueryHelper.checkStationType(uuid);
            map.put("type", type);
        }

        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        return common.apicb(0, "success", list);
    }

    /**
     * 获取巡检站点列表 分页
     * 2025-08-18 添加类型选择
     * @param request RegionPageRequest
     * @param keyword 关键词
     * @param types   类型 fireMonitoring autoSprinkler weatherSystem
     * @return JSONObject
     */
    public JSONObject getInspectStationForPage(RegionPageRequest request, String keyword, String types) {
        String cacheKey = String.format("Dashboard:V2:InspectStation:%s%s%s%s%s,%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , request.getOrganizeCode() == null ? "" : request.getOrganizeCode()
                , keyword
                , request.getPage()
                , request.getLimit()
        );
        String cacheTotalKey = String.format("Dashboard:V2:InspectStationTotal:%s%s%s%s%s,%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , request.getOrganizeCode() == null ? "" : request.getOrganizeCode()
                , keyword
                , request.getPage()
                , request.getLimit()
        );
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        int total = DataService.getMainCache().getInt(cacheTotalKey);
//        if (!cache.isEmpty()) {
//            return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), cache);
//        }

//        List<Object> paramCSIdsArray = new ArrayList<>();

//        switch (types) {
//            case "fireMonitoring":
//                paramCSIdsArray = DataService.getDB("inspect")
//                        .name("ChargeStationFireSafetyView")
//                        .where("material_id", 5)
//                        .where("data_value", 1)
//                        .where("CSId","<>","")
//                        .where("CSId","<>","0")
//                        .group("CSId")
//                        .selectForArray("CSId");
//                break;
//
//            case "autoSprinkler":
//
//                paramCSIdsArray = DataService.getDB("inspect")
//                        .name("ChargeStationFireSafetyView")
//                        .where("material_id", 6)
//                        .where("data_value", 1)
//
//                        .where("CSId","<>","")
//                        .where("CSId","<>","0")
//                        .group("CSId")
//                        .selectForArray("CSId");
//
//                break;
//            case "weatherSystem":
//
//                paramCSIdsArray = DataService.getDB("inspect")
//                        .name("ChargeStationFireSafetyView")
//                        .whereIn("material_id", "32,33,34")
//                        .where("data_value", 1)
//                        .where("CSId","<>","")
//                        .where("CSId","<>","0")
//                        .group("CSId")
//                        .selectForArray("CSId");
//
//                break;
//            default:break;
//        }
//        if(!StringUtils.hasLength(types) && paramCSIdsArray.isEmpty()){
//            return common.apicb(1, "暂无数据");
//        }

        String field = "id,station_attr,uuid,CSId,name,province,province_code,city,city_code,district,district_code,street,street_code,communities,roads,address,lon,lat,total_socket,ad_panel_count,main_image,platform_code,organize_code,organize_name,status";

        ISqlDBObject dbObject = DataService.getDB("inspect").name("ChargeStationView");
        dbObject.field(field);
        dbObject.where("status", 1);
        dbObject = new RegionQueryBuilder(request).applyTo(dbObject);

        if (StringUtils.hasLength(request.getOrganizeCode())) {
            dbObject.where("organize_code", request.getOrganizeCode());
        }

        if (StringUtils.hasLength(keyword)) {
            dbObject.whereOr("(", "name", "=", keyword, "");
            dbObject.whereOr("province", keyword);
            dbObject.whereOr("city", keyword);
            dbObject.whereOr("district", keyword);
            dbObject.whereOr("", "street", "=", keyword, ")");
        }
//        if(!paramCSIdsArray.isEmpty()) {
//            String[] paramCSIds = paramCSIdsArray.toString().split(",");
//            dbObject.whereIn("CSId", paramCSIds);
//        }
        dbObject.page(request.getPage(), request.getLimit());

        List<Map<String, Object>> list = dbObject.select();

        //总页码
        ISqlDBObject dbPageObject = DataService.getDB("inspect").name("ChargeStationView");
        dbPageObject.where("status", 1);
        dbPageObject = new RegionQueryBuilder(request).applyTo(dbPageObject);

        if (StringUtils.hasLength(request.getOrganizeCode())) {
            dbPageObject.where("organize_code", request.getOrganizeCode());
        }

        if (StringUtils.hasLength(keyword)) {
            dbPageObject.whereOr("(", "name","=", keyword,"");
            dbPageObject.whereOr("province", keyword);
            dbPageObject.whereOr("city", keyword);
            dbPageObject.whereOr("district", keyword);
            dbPageObject.whereOr("","street","=", keyword, ")");
        }
//        if(!paramCSIdsArray.isEmpty()) {
//            String[] paramCSIds = paramCSIdsArray.toString().split(",");
//            dbObject.whereIn("CSId", paramCSIds);
//        }
        total = dbPageObject.count();


        if (list.isEmpty()) return common.apicb(1, "数据不存在");

        for (Map<String, Object> map : list) {
            String uuid = MapUtil.getString(map, "uuid");
            String csID = MapUtil.getString(map, "CSId");
            String tag = InspectQueryHelper.getStationTag(uuid);
            map.put("tags", tag);
            //获取紧急联系人
            InspectContactInfo inspectContactInfo = InspectQueryHelper.getInspectEmergencyContact(uuid);

            if (inspectContactInfo != null) {
                map.put("contact_person", inspectContactInfo.getName());
                map.put("contact_phone", inspectContactInfo.getPhone());
            } else {
                map.put("contact_person", "");
                map.put("contact_phone", "");
            }
            //使用率
            if (StringUtils.hasLength(csID)) {
                ChargeStationEntity chargeStation = ChargeStationEntity.getInstance().getWithCSId(csID);
                //total_socket
                //using_socket
                int useSocket = chargeStation.totalSocket - chargeStation.totalIdleSocket;

                BigDecimal usingRate = new BigDecimal(useSocket)
                        .divide(new BigDecimal(chargeStation.totalSocket), 4, RoundingMode.HALF_UP)
                        .setScale(2, RoundingMode.HALF_UP); // 然后四舍五入到2位小数; // 先计算并保留4位

                map.put("use_socket", useSocket);
                map.put("use_socket_rate", usingRate);
            } else {
                map.put("use_socket", 0);
                map.put("use_socket_rate", 0);
            }

        }

        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        DataService.getMainCache().set(cacheTotalKey, total, ECacheTime.DAY);
        return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), list);

    }

    /**
     * 获取站点信息
     *
     * @param uuid
     * @return
     */
    public JSONObject getInspectStationInfo(String uuid) {
        if (!StringUtils.hasLength(uuid)) return common.apicb(1, "uuid不能为空");

        String cacheKey = String.format("Dashboard:V2:InspectStationInfo:%s",
                uuid
        );
        Map<String, Object> cache = DataService.getMainCache().getMap(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        Map<String, Object> map = DataService.getDB("inspect").name("ChargeStationView")
                .where("uuid", uuid)
                .where("status", 1)
                .find();

        if (map == null) return common.apicb(1, "站点不存在");
        String csID = MapUtil.getString(map, "CSId");
        String tag = InspectQueryHelper.getStationTag(uuid);
        map.put("tags", tag);
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
        //使用率
        if (StringUtils.hasLength(csID)) {
            ChargeStationEntity chargeStation = ChargeStationEntity.getInstance().getWithCSId(csID);
            int useSocket = chargeStation.totalSocket - chargeStation.totalIdleSocket;
            BigDecimal usingRate = new BigDecimal(useSocket)
                    .divide(new BigDecimal(chargeStation.totalSocket), 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP); // 然后四舍五入到2位小数; // 先计算并保留4位

            map.put("use_socket", useSocket);
            map.put("use_socket_rate", usingRate);
        } else {
            map.put("use_socket", 0);
            map.put("use_socket_rate", 0);
        }

        DataService.getMainCache().setMap(cacheKey, map, ECacheTime.DAY);
        return common.apicb(0, "success", map);
    }

    /**
     * 获取巡检日志列表
     *
     * @param request RegionPageRequest
     * @return JSONObject
     */
    public JSONObject getInspectLogList(RegionPageRequest request) {
        String cacheKey = String.format("Dashboard:V2:InspectLogList:%s%s%s%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , request.getPage()
                , request.getLimit()
        );

        String cacheTotalKey = String.format("Dashboard:V2:InspectLogListTotal:%s%s%s%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                , request.getPage()
                , request.getLimit()
        );


        int total = DataService.getMainCache().getInt(cacheTotalKey);


        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), cache);
        }

        ISqlDBObject db = DataService.getDB("inspect")
                .name("InspectLogView");
        db.where("status", 2);
        db = new RegionQueryBuilder(request).applyTo(db);

        db.page(request.getPage(), request.getLimit());
        db.order("id desc");
        List<Map<String, Object>> list = db.select();

        ISqlDBObject dbPageObject = DataService.getDB("inspect")
                .name("InspectLogView");
        dbPageObject.where("status", 2);
        dbPageObject = new RegionQueryBuilder(request).applyTo(dbPageObject);

        total = dbPageObject.count();

        if (list.isEmpty()) return common.apicb(1, "数据不存在");
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        DataService.getMainCache().set(cacheTotalKey, total, ECacheTime.DAY);
        return common.apicb(0, "success", list);
    }

    /**
     * 获取系统巡检日志
     *
     * @param request RegionPageRequest
     * @return
     */
    public JSONObject getSystemInspectLogList(RegionPageRequest request) {
        String cacheKey = String.format("Dashboard:V2:SystemInspectLogList:%s,%s"
                , request.getPage()
                , request.getLimit()
        );

        String cacheTotalKey = String.format("Dashboard:V2:SystemInspectLogListTotal:%s,%s"
                , request.getPage()
                , request.getLimit()
        );


        int total = DataService.getMainCache().getInt(cacheTotalKey);


        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        if (!cache.isEmpty()) {
            return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), cache);
        }


        ISqlDBObject db = DataService.getDB()
                .name("MDStreetLogsView");
        db.where("log_type", 101);
        db.page(request.getPage(), request.getLimit());
        db.order("id desc");
        List<Map<String, Object>> list = db.select();

        ISqlDBObject dbPageObject = DataService.getDB()
                .name("MDStreetLogsView");

        db.where("log_type", 101);
        total = dbPageObject.count();

        if (list.isEmpty()) return common.apicb(1, "数据不存在");
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        DataService.getMainCache().set(cacheTotalKey, total, ECacheTime.DAY);
        return common.apicb(0, "success", list);


    }
}
