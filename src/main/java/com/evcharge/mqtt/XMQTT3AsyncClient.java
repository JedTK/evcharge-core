package com.evcharge.mqtt;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

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

/**
 * MQTT3异步链接客户端-订阅客户端和推送客户端 分离
 */
public class XMQTT3AsyncClient {
    /**
     * 设置系统标签，用于方便查看日志
     */
    public String SysTAG = XMQTT3AsyncClient.class.getSimpleName();

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


    private static volatile XMQTT3AsyncClient instance;

    public static XMQTT3AsyncClient getInstance() {
        if (instance == null) {
            synchronized (XMQTT3AsyncClient.class) {
                if (instance == null) {
                    instance = new XMQTT3AsyncClient();
                }
            }
        }
        return instance;
    }


    /**
     * 自定义设置[推送]服务器链接
     *
     * @param listener 连接[推送]代码块
     * @return 推送客户端
     */
    public XMQTT3AsyncClient setPubClient(IMqttClientListener listener) {
        this.mPubClient = listener.get();
        return this;
    }

    /**
     * 自定义设置[订阅]服务器链接
     *
     * @param listener 连接[订阅]代码块
     * @return 订阅客户端
     */
    public XMQTT3AsyncClient setSubClient(IMqttClientListener listener) {
        this.mSubClient = listener.get();
        return this;
    }

    /**
     * 设置系统标签，用于方便查看日志
     */
    public XMQTT3AsyncClient setSysTAG(String SysTAG) {
        this.SysTAG = SysTAG;
        return this;
    }

    /**
     * （可选）设置平台变量（可用于区分平台订阅或推送）
     *
     * @param Platform 平台代码
     */
    public XMQTT3AsyncClient setPlatform(String Platform) {
        return addEnvParams("Platform", Platform);
    }

    /**
     * （可选）设置环境变量（可以用于区分测试服务器、本地服务器、生产服务器）
     */
    public XMQTT3AsyncClient setEnv(String env) {
        return addEnvParams("ENV", env);
    }

    /**
     * 新增环境参数
     *
     * @param key   参数名
     * @param value 参数值
     * @return 返回
     */
    public XMQTT3AsyncClient addEnvParams(String key, Object value) {
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
    public void easyConnectPub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener) {
        LogsUtil.info(SysTAG, "[推送] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);
        if (mPubClient != null && mPubClient.isConnected()) {
            LogsUtil.info(SysTAG, "[推送] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }
        if (!StringUtils.hasLength(brokerServer)) {
            LogsUtil.error(SysTAG, "[推送] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (!StringUtils.hasLength(ClientId)) {
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
                        easyConnectPub(brokerServer, ClientId, UserName, Password, iAsyncListener);
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
    public void easyConnectSub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener) {
        LogsUtil.info(SysTAG, "[订阅] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);
        if (mSubClient != null && mSubClient.isConnected()) {
            LogsUtil.info(SysTAG, "[订阅] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }
        if (!StringUtils.hasLength(brokerServer)) {
            LogsUtil.error(SysTAG, "[订阅] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (!StringUtils.hasLength(ClientId)) {
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
                        easyConnectSub(brokerServer, ClientId, UserName, Password, iAsyncListener);
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
    public boolean publish(@NonNull String topic, @NonNull SyncResult syncResult) {
        return publish(topic, syncResult, 0, false);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    public boolean publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos) {
        return publish(topic, syncResult, qos, false);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     */
    public boolean publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained) {
        return publish(topic, JSONObject.toJSONString(syncResult
                , JSONWriter.Feature.IgnoreNonFieldGetter
                , JSONWriter.Feature.IgnoreErrorGetter
                , JSONWriter.Feature.IgnoreNoneSerializable
        ), qos, retained);
    }

    /**
     * 推送消息
     *
     * @param topic      主题
     * @param syncResult 返回结果集合
     * @param qos        消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained   消息是否保留
     * @param openLog    是否打开日志
     */
    public boolean publish(@NonNull String topic, @NonNull SyncResult syncResult, int qos, boolean retained, boolean openLog) {
        return publish(topic, JSONObject.toJSONString(syncResult
                , JSONWriter.Feature.IgnoreNonFieldGetter
                , JSONWriter.Feature.IgnoreErrorGetter
                , JSONWriter.Feature.IgnoreNoneSerializable
        ), qos, retained, openLog);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     */
    public boolean publish(@NonNull String topic, @NonNull JSONObject json) {
        return publish(topic, json.toJSONString(), 0, false);
    }

    /**
     * 推送消息
     *
     * @param topic 主题
     * @param json  json数据
     * @param qos   消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    public boolean publish(@NonNull String topic, @NonNull JSONObject json, int qos) {
        return publish(topic, json.toJSONString(), qos, false);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    public boolean publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained) {
        return publish(topic, json.toJSONString(), qos, retained);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param json     json数据
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     * @param openLog  是否打开日志
     */
    public boolean publish(@NonNull String topic, @NonNull JSONObject json, int qos, boolean retained, boolean openLog) {
        return publish(topic, json.toJSONString(), qos, retained, openLog);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     */
    public boolean publish(@NonNull String topic, String content) {
        return publish(topic, content, 0, false);
    }

    /**
     * 推送消息
     *
     * @param topic   主题
     * @param content 消息内容
     * @param qos     消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    public boolean publish(@NonNull String topic, String content, int qos) {
        return publish(topic, content, qos, false);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     */
    public boolean publish(@NonNull String topic, String content, int qos, boolean retained) {
        return publish(topic, content, qos, retained, true);
    }

    /**
     * 推送消息
     *
     * @param topic    主题
     * @param content  消息内容
     * @param qos      消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param retained 消息是否保留
     * @param openLog  是否打开日志
     */
    public boolean publish(@NonNull String topic, String content, int qos, boolean retained, boolean openLog) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = formatTopic(topic);//格式化主题
        if (openLog) {
            LogsUtil.info(SysTAG, "[推送] - 主题：%s Qos：%s 消息：%s", topic, qos, content);
        }
        if (!StringUtils.hasLength(topic)) return false;
        try {
            if (mPubClient != null && !mPubClient.isConnected()) mPubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[推送] - 服务发生错误");
        }
        try {
            if (mPubClient == null) return false;
            // 发布消息
            mPubClient.publish(topic, content.getBytes(StandardCharsets.UTF_8), qos, retained);
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[推送] - 服务发生错误");
        }
        return false;
    }

    //endregion

    //region 订阅消息

    /**
     * 订阅消息
     *
     * @param topic                主题
     * @param qos                  消息质量：0=最多一次，1=至少一次，2=仅一次
     * @param iMqttMessageListener 订阅监听
     */
    public boolean subscribe(@NonNull String topic, int qos, IMqttMessageListener iMqttMessageListener) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = formatTopic(topic);//格式化主题
        LogsUtil.info(SysTAG, "[订阅] - 主题：%s Qos：%s", topic, qos);
        try {
            if (mSubClient != null && !mSubClient.isConnected()) mSubClient.reconnect();
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 服务发生错误");
        }

        try {
            if (mSubClient == null) return false;
            if (iMqttMessageListener == null) mSubClient.subscribe(topic, qos);
            else mSubClient.subscribe(topic, qos, iMqttMessageListener);
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, SysTAG, "[订阅] - 服务发生错误");
        }
        return false;
    }

    /**
     * 取消订阅消息
     *
     * @param topic 主题
     */
    public boolean unsubscribe(@NonNull String topic) {
        topic = replaceTopicEnvParams(topic);//替换主题中的变量
        topic = formatTopic(topic);//格式化主题
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
    public boolean unsubscribe(@NonNull String[] topics) {
        String[] new_topics = new String[topics.length];
        for (int i = 0; i < topics.length; i++) {
            String topic = topics[i];
            topic = replaceTopicEnvParams(topic);//替换主题中的变量
            topic = formatTopic(topic);//格式化主题
            new_topics[i] = topic;
            LogsUtil.info(SysTAG, "批量取消订阅 - 主题：%s", topic);
        }
        for (String topic : topics) {
            topic = replaceTopicEnvParams(topic);//替换主题中的变量
            topic = formatTopic(topic);//格式化主题
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
            subscribe(topic, xmqttSub.Qos(), (topic1, message) -> {
                try {
                    String content = new String(message.getPayload());
                    if (xmqttSub.openLog()) {
                        LogsUtil.info(SysTAG, "收到订阅消息 - %s - %s", topic1, content);
                    }
                    //判断是否是静态的
                    if (!Modifier.isStatic(m.getModifiers())) {
                        //获得实例化
                        Object wsMethodInstance = getWSMethodNewInstance(m);
                        //执行具体的方法
                        m.invoke(wsMethodInstance, topic1, message);
                    } else {
                        m.invoke(m.getClass(), topic1, message);
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
            if (getInstanceMethod != null) {
                //执行实例化方法
                Object wsMethodInstance = getInstanceMethod.invoke(null);
                return wsMethodInstance;
            }
        } catch (Exception ignored) {
        }

        try {
            //执行实例化方法
            Object wsMethodInstance = method.getDeclaringClass().newInstance();
            return wsMethodInstance;
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 转换成json数据
     */
    public static JSONObject toJson(MqttMessage message) {
        var content = new String(message.getPayload());
        if (!StringUtils.hasLength(content)) return null;
        return JSONObject.parseObject(content);
    }

    /**
     * 根据主题获取路径中最后一个值（这个值一般定义好为动态，可以是设备号或者其他）
     *
     * @param topic 订阅的主题
     */
    @Deprecated
    public static String getLastPathWithTopic(String topic) {
        String[] str = topic.split("/");
        return str[str.length - 1];
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

    /**
     * 格式化 MQTT 主题，以确保它不包含不必要的字符，同时保留 MQTT 通配符。
     *
     * @param topic 原始 MQTT 主题
     * @return 格式化后的 MQTT 主题
     */
    public static String formatTopic(String topic) {
        if (!StringUtils.hasLength(topic)) return "";
        // 替换空格和其他潜在问题字符，同时保留 MQTT 通配符 '+' 和 '#'
        // 将空格替换为下划线
        // 将.替换为下划线
        // 删除除字母、数字、下划线、斜杠以及 MQTT 通配符之外的所有字符
        return topic.trim()
                .replace(" ", "_") // 将空格替换为下划线
                .replace(".", "_") // 将.替换为下划线
                .replaceAll("[^a-zA-Z0-9_/#+]", "");
    }

    /**
     * 定义注册MQTT客户端的接口
     */
    public interface IMqttClientListener {
        /**
         * 获取一个MQTT客户端
         */
        MqttAsyncClient get();
    }
}

