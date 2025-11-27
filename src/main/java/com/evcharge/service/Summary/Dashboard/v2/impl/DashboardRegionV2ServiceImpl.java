package com.evcharge.service.Summary.Dashboard.v2.impl;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.admin.AdminToRegionEntity;
import com.evcharge.entity.admin.AdminToRegionLevelEntity;
import com.evcharge.service.Summary.Dashboard.v2.DashboardRegionV2Service;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardRegionV2ServiceImpl implements DashboardRegionV2Service {

    public JSONObject getUserRegionPermission(long adminId) {
        AdminToRegionLevelEntity adminToRegionLevelEntity = AdminToRegionLevelEntity.getInstance().where("admin_id", adminId).findEntity();
        if (adminToRegionLevelEntity == null) return common.apicb(1, "暂无权限信息");
        return common.apicb(0, "success", adminToRegionLevelEntity);

    }


    private static class RegionConfig {
        String tableName;
        String parentField;
        String idAndFields;
        String adminCodeField;
    }

    private static final Map<Integer, RegionConfig> REGION_CONFIG_MAP = Map.of(
            1, createConfig("SysProvince", null,
                    "province_id,province_code as code,province_name as name,lng as lon,lat as lat",
                    "province_code"),
            2, createConfig("SysCity", "province_code",
                    "city_id,city_code as code,city_name as name,lng as lon,lat as lat",
                    "city_code"),
            3, createConfig("SysArea", "city_code",
                    "area_id,area_code as code,area_name as name,lng as lon,lat as lat",
                    "area_code"),
            4, createConfig("SysStreet", "area_code",
                    "street_id,street_code as code,street_name as name,lng as lon,lat as lat",
                    "street_code")
    );

    private static RegionConfig createConfig(String table, String parent, String fields, String adminField) {
        RegionConfig cfg = new RegionConfig();
        cfg.tableName = table;
        cfg.parentField = parent;
        cfg.idAndFields = fields;
        cfg.adminCodeField = adminField;
        return cfg;
    }

    //    private static class AdminRegionAccess {
//        Set<String> provinceCodes;
//        Set<String> cityCodes;
//        Set<String> districtCodes;
//        Set<String> streetCodes;
//    }
    private static class AdminRegionAccess {
        Set<String> provinceCodes;
        Set<String> cityCodes;
        Set<String> districtCodes;
        Set<String> streetCodes;
    }


    @Override
    public JSONObject getUserRegion(long adminId, int level, String parentCode) {
        RegionConfig cfg = REGION_CONFIG_MAP.get(level);
        if (cfg == null) {
            return common.apicb(1, "无效的 level 参数");
        }

        ISqlDBObject iSqlDBObject = DataService.getDB("evcharge_rbac").name(cfg.tableName).field(cfg.idAndFields);

        if (cfg.parentField != null) {
            iSqlDBObject.where(cfg.parentField, parentCode);
        }

        AdminToRegionEntity adminToRegionEntity = new AdminToRegionEntity();
        adminToRegionEntity.where("admin_id", adminId);
        String[] adminRegionCodes = adminToRegionEntity.selectForStringArray(cfg.adminCodeField.equals("area_code") ? "district_code" : cfg.adminCodeField);
        // 去重
        adminRegionCodes = Arrays.stream(adminRegionCodes)
                .filter(Objects::nonNull) // 可选，防止 null
                .distinct()
                .toArray(String[]::new);

        if (!Arrays.asList(adminRegionCodes).contains("ALL")) {
            String fieldName = cfg.adminCodeField.endsWith("_code") ? cfg.adminCodeField : cfg.adminCodeField + "_code";
            iSqlDBObject.whereIn(fieldName, adminRegionCodes);
        }

        List<Map<String, Object>> list = iSqlDBObject.select();
        return list.isEmpty() ? common.apicb(1, "暂无数据") : common.apicb(0, "success", list);
    }


    private AdminRegionAccess getAdminRegionAccess(long adminId) {
        ISqlDBObject db = DataService.getDB("evcharge_rbac");
        db.name("AdminToRegion").where("admin_id", adminId);

        List<Map<String, Object>> rows = db.field("province_code,city_code,district_code,street_code").select();

        AdminRegionAccess access = new AdminRegionAccess();
        access.provinceCodes = new LinkedHashSet<>();
        access.cityCodes = new LinkedHashSet<>();
        access.districtCodes = new LinkedHashSet<>();
        access.streetCodes = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            if (row.get("province_code") != null) access.provinceCodes.add(row.get("province_code").toString());
            if (row.get("city_code") != null) access.cityCodes.add(row.get("city_code").toString());
            if (row.get("district_code") != null) access.districtCodes.add(row.get("district_code").toString());
            if (row.get("street_code") != null) access.streetCodes.add(row.get("street_code").toString());
        }
        return access;
    }


    public JSONObject getUserRegionOptimized(long adminId, int level, String parentCode) {
        // 配置信息映射表
        RegionConfig cfg = REGION_CONFIG_MAP.get(level);
        if (cfg == null) return common.apicb(1, "无效的 level 参数");

        // 一次性获取管理员授权的全部区域代码
        AdminRegionAccess access = getAdminRegionAccess(adminId);

        // 构建查询
        ISqlDBObject iSqlDBObject = DataService.getDB("evcharge_rbac")
                .name(cfg.tableName)
                .field(cfg.idAndFields);

        // 添加上级区域过滤
        if (cfg.parentField != null) {
//            iSqlDBObject.where(cfg.parentField, parentCode);
            iSqlDBObject.where(cfg.parentField.equals("district_code") ? "area_code" : cfg.parentField, parentCode);
        }

        // 按 level 选择对应的授权代码集合
        Set<String> regionCodes;
        switch (level) {
            case 1:
                regionCodes = access.provinceCodes;
                break;
            case 2:
                regionCodes = access.cityCodes;
                break;
            case 3:
                regionCodes = access.districtCodes;
                break;
            case 4:
                regionCodes = access.streetCodes;
                break;
            default:
                return common.apicb(1, "无效的 level 参数");
        }

        // 如果不是 ALL，则加 whereIn 过滤
        if (!regionCodes.contains("ALL")) {
            iSqlDBObject.whereIn(cfg.adminCodeField, regionCodes.toArray(new String[0]));
        }

        // 查询结果
        List<Map<String, Object>> list = iSqlDBObject.select();
        return list.isEmpty() ? common.apicb(1, "暂无数据") : common.apicb(0, "success", list);
    }

    public JSONObject getUserRegionTree(long adminId) {

        String cacheKey = String.format("Dashboard:V2:User:%s:RegionTree", adminId);

        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);

        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }


        AdminRegionAccess access = getAdminRegionAccess(adminId);

        List<Map<String, Object>> provinces = queryProvinces(access);
        List<Map<String, Object>> cities = queryCities(access, provinces);
        List<Map<String, Object>> districts = queryDistricts(access, cities);
        List<Map<String, Object>> streets = queryStreets(access, districts);

        // 构造树
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> province : provinces) {
            province.put("child", new ArrayList<>());
            for (Map<String, Object> city : cities) {
                if (city.get("province_code").equals(province.get("code"))) {
                    city.put("child", new ArrayList<>());

                    for (Map<String, Object> district : districts) {
                        if (district.get("city_code").equals(city.get("code"))) {
                            district.put("child", new ArrayList<>());

                            for (Map<String, Object> street : streets) {
                                if (street.get("district_code").equals(district.get("code"))) {
                                    ((List<Map<String, Object>>) district.get("child")).add(street);
                                }
                            }
                            ((List<Map<String, Object>>) city.get("child")).add(district);
                        }
                    }
                    ((List<Map<String, Object>>) province.get("child")).add(city);
                }
            }
            tree.add(province);
        }
        DataService.getMainCache().setList(cacheKey, tree, ECacheTime.WEEK);
        return common.apicb(0, "success", tree);
    }

    private List<Map<String, Object>> queryProvinces(AdminRegionAccess access) {
        if (access.provinceCodes.contains("ALL")) {
            return DataService.getDB("evcharge_rbac").name("SysProvince")
                    .field("province_name AS name, province_code AS code, lng as lon, lat")
                    .select();
        }
        return DataService.getDB("evcharge_rbac").name("SysProvince")
                .field("province_name AS name, province_code AS code, lng as lon, lat")
                .whereIn("province_code", access.provinceCodes.toArray(new String[0]))
                .select();
    }

    private List<Map<String, Object>> queryCities(AdminRegionAccess access, List<Map<String, Object>> provinces) {
        Set<String> provinceCodes = provinces.stream()
                .map(p -> (String) p.get("code"))
                .collect(Collectors.toSet());

        if (access.cityCodes.contains("ALL")) {
            return DataService.getDB("evcharge_rbac").name("SysCity")
                    .field("city_name AS name, city_code AS code, lng as lon, lat, province_code")
                    .whereIn("province_code", provinceCodes.toArray(new String[0]))
                    .select();
        }
        return DataService.getDB("evcharge_rbac").name("SysCity")
                .field("city_name AS name, city_code AS code, lng as lon, lat, province_code")
                .whereIn("province_code", provinceCodes.toArray(new String[0]))
                .whereIn("city_code", access.cityCodes.toArray(new String[0]))
                .select();
    }

    private List<Map<String, Object>> queryDistricts(AdminRegionAccess access, List<Map<String, Object>> cities) {
        Set<String> cityCodes = cities.stream()
                .map(c -> (String) c.get("code"))
                .collect(Collectors.toSet());

        if (access.districtCodes.contains("ALL")) {
            return DataService.getDB("evcharge_rbac").name("SysArea")
                    .field("area_name AS name, area_code AS code, lng as lon, lat, city_code")
                    .whereIn("city_code", cityCodes.toArray(new String[0]))
                    .select();
        }
        return DataService.getDB("evcharge_rbac").name("SysArea")
                .field("area_name AS name, area_code AS code, lng as lon, lat, city_code")
                .whereIn("city_code", cityCodes.toArray(new String[0]))
                .whereIn("area_code", access.districtCodes.toArray(new String[0]))
                .select();
    }

    private List<Map<String, Object>> queryStreets(AdminRegionAccess access, List<Map<String, Object>> districts) {
        Set<String> districtCodes = districts.stream()
                .map(d -> (String) d.get("code"))
                .collect(Collectors.toSet());

        if (access.streetCodes.contains("ALL")) {
            return DataService.getDB("evcharge_rbac").name("SysStreet")
                    .field("street_name AS name, street_code AS code, lng as lon, lat, area_code as district_code")
                    .whereIn("area_code", districtCodes.toArray(new String[0]))
                    .select();
        }
        return DataService.getDB("evcharge_rbac").name("SysStreet")
                .field("street_name AS name, street_code AS code, lng as lon, lat, area_code as district_code")
                .whereIn("area_code", districtCodes.toArray(new String[0]))
                .whereIn("street_code", access.streetCodes.toArray(new String[0]))
                .select();
    }

}
