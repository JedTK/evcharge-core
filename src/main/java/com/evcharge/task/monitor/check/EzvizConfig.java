package com.evcharge.task.monitor.check;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 萤石云配置类
 */
@Getter
public class EzvizConfig {
    // --- Getters ---
    private final String appKey = SysGlobalConfigEntity.getString("YSS:AppKey");
    private final String secret = SysGlobalConfigEntity.getString("YSS:AppSecret");
    // 【新增】根据您提供的信息，添加新的API地址
    // 【新增】根据您提供的信息，添加新的API地址
    private final String capacityUrl = "https://open.ys7.com/api/lapp/device/capacity";
    private final String tokenUrl = "https://open.ys7.com/api/lapp/token/get";
    private final String webrtcUrl = "https://open.ys7.com/api/lapp/v2/live/webrtc/url";

}