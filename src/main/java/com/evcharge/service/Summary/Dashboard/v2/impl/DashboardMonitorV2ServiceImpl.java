package com.evcharge.service.Summary.Dashboard.v2.impl;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.entity.inspect.InspectContactInfo;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.service.Summary.Dashboard.v2.DashboardMonitorV2Service;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionQueryBuilder;
import com.evcharge.service.Summary.Dashboard.v2.helper.InspectQueryHelper;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class DashboardMonitorV2ServiceImpl implements DashboardMonitorV2Service {

    public JSONObject getMonitorList(RegionPageRequest request,String type) {
        String cacheKey = String.format("Dashboard:V2:MonitorList:%s:%s%s%s%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                ,type
                , request.getPage()
                , request.getLimit()
        );
        String cacheTotalKey = String.format("Dashboard:V2:MonitorList:%s:Total:%s%s%s%s:%s,%s",
                request.getProvinceCode()
                , request.getCityCode()
                , request.getDistrictCode()
                , request.getStreetCode()
                ,type
                , request.getPage()
                , request.getLimit()
        );
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);
        int total = DataService.getMainCache().getInt(cacheTotalKey);
        if (!cache.isEmpty()) {
//            return common.apicb(0, "success", cache);
            return common.apicbWithPageV2(total, request.getPage(), request.getLimit(), cache);

        }

        ISqlDBObject db = DataService.getDB().name("GeneralDeviceView");
        db.where("CSId", "<>", 10);
        db = new RegionQueryBuilder(request).applyTo(db);

        db.page(request.getPage(), request.getLimit());

        List<Map<String, Object>> list = db
                .where("online_status",1)
                .where("typeCode", "4GNVR")
                .where("brandCode", type)
                .select();

        if (list.isEmpty()) return common.apicb(1, "数据不存在");

        ISqlDBObject dbPageObject = DataService.getDB().name("GeneralDeviceView");

        dbPageObject = new RegionQueryBuilder(request).applyTo(dbPageObject);
        dbPageObject.where("online_status", 1);
        dbPageObject.where("CSId", "<>", 10);
        dbPageObject.where("brandCode", type);
        total = dbPageObject.count();

        for (Map<String, Object> map : list) {

            String csID = MapUtil.getString(map, "CSId");

            Map<String, Object> inspectStation = DataService.getDB("inspect").name("ChargeStation")
                    .where("CSId", csID)
                    .find();

            if (inspectStation != null) {
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


    private BigDecimal safeDivide(int numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return new BigDecimal(numerator)
                .divide(new BigDecimal(denominator), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

}
