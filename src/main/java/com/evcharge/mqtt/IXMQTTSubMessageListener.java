package com.evcharge.mqtt;

/**
 * XMQTT 消息监听器
 */
public interface IXMQTTSubMessageListener {
    void onMessage(String topic, int qos, String message);
}
