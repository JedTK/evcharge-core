package com.evcharge.dto.summary;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class RegionRequest {
    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String streetCode;

    private String organizeCode;

}
