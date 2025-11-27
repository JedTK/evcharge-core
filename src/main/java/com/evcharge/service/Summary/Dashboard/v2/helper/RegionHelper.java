package com.evcharge.service.Summary.Dashboard.v2.helper;

import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.service.Summary.Dashboard.v2.builder.RegionLonLat;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.util.Map;

public class RegionHelper {

    /**
     * 获取区域经纬度
     *
     * @param regionRequest regionRequest
     * @return RegionLonLat
     */
    public static RegionLonLat getRegionLonLat(RegionRequest regionRequest) {
        RegionLonLat regionLonLat = new RegionLonLat();

        ISqlDBObject db = DataService.getDB("evcharge_rbac");

        if (StringUtils.hasText(regionRequest.getProvinceCode())) {
            db.name("SysProvince").where("province_code", regionRequest.getProvinceCode());
        }
        if (StringUtils.hasText(regionRequest.getCityCode())) {
            db.name("SysCity").where("city_code", regionRequest.getCityCode());
        }
        if (StringUtils.hasText(regionRequest.getDistrictCode())) {
            db.name("SysArea").where("area_code", regionRequest.getDistrictCode());
        }
        if (StringUtils.hasText(regionRequest.getStreetCode())) {
            db.name("SysStreet").where("street_code", regionRequest.getStreetCode());
        }
        Map<String, Object> info = db.find();

        if (info.isEmpty()) {
            return null;
        }
        regionLonLat.setLon(MapUtil.getString(info, "lng"));
        regionLonLat.setLat(MapUtil.getString(info, "lat"));
        return regionLonLat;
    }

}
