package com.evcharge.task.monitor.check;


//import cn.com.sand.third.com.google.gson.Gson;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.ys7.YSSDK;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 萤石云设备检测器的具体实现 (纯Java类)
 */
public class EzvizDeviceChecker implements DeviceChecker {
//    private final Logger log = LoggerFactory.getLogger(EzvizDeviceChecker.class);
    private final EzvizConfig config;
    private final RestTemplate restTemplate;
//    private final WebSocketClient webSocketClient;
  //  private final Gson gson = new Gson();

    /**
     * 通过构造器接收依赖，而不是由Spring注入
     */
    public EzvizDeviceChecker(EzvizConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
//        this.webSocketClient = webSocketClient;
    }

    @Override
    public SyncResult check(Device device) {
        LogsUtil.info(this.getClass().getName(),"--- 开始检测 [萤石云] 设备: {%s} ---", device.getName());
//        String deviceSerial = device.getProperties().get("deviceSerial");
        String deviceSerial = device.getSerialNumber();
        if (deviceSerial == null || deviceSerial.isBlank()) {
            LogsUtil.error(this.getClass().getName(),"[{%s}] 参数缺失: 'deviceSerial'", device.getName());
            return new SyncResult(1,String.format("[{%s}] 参数缺失: 'deviceSerial'", device.getName()));
        }

        try {
            // 步骤1: 调用API获取AccessToken
            LogsUtil.info(this.getClass().getName(),"[{%s}] 步骤 1/2: 获取AccessToken...", device.getName());
            String accessToken = getAccessToken();
            if (accessToken == null) {
                LogsUtil.error(this.getClass().getName(),"[{%s}] 步骤 1/2 失败: 未能获取到AccessToken。", device.getName());
                return new SyncResult(1,String.format("[{%s}] 步骤 1/2 失败: 未能获取到AccessToken。", device.getName()));
            }
            LogsUtil.info(this.getClass().getName(),"[{%s}] 步骤 1/2 成功。", device.getName());

            // 步骤2: 调用 /api/lapp/device/capacity 接口检查设备状态
            LogsUtil.info(this.getClass().getName(),"[{%s}] 步骤 2/2: 调用API检查设备能力(在线状态)...", device.getName());
            SyncResult res = checkDeviceCapacity(accessToken, deviceSerial);
            if(res.code==0) {
                LogsUtil.info(this.getClass().getName(),"[{%s}] 步骤 2/2 成功: API返回code为\"200\"，设备可正常观看。", device.getName());
                return new SyncResult(0,String.format("[{%s}] 步骤 2/2 成功: API返回code为\"200\"，设备可正常观看。", device.getName()),res.data);
            } else {
                LogsUtil.error(this.getClass().getName(),"[{%s}] 步骤 2/2 失败: API返回非\"200\"状态码或请求异常。", device.getName());
                return new SyncResult(0,String.format("[{%s}] 步骤 2/2 失败: API返回非\"200\"状态码或请求异常。", device.getName()),res.data);
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(),"[{%s}] 检测过程中发生未知异常: {%s}", device.getName(), e.getMessage());
            return new SyncResult(1,String.format("[{%s}] 检测过程中发生未知异常: {%s}", device.getName(), e.getMessage()));
        }
    }

    /**
     * 调用萤石云 /api/lapp/device/capacity 接口检查设备状态
     * @param accessToken 有效的AccessToken
     * @param deviceSerial 设备序列号
     * @return 如果API返回code为"200"则为true，否则为false
     */
    private SyncResult checkDeviceCapacity(String accessToken, String deviceSerial) {
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("deviceSerial", deviceSerial);
        params.put("accessToken", accessToken);
        LogsUtil.info(this.getClass().getName(),"向萤石云发起POST请求: URL=%s, Body=%s", config.getCapacityUrl(), params);
        try {
            // 4. 发起POST请求并获取响应体
//            String jsonResponse = restTemplate.postForObject(config.getCapacityUrl(), request, String.class);
            String jsonResponse = Http2Util.post(config.getCapacityUrl(), params, "application/x-www-form-urlencoded");
            LogsUtil.info(this.getClass().getName(),"收到萤石云响应: {%s}", jsonResponse);

            // 5. 【修改】使用 fastjson2 解析JSON
            EzvizCapacityResponse response = JSON.parseObject(jsonResponse, EzvizCapacityResponse.class);

            if(response==null){
                return new SyncResult(1,String.format("设备 [%s] 状态检查失败，API返回码: null",
                        deviceSerial),null);
            }

            // 6. 判断code是否为字符串 "200"
            if ( "200".equals(response.getCode())) {
                return new SyncResult(0,"success",JSON.toJSONString(response));
            } else {
                String code = response.getCode();
                String msg = response.getMsg();
                LogsUtil.warn(this.getClass().getName(),"设备 [%s] 状态检查失败，API返回码: %s, 消息: %s",
                        deviceSerial, code, msg);
                return new SyncResult(1,msg,JSON.toJSONString(response));
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(),"请求萤石云设备能力API时出错: %s", e.getMessage());
            return new SyncResult(1,String.format("请求萤石云设备能力API时出错: %s", e.getMessage()));
        }
    }

    /**
     * 模拟调用萤石API获取AccessToken
     */
    private String getAccessToken() {
        LogsUtil.info("正在从 {%s} 获取AccessToken...", config.getTokenUrl());
        EzvizConfig ezvizConfig = new EzvizConfig();
        YSSDK yssdk = new YSSDK(ezvizConfig.getAppKey(), ezvizConfig.getSecret());
        String accessToken = yssdk.getAccessToken();
        LogsUtil.info(this.getClass().getName(),"成功获取AccessToken%s。",accessToken);
        return accessToken;
    }

    @Override
    public DeviceType supports() {
        return DeviceType.EZVIZ;
    }

    // --- 内部类，用于fastjson2解析JSON响应 ---
    @Setter
    @Getter
    private static class EzvizCapacityResponse {
        private String code;
        private String msg;
        private JSONObject data;

    }
}