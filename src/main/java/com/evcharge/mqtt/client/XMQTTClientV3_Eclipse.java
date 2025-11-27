package com.evcharge.mqtt.client;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.evcharge.mqtt.*;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.ThreadUtil;
import lombok.NonNull;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMQTTClientV3_Eclipse implements IXMQTTClient {
    /**
     * 设置系统标签，用于方便查看日志
     */
    public String SysTAG = "XMQTT3";

    private static volatile XMQTTClientV3_Eclipse instance;

    public static XMQTTClientV3_Eclipse getInstance() {
        if (instance == null) {
            synchronized (XMQTTClientV3_Eclipse.class) {
                if (instance == null) {
                    instance = new XMQTTClientV3_Eclipse();
                }
            }
        }
        return instance;
    }

    /**
     * 自定义环境参数，可以通过addEnvParams插入环境参数，并且在主题中利用{参数名}来使用定义好的参数值
     */
    private Map<String, Object> EnvParams;

    /**
     * 推送客户端
     */
    protected MqttAsyncClient mPubClient;

    /**
     * 订阅客户端
     */
    protected MqttAsyncClient mSubClient;

    /**
     * 设置系统标签，用于方便查看日志
     */
    @Override
    public IXMQTTClient setSysTAG(String SysTAG) {
        this.SysTAG = SysTAG;
        return this;
    }

    /**
     * （可选）设置平台变量（可用于区分平台订阅或推送）
     *
     * @param Platform 平台代码
     */
    @Override
    public IXMQTTClient setPlatform(String Platform) {
        return addEnvParams("Platform", Platform);
    }

    /**
     * （可选）设置环境变量（可以用于区分测试服务器、本地服务器、生产服务器）
     */
    @Override
    public IXMQTTClient setEnv(String env) {
        return addEnvParams("ENV", env);
    }

    /**
     * 新增环境参数
     *
     * @param key   参数名
     * @param value 参数值
     * @return 返回
     */
    @Override
    public IXMQTTClient addEnvParams(String key, Object value) {
        if (this.EnvParams == null) this.EnvParams = new LinkedHashMap<>();
        this.EnvParams.put(key, value);
        return this;
    }

    /**
     * 推送简单连接
     *
     * @param brokerServer broker服务器地址
     * @param ClientId     客户端id，必须保存唯一的，可以自定义
     * @param UserName     用户名
     * @param Password     密码
     */
    @Override
    public void connectPub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener) {
        LogsUtil.info(SysTAG, "[推送] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);
        if (mPubClient != null && mPubClient.isConnected()) {
            LogsUtil.info(SysTAG, "[推送] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }
        if (StringUtil.isEmpty(brokerServer)) {
            LogsUtil.error(SysTAG, "[推送] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (StringUtil.isEmpty(ClientId)) {
            LogsUtil.error(SysTAG, "[推送] - 缺少客户端ID");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "缺少客户端ID");
            return;
        }
        try {
            // 创建客户端
            mPubClient = new MqttAsyncClient(brokerServer, ClientId, new MemoryPersistence());
            // MQTT的连接设置
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            connOpts.setCleanSession(true);
            // 设置连接的用户名
            connOpts.setUserName(UserName);
            // 设置连接的密码
            connOpts.setPassword(Password.toCharArray());
            // 设置超时时间 单位为秒
            connOpts.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            connOpts.setKeepAliveInterval(20);
            connOpts.setAutomaticReconnect(true);

            mPubClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Writer writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    while (cause != null) {
                        cause.printStackTrace(printWriter);
                        cause = cause.getCause();
                    }
                    printWriter.close();
                    String message = String.format("%s", writer);

                    LogsUtil.error(SysTAG, "[推送] - 已断开连接：%s", message);
                    try {
                        Thread.sleep(5000);
                        mPubClient.close();
                        //自动重新连接
                        connectPub(brokerServer, ClientId, UserName, Password, iAsyncListener);
                    } catch (InterruptedException | MqttException e) {
                        LogsUtil.error(e, SysTAG, "[推送] - 重新连接发生错误");
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String content = new String(message.getPayload());
                    LogsUtil.info(SysTAG, "[订阅消息] - 主题：[%s] Qos：%s 消息内容：%s", topic, message.getQos(), content);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            mPubClient.connect(connOpts, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    if (iMqttToken.isComplete()) {
                        LogsUtil.info(SysTAG, "[推送] - %s - 连接成功", brokerServer);
                        if (iAsyncListener != null) iAsyncListener.onResult(0, "连接成功");
                    } else {
                        LogsUtil.info(SysTAG, "[推送] - 链接失败");
                        if (iAsyncListener != null) iAsyncListener.onResult(1, "链接失败");
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    LogsUtil.info(SysTAG, "[推送] - 链接失败 - %s", throwable.getMessage());
                    if (iAsyncListener != null) iAsyncListener.onResult(4, throwable.getMessage());
                }
            });
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[推送] - 链接失败");
        }
    }

    /**
     * [推送]简单连接
     *
     * @param brokerServer broker服务器地址
     * @param ClientId     客户端id，必须保存唯一的，可以自定义
     * @param UserName     用户名
     * @param Password     密码
     */
    @Override
    public void connectSub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener) {
        LogsUtil.info(SysTAG, "[订阅] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);
        if (mSubClient != null && mSubClient.isConnected()) {
            LogsUtil.info(SysTAG, "[订阅] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }
        if (StringUtil.isEmpty(brokerServer)) {
            LogsUtil.error(SysTAG, "[订阅] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (StringUtil.isEmpty(ClientId)) {
            LogsUtil.error(SysTAG, "[订阅] - 缺少客户端ID");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "缺少客户端ID");
            return;
        }

        try {
            // 创建客户端
            mSubClient = new MqttAsyncClient(brokerServer, ClientId, new MemoryPersistence());
            // MQTT的连接设置
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            connOpts.setCleanSession(true);
            // 设置连接的用户名
            connOpts.setUserName(UserName);
            // 设置连接的密码
            connOpts.setPassword(Password.toCharArray());
            // 设置超时时间 单位为秒
            connOpts.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            connOpts.setKeepAliveInterval(20);
            connOpts.setAutomaticReconnect(true);

            mSubClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Writer writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    while (cause != null) {
                        cause.printStackTrace(printWriter);
                        cause = cause.getCause();
                    }
                    printWriter.close();
                    String message = String.format("%s", writer);

                    LogsUtil.error(SysTAG, "[订阅] - 已断开连接:%s", message);
                    try {
                        Thread.sleep(5000);
                        mSubClient.close();
                        //自动重新连接
                        connectSub(brokerServer, ClientId, UserName, Password, iAsyncListener);
                    } catch (InterruptedException | MqttException e) {
                        LogsUtil.error(e, SysTAG, "[订阅] - 重新连接发生错误");
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String content = new String(message.getPayload());
                    LogsUtil.info(SysTAG, "[订阅消息] - 主题：[%s] Qos：%s 消息内容：%s", topic, message.getQos(), content);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            mSubClient.connect(connOpts, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    if (iMqttToken.isComplete()) {
                        LogsUtil.info(SysTAG, "[订阅] - %s - 连接成功", brokerServer);
                        if (iAsyncListener != null) iAsyncListener.onResult(0, "连接成功");
                    } else {
                        LogsUtil.info(SysTAG, "[订阅] - 链接失败");
                        if (iAsyncListener != null) iAsyncListener.onResult(1, "链接失败");
                    }
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    LogsUtil.info(SysTAG, "[订阅] - 链接失败 - %s", throwable.getMessage());
                    if (iAsyncListener != null) iAsyncListener.onResult(4, throwable.getMessage());
                }
            });
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 链接失败");
        }
    }

    //region 推送消息

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult) {
        publish(topic, syncResult, 0, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos) {
        publish(topic, syncResult, qos, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained) {
        publish(topic, JSONObject.toJSONString(syncResult
                , JSONWriter.Feature.IgnoreNonFieldGetter
                , JSONWriter.Feature.IgnoreErrorGetter
                , JSONWriter.Feature.IgnoreNoneSerializable
        ), qos, retained, null);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json) {
        publish(topic, json.toJSONString(), 0, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     * @param qos   消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json, int qos) {
        publish(topic, json.toJSONString(), qos, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained) {
        publish(topic, json.toJSONString(), qos, retained, null);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     */
    @Override
    public void publish(@NonNull String topic, String content) {
        publish(topic, content, 0, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     * @param qos     消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, String content, int qos) {
        publish(topic, content, qos, false, null);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, String content, int qos, boolean retained) {
        publish(topic, content, qos, retained, null);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult, IAsyncListener iAsyncListener) {
        publish(topic, syncResult, 0, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, IAsyncListener iAsyncListener) {
        publish(topic, syncResult, qos, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained, IAsyncListener iAsyncListener) {
        publish(topic, JSONObject.toJSONString(syncResult
                , JSONWriter.Feature.IgnoreNonFieldGetter
                , JSONWriter.Feature.IgnoreErrorGetter
                , JSONWriter.Feature.IgnoreNoneSerializable
        ), qos, retained, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json, IAsyncListener iAsyncListener) {
        publish(topic, json.toJSONString(), 0, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     * @param qos   消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json, int qos, IAsyncListener iAsyncListener) {
        publish(topic, json.toJSONString(), qos, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained, IAsyncListener iAsyncListener) {
        publish(topic, json.toJSONString(), qos, retained, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     */
    @Override
    public void publish(@NonNull String topic, String content, IAsyncListener iAsyncListener) {
        publish(topic, content, 0, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     * @param qos     消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    @Override
    public void publish(@NonNull String topic, String content, int qos, IAsyncListener iAsyncListener) {
        publish(topic, content, qos, false, iAsyncListener);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    @Override
    public void publish(@NonNull String topic, String content, int qos, boolean retained, IAsyncListener iAsyncListener) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = XMQTTUtils.formatTopic(topic);//格式化主题

        LogsUtil.info(SysTAG, "[推送] - 主题：%s Qos：%s 消息：%s", topic, qos, content);

        if (StringUtil.isEmpty(topic)) {
            if (iAsyncListener != null) iAsyncListener.onResult(2, "推送主题为空");
            return;
        }
        try {
            if (mPubClient != null && !mPubClient.isConnected()) mPubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[推送] - 服务发重新连接发生错误");
        }
        try {
            if (mPubClient == null) {
                if (iAsyncListener != null) iAsyncListener.onResult(2, "推送客户端没初始化");
                return;
            }
            // 发布消息
            mPubClient.publish(topic, content.getBytes(StandardCharsets.UTF_8), qos, retained);
            if (iAsyncListener != null) iAsyncListener.onResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[推送] - 推送消息发生错误");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "推送消息发生错误");
        }
    }

    //endregion

    //region 订阅消息

    /**
     * 订阅消息
     *
     * @param topic              主题
     * @param qos                消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param subMessageListener 订阅监听
     */
    @Override
    public boolean subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = XMQTTUtils.formatTopic(topic);//格式化主题
        LogsUtil.info(SysTAG, "[订阅] - 主题：%s Qos：%s", topic, qos);
        try {
            if (mSubClient != null && !mSubClient.isConnected()) mSubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 服务发生错误");
        }

        try {
            if (mSubClient == null) return false;
            if (subMessageListener == null) mSubClient.subscribe(topic, qos);
            else
                mSubClient.subscribe(topic, qos, (mqtt_topic, mqttMessage) -> subMessageListener.onMessage(mqtt_topic, mqttMessage.getQos(), new String(mqttMessage.getPayload())));
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 服务发生错误");
        }
        return false;
    }

    @Override
    public void subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener, IAsyncListener iAsyncListener) {
        if (subscribe(topic, qos, subMessageListener)) {
            if (iAsyncListener != null) iAsyncListener.onResult(0, "");
        } else if (iAsyncListener != null) iAsyncListener.onResult(3, "订阅发生错误");
    }

    /**
     * 取消订阅消息
     *
     * @param topic 主题
     */
    @Override
    public boolean unsubscribe(@NonNull String topic) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = XMQTTUtils.formatTopic(topic);//格式化主题
        LogsUtil.info(SysTAG, "取消订阅 - 主题：%s", topic);
        try {
            if (mSubClient != null && !mSubClient.isConnected()) mSubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 取消 - 服务发生错误");
        }

        try {
            if (mSubClient == null) return false;
            mSubClient.unsubscribe(topic);
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] 取消订阅服务发生错误");
        }
        return false;
    }

    /**
     * 取消订阅消息
     *
     * @param topics 主题集合
     */
    @Override
    public boolean unsubscribe(@NonNull String[] topics) {
        String[] new_topics = new String[topics.length];
        for (int i = 0; i < topics.length; i++) {
            String topic = topics[i];
            topic = replaceTopicEnvParams(topic);//替换主题中的变量
            topic = XMQTTUtils.formatTopic(topic);//格式化主题
            new_topics[i] = topic;
            LogsUtil.info(SysTAG, "批量取消订阅 - 主题：%s", topic);
        }
        for (String topic : topics) {
            topic = replaceTopicEnvParams(topic);//替换主题中的变量
            topic = XMQTTUtils.formatTopic(topic);//格式化主题
            LogsUtil.info(SysTAG, "批量取消订阅 - 主题：%s", topic);
        }
        try {
            if (mSubClient != null && !mSubClient.isConnected()) mSubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "批量取消订阅 - 服务发生错误");
        }

        try {
            if (new_topics.length == 0) return false;
            if (mSubClient == null) return false;
            mSubClient.unsubscribe(new_topics);
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] 取消订阅服务发生错误");
        }
        return false;
    }

    //endregion

    /**
     * 注册订阅监听器
     *
     * @param target 目标监听监听器
     * @param delay  延迟执行，因为连接可能会慢，延迟注入订阅
     */
    @Override
    public void addSubListener(Class target, int delay) {
        if (delay > 0) {
            ThreadUtil.getInstance().execute("MQTT延迟订阅", () -> {
                ThreadUtil.sleep(delay);
                addSubListener(target);
            });
        }
        addSubListener(target);
    }

    /**
     * 注册订阅监听器
     *
     * @param target 目标监听监听器
     */
    @Override
    public void addSubListener(Class target) {
        if (!mSubClient.isConnected()) {
            addSubListener(target, 3000);
            return;
        }
        Method[] methods = target.getMethods();
        for (Method m : methods) {
            XMQTTSub xmqttSub = m.getAnnotation(XMQTTSub.class);
            if (xmqttSub == null) continue;
            String topic = xmqttSub.Topic();
            subscribe(topic, xmqttSub.Qos(), (topic1, qos, message) -> {
                try {
                    if (xmqttSub.openLog()) {
                        LogsUtil.info(SysTAG, "收到订阅消息 - %s - %s", topic1, message);
                    }
                    //判断是否是静态的
                    if (!Modifier.isStatic(m.getModifiers())) {
                        //获得实例化
                        Object wsMethodInstance = getWSMethodNewInstance(m);
                        //执行具体的方法
                        m.invoke(wsMethodInstance, topic1, qos, message);
                    } else {
                        m.invoke(m.getClass(), topic1, qos, message);
                    }
                } catch (Exception e) {
                    LogsUtil.error(e, this.SysTAG, "处理MQTT订阅消息过程中发生错误");
                }
            });
        }
    }

    /**
     * 注册订阅监听器
     *
     * @param targets 目标监听监听器集合
     */
    @Override
    public void addSubListener(Class[] targets) {
        for (Class c : targets) {
            addSubListener(c);
        }
    }

    /**
     * 根据方法获得一个实例
     *
     * @param method 实现方法
     */
    private Object getWSMethodNewInstance(Method method) {
        try {
            //获得实例化
            Method getInstanceMethod = method.getClass().getMethod("getInstance");
            //执行实例化方法
            return getInstanceMethod.invoke(null);
        } catch (Exception ignored) {
        }

        try {
            //执行实例化方法
            return method.getDeclaringClass().newInstance();
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 替换主题环境变量
     */
    private String replaceTopicEnvParams(String topic) {
        if (this.EnvParams == null) return topic;

        for (Map.Entry<String, Object> entry : this.EnvParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 预编译正则表达式并使用Matcher.quoteReplacement来处理特殊字符
            Pattern pattern = Pattern.compile(String.format("(?i)\\{%s\\}", Pattern.quote(key)));
            Matcher matcher = pattern.matcher(topic);
            topic = matcher.replaceAll(Matcher.quoteReplacement(value.toString()));
        }
        return topic;
    }
}
