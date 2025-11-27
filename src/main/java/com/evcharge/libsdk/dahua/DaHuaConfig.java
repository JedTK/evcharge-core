package com.evcharge.libsdk.dahua;

public class DaHuaConfig {


    /**
     * 在大华开发者账号-物联网开放-产品管理获取
     * <a href="https://open.cloud-dahua.com/console/develop/iotList?t=1756967325620">...</a>
     */
    public static final String productId = "427361838";

    public static final String accessKey = "1963131598522904576";

    public static final String secretAccessKey="ARJ7bL5RZOENRmPnDiYqeKlfvpzU5VES";

    public static final String BaseUrl = "https://open.cloud-dahua.com";

    /**
     * 获取AppAccessToken
     * <a href="https://open.cloud-dahua.com/platform/develop/doccenter/doc?d=1715313431302x1724904710049">...</a>
     */
    public static final String authUrl = String.format("%s%s", BaseUrl, "/open-api/api-base/auth/getAppAccessToken");

    /**
     * 查询设备列表
     * <a href="https://open.cloud-dahua.com/platform/develop/doccenter/doc?d=1715313431302x1754380057061">...</a>
     */
    public static final String deviceListUrl = String.format("%s%s", BaseUrl, "/open-api/api-iot/device/getDeviceList");

    /**
     * 查询单SIM卡设备的SIM、IMEI信息
     * <a href="https://open.cloud-dahua.com/platform/develop/doccenter/doc?d=1715313431302x1745554275682">...</a>
     */
    public static final String deviceSimUrl = String.format("%s%s", BaseUrl, "/open-api/api-aiot/device/queryDeviceSingleSimCard");

    /**
     * 创建设备web私有拉流地址
     * 说明文档：<a href="https://open.cloud-dahua.com/platform/develop/doccenter/doc?d=1715313431302x1724930310928">说明文档</a>
     */
    public static final String deviceStreamUrl = String.format("%s%s", BaseUrl, "/open-api/api-iot/device/createDeviceStreamUrl");





}
