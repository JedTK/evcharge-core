package com.evcharge.task.monitor.check;

import com.google.gson.Gson;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 通用WebRTC设备检测器的具体实现 (纯Java类)
 */
public class GenericWebRtcChecker implements DeviceChecker {
    private final Logger log = LoggerFactory.getLogger(GenericWebRtcChecker.class);
//    private final WebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    /**
     * 通过构造器接收依赖，而不是由Spring注入
     */
//    public GenericWebRtcChecker(WebSocketClient webSocketClient) {
//        this.webSocketClient = webSocketClient;
//    }

    @Override
    public SyncResult check(Device device) {
        LogsUtil.info(this.getClass().getName(),"--- 开始检测 [通用WebRTC] 设备: {%s} ---", device.getName());
        String signalingUrl = device.getProperties().get("signalingUrl");
        if (signalingUrl == null || signalingUrl.isBlank()) {
            LogsUtil.error(this.getClass().getName(),"[{%s}] 参数缺失: 'signalingUrl'", device.getName());
            return new SyncResult(1,String.format("[{%s}] 参数缺失: 'signalingUrl'", device.getName()));
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            // TODO: 根据您的通用WebRTC信令协议，构建正确的请求消息
            String requestMessage = gson.toJson(Map.of("event", "subscribe", "streamId", device.getId()));

            // 创建一个专用于本次连接的处理器
//            SignalingHandler handler = new SignalingHandler(device.getName(), requestMessage, future);

            // 正确的代码行：使用 doHandshake 方法
//            webSocketClient.doHandshake(handler, String.valueOf(new URI(signalingUrl)));

            // 阻塞等待结果，并设置10秒超时
//            return future.get(10, TimeUnit.SECONDS);
            return new SyncResult(1, requestMessage, future);
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(),"[{%s}] 检测失败: {%s}", device.getName(), e.getMessage());
            future.cancel(true); // 确保在异常时取消Future
            return new SyncResult(1,String.format("[{%s}] 检测失败: {%s}", device.getName(), e.getMessage()));
        }
    }

    @Override
    public DeviceType supports() {
        return DeviceType.GENERIC_WEBRTC;
    }
}
