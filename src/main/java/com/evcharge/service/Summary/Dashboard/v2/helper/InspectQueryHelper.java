package com.evcharge.service.Summary.Dashboard.v2.helper;

import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.entity.inspect.InspectContactInfo;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionQueryBuilder;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;


public class InspectQueryHelper {

    private final RegionRequest request;

    public InspectQueryHelper(RegionRequest request) {
        this.request = request;
    }

    /**
     * 获取水基灭火器数量
     * @return int
     */
    public int getWaterBasedFireExtinguisher(){
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .whereIn("material_id", "12,27")
                .sum("data_value");
    }

    /**
     * 获取烟感传感器数量
     * @return int
     */
    public int getSmokeDetector(){
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .where("material_id", 14)
                .sum("data_value");
    }

    /**
     * 获取悬挂式灭火器
     * @return int
     */
    public int getHangingFireExtinguisher(){
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .where("material_id", 15)
                .sum("data_value");
    }

    /**
     * 获取消防应急工具
     * @return 0
     */
    public int getFireEmergencyTools(){
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .whereIn("material_id", "29,30")
                .sum("data_value");
    }


    /**
     * 获取人工巡检数量
     *
     * @return int
     */
    public int getManualInspect() {
        return applyRegion(DataService.getDB("inspect").name("InspectLogView"))
                .where("status", 2).count();
    }
    /**
     * 获取人工巡检数量
     *
     * @return int
     */
    public int getSystemInspect() {
        return applyRegion(DataService.getDB().name("MDStreetLogsView"))
                .where("log_type", 101).count();
    }

    /**
     * 获取巡检地图站点信息 默认获取全部
     *
     * @return List<Map < String, Object>>
     */
    public List<Map<String, Object>> getInspectStation() {
        return getInspectStation("");
    }

    /**
     * 获取某个组织的巡检地图站点信息
     *
     * @param organizeCode String
     * @return List<Map < String, Object>>
     */
    public List<Map<String, Object>> getInspectStation(String organizeCode) {
        ISqlDBObject dbObject = applyRegion(DataService.getDB("inspect").name("ChargeStationView"))
                .field("id,station_attr,uuid,CSId,name,province,province_code,city,city_code,district,district_code,street,street_code,communities,roads,address,lon,lat,total_socket,ad_panel_count,main_image,platform_code,organize_code,organize_name,status")
                .where("status", 1);

        if (StringUtils.hasLength(organizeCode)) {
            dbObject.where("organize_code", organizeCode);
        }
        return dbObject.select();
    }


    /**
     * 获取站点类型
     *
     * @param uuid 站点uuid
     * @return type int  0=正常运营 1=AI火灾监控 2=AI自动喷淋 3=AI智慧消防
     */
    public int checkStationType(String uuid) {
        int type = 0; // 0=正常运营 1=AI火灾监控 2=AI自动喷淋 3=AI智慧消防
        if (!StringUtils.hasLength(uuid)) {
            return type;
        }
        List<Map<String, Object>> list = DataService.getDB("inspect")
                .name("ChargeStationFireSafetyView")
                .field("uuid,CSId,material_id,material_name,data_value")
                .where("uuid", uuid)
                .whereIn("material_id", "5,6")
                .where("data_value", 1)
                .order("material_id asc")
                .select();

        if (list.isEmpty()) return type;
        if (list.size() == 2) {
            type = 3;
        }
        Map<String, Object> map = list.get(0);
        long materialId = MapUtil.getLong(map, "material_id");
        if (materialId == 5) {
            type = 1;
        }
        if (materialId == 6) {
            type = 2;
        }
        return type;
    }


    /**
     * 获取站点类型
     *
     * @param uuid 站点uuid
     * @return type String  0=正常运营 1=AI火灾监控 2=AI自动喷淋 3=AI智慧消防
     */
    public static String getStationTag(String uuid) {
        StringBuilder tags = new StringBuilder(); // 0=正常运营 1=AI火灾监控 2=AI自动喷淋 3=气象系统
        if (!StringUtils.hasLength(uuid)) {
            return tags.toString();
        }
        List<Map<String, Object>> list = DataService.getDB("inspect")
                .name("ChargeStationFireSafetyView")
                .field("uuid,CSId,material_id,material_name,data_value")
                .where("uuid", uuid)
                .whereIn("material_id", "5,6,32,33,34")
                .where("data_value",">=", 1)
                .order("material_id asc")
                .select();

        if (list.isEmpty()) return tags.toString();
        for (Map<String, Object> map : list) {
            long materialId =  MapUtil.getLong(map, "material_id");
            if(materialId==5){
                tags.append("fire_monitoring");
            }
            if(materialId==6){
                tags.append("automatic_sprinkler");
            }
            if(materialId==32){
                tags.append("weather_system");
                break;
            }
            if(materialId==33){
                tags.append("weather_system");
                break;
            }
            if(materialId==34){
                tags.append("weather_system");
                break;
            }
        }
        return tags.toString();
    }

    /**
     * 获取最新巡检日志的紧急联系人
     * @param uuid String
     * @return InspectContactInfo
     */
    public static InspectContactInfo getInspectEmergencyContact(String uuid) {
        InspectContactInfo inspectContactInfo=new InspectContactInfo();

        Map<String,Object> data=DataService.getDB("inspect").name("InspectLogView")
                .where("status",2)
                .where("cs_uuid",uuid)
                .order("inspect_time desc")
                .find();

        if(data.isEmpty()) return null;
        inspectContactInfo.setName(String.format("%s%s",MapUtil.getString(data, "last_name"),MapUtil.getString(data, "first_name")));
        inspectContactInfo.setPhone(MapUtil.getString(data, "phone_num"));
        inspectContactInfo.setInspectTime(MapUtil.getLong(data, "inspect_time"));
        return inspectContactInfo;
    }


    /**
     * 获取火焰监控数量
     *
     * @return int
     */
    public int getFireMonitor() {
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .where("material_id", 5)
                .where("data_value", 1)
                .count();
    }

    /**
     * 获取高清监控数量
     *
     * @return int
     */
    public int getMonitor() {
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .where("material_id", 1)
                .where("data_value", 1)
                .count();
    }

    /**
     * 获取喷淋系统数量
     *
     * @return int
     */
    public int getSprinkler() {
        return applyRegion(DataService.getDB("inspect").name("ChargeStationFireSafetyView"))
                .where("material_id", 6)
                .where("data_value", 1)
                .count();
    }

    private ISqlDBObject applyRegion(ISqlDBObject db) {
        return new RegionQueryBuilder(request).applyTo(db);
    }

}
