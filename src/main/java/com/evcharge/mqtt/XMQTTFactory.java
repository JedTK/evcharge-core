package com.evcharge.mqtt;

import com.evcharge.mqtt.client.XMQTTClientV3_Eclipse;

/**
 * XMQTT工厂模式
 */
public class XMQTTFactory {

    private static volatile IXMQTTClient instance;

    /**
     * 初始化 XMQTT 客户端
     *
     * @param instance
     * @return
     */
    public static IXMQTTClient setInstance(IXMQTTClient instance) {
        XMQTTFactory.instance = instance;
        return instance;
    }

    /**
     * 获取 XMQTT 客户端，如果没有调用初始化，则默认使用 XMQTTClient3
     *
     * @return
     */
    public static IXMQTTClient getInstance() {
        if (instance == null) {
            synchronized (IXMQTTClient.class) {
                if (instance == null) {
                    instance = XMQTTClientV3_Eclipse.getInstance();
                }
            }
        }
        return instance;
    }
}
