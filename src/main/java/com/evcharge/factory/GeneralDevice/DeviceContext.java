package com.evcharge.factory.GeneralDevice;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

/**
 * 设备上下文：承载库表信息与运行期依赖
 */
public class DeviceContext {
    public final long id;
    public final String serialNumber;
    public final String spuCode;
    public final String brandCode;
    public final String typeCode;
    public final String csId;
    public final String csUuid;
    public final String platformCode;
    public final JSONObject spec;        // GeneralDevice.spec JSON
    public final JSONObject config;      // GeneralDeviceConfig.config JSON
    public final JSONObject dynamicInfo; // GeneralDevice.dynamic_info JSON(可缓存)

    public DeviceContext(long id,
                         String serialNumber,
                         String spuCode,
                         String brandCode,
                         String typeCode,
                         String csId,
                         String csUuid,
                         String platformCode,
                         JSONObject spec,
                         JSONObject config,
                         JSONObject dynamicInfo) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.spuCode = spuCode;
        this.brandCode = brandCode;
        this.typeCode = typeCode;
        this.csId = csId;
        this.csUuid = csUuid;
        this.platformCode = platformCode;
        this.spec = spec;
        this.config = config;
        this.dynamicInfo = dynamicInfo;
    }
}