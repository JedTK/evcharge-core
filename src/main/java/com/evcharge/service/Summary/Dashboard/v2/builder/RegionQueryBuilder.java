package com.evcharge.service.Summary.Dashboard.v2.builder;

import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.dto.summary.RegionRequest;
import com.xyzs.database.ISqlDBObject;
import org.springframework.util.StringUtils;

public class RegionQueryBuilder {

    private final String provinceCode;
    private final String cityCode;
    private final String districtCode;
    private final String streetCode;

    private final int page;
    private final int limit;
//    private final String organizeCode;

    public RegionQueryBuilder(RegionRequest request) {
        this.provinceCode = request.getProvinceCode();
        this.cityCode = request.getCityCode();
        this.districtCode = request.getDistrictCode();
        this.streetCode = request.getStreetCode();
//        this.organizeCode = request.getOrganizeCode();
        this.page = 0;
        this.limit = 0;
    }

    public RegionQueryBuilder(RegionPageRequest request) {
        this.provinceCode = request.getProvinceCode();
        this.cityCode = request.getCityCode();
        this.districtCode = request.getDistrictCode();
        this.streetCode = request.getStreetCode();
        this.page = request.getPage();
        this.limit = request.getLimit();
//        this.organizeCode = request.getOrganizeCode();
    }

    /**
     * 根据 request 构建区域维度 where 条件
     */
    public ISqlDBObject applyTo(ISqlDBObject db) {
        if (StringUtils.hasText(provinceCode)) {
            db.where("province_code", provinceCode);
        }
        if (StringUtils.hasText(cityCode)) {
            db.where("city_code", cityCode);
        }
        if (StringUtils.hasText(districtCode)) {
            db.where("district_code", districtCode);
        }
        if (StringUtils.hasText(streetCode)) {
            db.where("street_code", streetCode);
        }
//        if (StringUtils.hasText(organizeCode)) {
//            db.where("organize_code", organizeCode);
//        }
        return db;
    }

    /**
     * 根据 request 构建区域维度 where 条件
     */
    public ISqlDBObject pageApplyTo(ISqlDBObject db) {
        return applyTo(db).page(this.page, this.limit);
    }
}
