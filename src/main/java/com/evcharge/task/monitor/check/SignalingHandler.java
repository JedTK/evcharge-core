package com.evcharge.task.monitor.check;


import com.xyzs.utils.LogsUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 通用的WebSocket信令处理器。
 * 这是一个非Spring Bean的普通Java类，每次检测都会创建一个新实例，以避免状态混淆。
 * 继承Spring的TextWebSocketHandler来简化文本消息处理。
 */
//public class SignalingHandler extends TextWebSocketHandler {
public class SignalingHandler {
//    private final Logger log = LoggerFactory.getLogger(SignalingHandler.class);
//
//    private final String deviceName;
//    private final String messageToSend;
//    private final CompletableFuture<Boolean> resultFuture;
//
//    /**
//     * 构造函数
//     * @param deviceName    设备名称，用于日志输出
//     * @param messageToSend 连接成功后要发送的信令消息
//     * @param resultFuture  用于异步返回检测结果的CompletableFuture
//     */
//    public SignalingHandler(String deviceName, String messageToSend, CompletableFuture<Boolean> resultFuture) {
//        this.deviceName = deviceName;
//        this.messageToSend = messageToSend;
//        this.resultFuture = resultFuture;
//    }
//
//    /**
//     * 当WebSocket连接成功建立后，此方法被Spring框架自动调用。
//     */
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
//        LogsUtil.info(this.getClass().getName(),"[{}] 信令连接成功，准备发送请求...", deviceName);
//        session.sendMessage(new TextMessage(messageToSend));
//         LogsUtil.info(this.getClass().getName(),"[{}] 已发送信令消息: {}", deviceName, messageToSend);
//    }
//
//    /**
//     * 当收到文本消息时，此方法被Spring框架自动调用。
//     */
//    @Override
//    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws IOException {
//        String payload = message.getPayload();
//         LogsUtil.info(this.getClass().getName(),"[{%s}] 收到信令响应: {}", deviceName, payload);
//
//        // TODO: 根据您的具体协议，定义更精确的成功判断逻辑。
//        // 例如，检查响应中是否包含 "code: 200" 或 "sdp" 字段。
//        // 为了简洁，我们假设收到任何非空消息即为成功。
//        if (!payload.isEmpty()) {
//             LogsUtil.info(this.getClass().getName(),"[{%s}] 响应有效，判定为检测成功。", deviceName);
//            resultFuture.complete(true);
//            session.close(CloseStatus.NORMAL); // 任务完成，主动关闭连接
//        }
//    }
//
//    /**
//     * 当连接出现传输错误时，此方法被Spring框架自动调用。
//     */
//    @Override
//    public void handleTransportError(@NotNull WebSocketSession session, Throwable exception) {
//         LogsUtil.error(this.getClass().getName(),"[{%s}] 信令连接发生传输错误: {}", deviceName, exception.getMessage());
//        resultFuture.complete(false); // 将结果标记为失败
//    }
//
//    /**
//     * 当WebSocket连接关闭后，此方法被Spring框架自动调用。
//     */
//    @Override
//    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
//        LogsUtil.error(this.getClass().getName(),"[{%s}] 信令连接关闭. Status: {}", deviceName, status);
//        // 如果Future还未被正常完成（即没有收到成功消息），说明连接是意外关闭的，视为失败。
//        resultFuture.completeExceptionally(new IOException("Connection closed with status: " + status));
//    }
}