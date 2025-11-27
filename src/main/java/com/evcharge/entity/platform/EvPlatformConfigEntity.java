package com.evcharge.entity.platform;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩平台配置;
 *
 * @author : JED
 * @date : 2023-11-22
 */
public class EvPlatformConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 主机通信地址
     */
    public String host_domain;
    /**
     * 分机设备二维码重定向地址，格式：https://域名路径/{deviceCode}/{port}
     */
    public String device_qrcode_url;
    /**
     * REST API地址
     */
    public String RestAPI;
    /**
     * REST API加密key
     */
    public String RestAppSecret;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static EvPlatformConfigEntity getInstance() {
        return new EvPlatformConfigEntity();
    }

    public EvPlatformConfigEntity getWithPlatformCode(String platform_code) {
        return this.cache(String.format("BaseData:EvPlatformConfig:%s", platform_code))
                .where("platform_code", platform_code)
                .findModel();
    }
}
