package com.evcharge.dto.summary;

import lombok.Data;

@Data
public class RegionPageRequest {
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String streetCode;
    private String organizeCode;

    // 分页字段
    private int page = 1;  // 第几页
    private int limit = 10; // 每页多少条
}
