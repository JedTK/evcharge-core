package com.evcharge.mqtt;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import lombok.NonNull;

/**
 * MQTT接口规范
 */
public interface IXMQTTClient {

    /**
     * 设置初始化服务，使用此设置，可以在MQTT断开时自定义重新连接，而不需要理会MQTT5本身的自动重连机制
     */
//    void setInitService(IAsyncListener iAsyncListener);

    /**
     * 设置系统标签，用于方便查看日志
     */
    IXMQTTClient setTAG(String SysTAG);

    /**
     * （可选）设置平台变量（可用于区分平台订阅或推送）
     *
     * @param Platform 平台代码
     */
    IXMQTTClient setPlatform(String Platform);

    /**
     * （可选）设置环境变量（可以用于区分测试服务器、本地服务器、生产服务器）
     */
    IXMQTTClient setEnv(String env);

    /**
     * 新增环境参数
     *
     * @param key   参数名
     * @param value 参数值
     * @return 返回
     */
    IXMQTTClient addEnvParams(String key, Object value);

    /**
     * 推送简单连接
     *
     * @param brokerServer broker服务器地址
     * @param ClientId     客户端id，必须保存唯一的，可以自定义
     * @param UserName     用户名
     * @param Password     密码
     */
    void connectPub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener);

    /**
     * [推送]简单连接
     *
     * @param brokerServer broker服务器地址
     * @param ClientId     客户端id，必须保存唯一的，可以自定义
     * @param UserName     用户名
     * @param Password     密码
     */
    void connectSub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener);

    //region 推送消息

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult);

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos);

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained);

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     */
    void publish(@NonNull String topic, @NonNull JSONObject json);

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     * @param qos   消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, @NonNull JSONObject json, int qos);

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    void publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained);

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     */
    void publish(@NonNull String topic, String content);

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     * @param qos     消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, String content, int qos);

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    void publish(@NonNull String topic, String content, int qos, boolean retained);

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     */
    void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     */
    void publish(@NonNull String topic, @NonNull JSONObject json, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     * @param qos   消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, @NonNull JSONObject json, int qos, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    void publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     */
    void publish(@NonNull String topic, String content, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     * @param qos     消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    void publish(@NonNull String topic, String content, int qos, IAsyncListener iAsyncListener);

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    void publish(@NonNull String topic, String content, int qos, boolean retained, IAsyncListener iAsyncListener);

    //endregion

    //region 订阅消息

    /**
     * 订阅消息
     *
     * @param topic              主题
     * @param qos                消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param subMessageListener 订阅监听
     */
    boolean subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener);

    /**
     * 订阅消息
     *
     * @param topic              主题
     * @param qos                消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param subMessageListener 订阅监听
     * @param iAsyncListener     订阅成功/失败回调
     */
    void subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener, IAsyncListener iAsyncListener);

    /**
     * 取消订阅消息
     *
     * @param topic 主题
     */
    boolean unsubscribe(@NonNull String topic);

    /**
     * 取消订阅消息
     *
     * @param topics 主题集合
     */
    boolean unsubscribe(@NonNull String[] topics);

    //endregion

    /**
     * 注册订阅监听器
     *
     * @param target 目标监听监听器
     * @param delay  延迟执行，因为连接可能会慢，延迟注入订阅
     */
    void addSubListener(Class target, int delay);

    /**
     * 注册订阅监听器
     *
     * @param target 目标监听监听器
     */
    void addSubListener(Class target);

    /**
     * 注册订阅监听器
     *
     * @param targets 目标监听监听器集合
     */
    void addSubListener(Class[] targets);
}

