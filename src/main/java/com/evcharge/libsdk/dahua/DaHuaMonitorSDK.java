package com.evcharge.libsdk.dahua;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DaHuaMonitorSDK {


    /**
     * 获取播放器
     * @param deviceId 设备id
     */
    public SyncResult createDeviceStreamUrl(String deviceId){
        DaHuaAuthSDK daHuaAuthSDK = new DaHuaAuthSDK();
        String url = DaHuaConfig.deviceStreamUrl;

        Map<String, Object> param = new LinkedHashMap<>();
        param.put("deviceId", deviceId);
        param.put("channelId", "0"); //设备通道号
        param.put("businessType", "real"); //服务类型。real：实时视图；talk：语音谈话；localRecord：本地视频录制；cloudRecord：云视频录制
        param.put("encryptMode", "1"); //加密模式，0：不加密；1:加密
        param.put("protoType", "rtsp"); //协议类型：rtsp或rtsv

        Map<String, Object> header = daHuaAuthSDK.createHeader(param);

        String text = Http2Util.post(url, param, header, "application/json");

        if (!StringUtils.hasLength(text)) {
            return new SyncResult(1, "大华云联】获取设备 web私有拉流地址 失败，设备ID=" + deviceId);

        }
        JSONObject jsonObject = JSONObject.parseObject(text);
        if (jsonObject.getInteger("code") != 200) {
            LogsUtil.info(this.getClass().getName(), "【大华云联】获取设备web私有拉流地址失败，失败原因：" + jsonObject.getString("msg"));
            return new SyncResult(1, "【大华云联】获取设备web私有拉流地址失败，失败原因：" + jsonObject.getString("msg"));
        }

        JSONObject data = jsonObject.getJSONObject("data");

        String playUrl = data.getString("url");

        return new SyncResult(0, "success", playUrl);


    }








}
