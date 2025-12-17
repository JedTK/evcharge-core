package com.evcharge.mqtt.client;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.mqtt.*;
import com.evcharge.service.notify.NotifyService;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XMQTTClientV5 implements IXMQTTClient {

    /**
     * 设置系统标签，用于方便查看日志
     */
    public String TAG = "XMQTT5";

    private static volatile XMQTTClientV5 instance;

    public static XMQTTClientV5 getInstance() {
        if (instance == null) {
            synchronized (XMQTTClientV5.class) {
                if (instance == null) {
                    instance = new XMQTTClientV5();
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
    protected Mqtt5AsyncClient mPubClient;

    /**
     * 订阅客户端
     */
    protected Mqtt5AsyncClient mSubClient;

    /**
     * 自动重连策略配置：初始重连间隔
     */
    private long initialDelayMs = 2000;
    /**
     * 自动重连策略配置：最大重连间隔
     */
    private long maxDelayMs = 60000;

    /**
     * 设置系统标签，用于方便查看日志
     */
    @Override
    public IXMQTTClient setTAG(String SysTAG) {
        this.TAG = SysTAG;
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
     * 建立 MQTT 推送客户端的连接（异步方式）
     *
     * @param brokerServer   Broker服务器地址，例如：tcp://120.78.11.119:1884
     * @param ClientId       客户端唯一标识，必须唯一，可自定义
     * @param UserName       MQTT 账号用户名
     * @param Password       MQTT 账号密码
     * @param iAsyncListener 回调监听器，用于通知连接结果（成功/失败）
     */
    @Override
    public void connectPub(String brokerServer, String ClientId, String UserName, String Password, IAsyncListener iAsyncListener) {
        LogsUtil.info(TAG, "[推送] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);

        // 已连接则直接返回
        if (mPubClient != null && mPubClient.getState().isConnected()) {
            LogsUtil.info(TAG, "[推送] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }

        // 参数校验
        if (StringUtil.isEmpty(brokerServer)) {
            LogsUtil.error(TAG, "[推送] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (StringUtil.isEmpty(ClientId)) {
            LogsUtil.error(TAG, "[推送] - 缺少客户端ID");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "缺少客户端ID");
            return;
        }

        try {
            // 解析 broker 地址中的 host 和 port
            URI uri = new URI(brokerServer);
            String host = uri.getHost();   // 获取主机地址
            int port = uri.getPort();      // 获取端口号
            byte[] pwd = Password == null ? new byte[0] : Password.getBytes(StandardCharsets.UTF_8);

            // 创建异步 MQTT 5 客户端
            mPubClient = MqttClient.builder().useMqttVersion5()                 // 使用 MQTT 5 协议
                    .identifier(ClientId)              // 客户端唯一标识（ClientId）
                    .serverHost(host)                  // 设置服务器地址
                    .serverPort(port)                  // 设置服务器端口

                    .simpleAuth()                      // 开启简单用户名/密码认证
                    .username(UserName)                // 设置用户名
                    .password(pwd)                     // 设置密码
                    .applySimpleAuth()                 // 应用认证配置

                    // 自动重连策略配置：指数退避（失败一次延迟翻倍）初始重连间隔1s，直到最大1m重连间隔
                    .automaticReconnect()
                    .initialDelay(this.initialDelayMs, TimeUnit.MILLISECONDS)   // 初始重连间隔：10秒
                    .maxDelay(this.maxDelayMs, TimeUnit.MILLISECONDS)           // 最大重连间隔：2分钟
                    .applyAutomaticReconnect()

                    // 网络传输参数配置
                    .transportConfig()
                    .mqttConnectTimeout(10, TimeUnit.SECONDS)  // MQTT CONNECT 报文的超时时间
                    .socketConnectTimeout(5, TimeUnit.SECONDS) // TCP 连接超时时间
                    .applyTransportConfig()            // 应用网络传输配置

                    // 连接成功监听器
                    .addConnectedListener(ctx -> {
                        LogsUtil.info(TAG, "[推送] - ✅ 已连接，心跳 20s，监听自动重连");
                        ExecutionThrottle.getInstance().run(data -> {
                            NotifyService.getInstance().asyncPush(ClientId
                                    , "NOTIFY.TEXT"
                                    , ENotifyType.WECHATCORPBOT
                                    , new JSONObject() {{
                                        put("title", "MQTT连接成功");
                                        put("content", String.format("ClientId=%s,UserName=%s", ClientId, UserName));
                                    }}
                            );
                            return new SyncResult();
                        }, String.format("MQTT.Connected:%s", ClientId), ECacheTime.MINUTE);

                    })

                    // 断开连接监听器
                    .addDisconnectedListener(ctx -> {
                        Throwable cause = ctx.getCause();
                        LogsUtil.error(TAG, "[推送] - ⚠️ 已断开 - %s - %s", ctx.getSource(), cause.getMessage());

                        ExecutionThrottle.getInstance().run(data -> {
                            NotifyService.getInstance().asyncPush(ClientId
                                    , "NOTIFY.TEXT"
                                    , ENotifyType.WECHATCORPBOT
                                    , new JSONObject() {{
                                        put("title", "MQTT已断开，尝试重连中...");
                                        put("content", String.format("ClientId=%s,UserName=%s", ClientId, UserName));
                                    }}
                            );
                            return new SyncResult();
                        }, String.format("MQTT.Disconnected:%s", ClientId), ECacheTime.MINUTE);
                    })

                    .buildAsync();                     // 构建异步客户端

            // 启动连接流程
            mPubClient.connectWith()
                    .cleanStart(true)       // 是否清除Session：true 表示不保留历史订阅和会话记录
                    .keepAlive(30)          // 设置心跳间隔时间（秒）：客户端多久发一次 PING 请求，建议20~60秒
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        // 异步连接完成后回调
                        if (throwable != null) {
                            // 这里可以做手动重连
                            LogsUtil.error(TAG, "[推送] - ⚠️ 连接失败：%s", throwable.getMessage());
                            if (iAsyncListener != null) {
                                iAsyncListener.onResult(1, "连接失败：" + throwable.getMessage());
                            }
                        } else {
                            LogsUtil.info(TAG, "[推送] - ✅ 连接成功");
                            if (iAsyncListener != null) iAsyncListener.onResult(0, "连接成功");
                        }
                    });
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[推送] - 链接失败");
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
        LogsUtil.info(TAG, "[订阅] - 正在进行连接：%s - %s - %s", brokerServer, ClientId, UserName);

        // 已连接则直接返回
        if (mSubClient != null && mSubClient.getState().isConnected()) {
            LogsUtil.info(TAG, "[订阅] - 已经连接");
            if (iAsyncListener != null) iAsyncListener.onResult(0, "已经连接");
            return;
        }

        // 参数校验
        if (StringUtil.isEmpty(brokerServer)) {
            LogsUtil.error(TAG, "[订阅] - 缺少Broker服务器地址");
            if (iAsyncListener != null) iAsyncListener.onResult(2, "缺少Broker服务器地址");
            return;
        }
        if (StringUtil.isEmpty(ClientId)) {
            LogsUtil.error(TAG, "[订阅] - 缺少客户端ID");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "缺少客户端ID");
            return;
        }

        try {
            // 解析 broker 地址中的 host 和 port
            URI uri = new URI(brokerServer);
            String host = uri.getHost();   // 获取主机地址
            int port = uri.getPort();      // 获取端口号
            byte[] pwd = Password == null ? new byte[0] : Password.getBytes(StandardCharsets.UTF_8);

            // 创建异步 MQTT 5 客户端
            mSubClient = MqttClient.builder().useMqttVersion5()                 // 使用 MQTT 5 协议
                    .identifier(ClientId)              // 客户端唯一标识（ClientId）
                    .serverHost(host)                  // 设置服务器地址
                    .serverPort(port)                  // 设置服务器端口

                    .simpleAuth()                      // 开启简单用户名/密码认证
                    .username(UserName)                // 设置用户名
                    .password(pwd)                     // 设置密码
                    .applySimpleAuth()                 // 应用认证配置

                    // 自动重连策略配置：指数退避（失败一次延迟翻倍）初始重连间隔1s，直到最大1m重连间隔
                    .automaticReconnect()
                    .initialDelay(this.initialDelayMs, TimeUnit.MILLISECONDS)   // 初始重连间隔：10秒
                    .maxDelay(this.maxDelayMs, TimeUnit.MILLISECONDS)           // 最大重连间隔：2分钟
                    .applyAutomaticReconnect()

                    // 网络传输参数配置
                    .transportConfig()
                    .mqttConnectTimeout(10, TimeUnit.SECONDS)  // MQTT CONNECT 报文的超时时间
                    .socketConnectTimeout(5, TimeUnit.SECONDS) // TCP 连接超时时间
                    .applyTransportConfig()

                    .addConnectedListener(ctx -> {      // 连接成功监听器
                        LogsUtil.info(TAG, "[订阅] - ✅ 已连接，心跳 20s，监听自动重连");
                        ExecutionThrottle.getInstance().run(data -> {
                            NotifyService.getInstance().asyncPush(ClientId
                                    , "NOTIFY.TEXT"
                                    , ENotifyType.WECHATCORPBOT
                                    , new JSONObject() {{
                                        put("title", "MQTT连接成功");
                                        put("content", String.format("ClientId=%s,UserName=%s", ClientId, UserName));
                                    }}
                            );
                            return new SyncResult();
                        }, String.format("MQTT.Connected:%s", ClientId), ECacheTime.MINUTE);

                    })
                    .addDisconnectedListener(ctx -> {
                        Throwable cause = ctx.getCause();
                        LogsUtil.error(TAG, "[订阅] - ⚠️ 已断开 - %s - %s", ctx.getSource(), cause.getMessage());

                        ExecutionThrottle.getInstance().run(data -> {
                            NotifyService.getInstance().asyncPush(ClientId
                                    , "NOTIFY.TEXT"
                                    , ENotifyType.WECHATCORPBOT
                                    , new JSONObject() {{
                                        put("title", "MQTT已断开，尝试重连中...");
                                        put("content", String.format("ClientId=%s,UserName=%s", ClientId, UserName));
                                    }}
                            );
                            return new SyncResult();
                        }, String.format("MQTT.Disconnected:%s", ClientId), ECacheTime.MINUTE);
                    })
                    .buildAsync();                     // 构建异步客户端

            // 启动连接流程
            mSubClient.connectWith()
                    .cleanStart(true)      // 是否清除Session：true 表示不保留历史订阅和会话记录
                    .keepAlive(30)         // 设置心跳间隔时间（秒）：客户端多久发一次 PING 请求，建议20~60秒
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        // 异步连接完成后回调
                        if (throwable != null) {
                            // 这里可以做手动重连
                            LogsUtil.error(TAG, "[订阅] - ⚠️ 连接失败：%s", throwable.getMessage());
                            if (iAsyncListener != null) {
                                iAsyncListener.onResult(1, "连接失败：" + throwable.getMessage());
                            }
                        } else {
                            LogsUtil.info(TAG, "[订阅] - ✅ 连接成功");
                            if (iAsyncListener != null) iAsyncListener.onResult(0, "连接成功");
                        }
                    });
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[订阅] - 链接失败");
        }
    }

    /**
     * 自动重连策略配置
     *
     * @param initialDelayMs 初始重连间隔（毫秒）
     * @param maxDelayMs     最大重连间隔（毫秒）
     */
    public IXMQTTClient setReconnectConfig(long initialDelayMs, long maxDelayMs) {
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        return this;
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
        publish(topic, JSONObject.toJSONString(syncResult, JSONWriter.Feature.IgnoreNonFieldGetter, JSONWriter.Feature.IgnoreErrorGetter, JSONWriter.Feature.IgnoreNoneSerializable), qos, retained, null);
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
        publish(topic, JSONObject.toJSONString(syncResult, JSONWriter.Feature.IgnoreNonFieldGetter, JSONWriter.Feature.IgnoreErrorGetter, JSONWriter.Feature.IgnoreNoneSerializable), qos, retained, iAsyncListener);
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
     * @param topic          消息主题（支持动态变量替换）
     * @param content        消息内容
     * @param qos            消息质量等级：0=最多一次，1=至少一次，2=仅一次
     * @param retained       是否保留该消息
     * @param iAsyncListener 异步监听是否推送成功
     */
    @Override
    public void publish(@NonNull String topic, String content, int qos, boolean retained, IAsyncListener iAsyncListener) {
        try {
            // 替换并格式化主题
            topic = replaceTopicEnvParams(topic);
            topic = XMQTTUtils.formatTopic(topic);

            if (StringUtil.isEmpty(topic)) {
                LogsUtil.warn(TAG, "[推送] - 主题为空，取消发送");
                if (iAsyncListener != null) iAsyncListener.onResult(2, "推送主题为空，取消发送");
                return;
            }

            LogsUtil.info(TAG, "[推送] - 主题：%s Qos：%s 保留：%s 消息：%s", topic, qos, retained, content);

            if (mPubClient == null) {
                LogsUtil.error(TAG, "[推送] - 客户端未初始化");
                if (iAsyncListener != null) iAsyncListener.onResult(2, "推送客户端未初始化");
                return;
            }

            // 检查连接状态
            if (!mPubClient.getState().isConnected()) {
                LogsUtil.warn(TAG, "[推送] - 客户端未连接，消息未发送");
                if (iAsyncListener != null) iAsyncListener.onResult(2, "推送客户端未连接，消息未发送");
//                reinitialize(); // 重新初始化
                return;
            }

            // 设置消息质量
            MqttQos mqttQos = MqttQos.fromCode(qos);
            if (mqttQos == null) mqttQos = MqttQos.AT_MOST_ONCE;

            // 发送异步消息
            mPubClient.publishWith().topic(topic).payload(content.getBytes(StandardCharsets.UTF_8)).qos(mqttQos).retain(retained).send().whenComplete((ack, throwable) -> {
                if (throwable != null) {
                    LogsUtil.warn(TAG, "[推送] - 主题：%s Qos：%s 保留：%s 消息：%s - 消息发送失败：%s", ack.getPublish().getTopic(), qos, retained, content, throwable.getMessage());
                    if (iAsyncListener != null)
                        iAsyncListener.onResult(10, "推送消息发送失败：" + throwable.getMessage());
                }
                if (iAsyncListener != null) iAsyncListener.onResult(0, "");
            });
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[推送] - 发送异常");
            if (iAsyncListener != null) iAsyncListener.onResult(3, "推送发送异常: " + e.getMessage());
        }
    }

    //endregion

    //region 订阅消息

    /**
     * 订阅消息主题（异步）
     *
     * @param topic              订阅主题（支持变量替换）
     * @param qos                消息质量等级：0=最多一次，1=至少一次，2=仅一次
     * @param subMessageListener 消息处理回调监听器（每条消息触发）
     * @return 是否触发订阅操作
     */
    @Override
    public boolean subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener) {
        subscribe(topic, qos, subMessageListener, (resultCode, asyncResult) -> {
            if (resultCode != 0) LogsUtil.warn(TAG, asyncResult.toString());
        });
        return true;
    }

    @Override
    public void subscribe(@NonNull String topic, int qos, IXMQTTSubMessageListener subMessageListener, IAsyncListener iAsyncListener) {
        try {
            // 处理主题变量和格式
            topic = replaceTopicEnvParams(topic);
            topic = XMQTTUtils.formatTopic(topic);

            if (StringUtil.isEmpty(topic)) {
                if (iAsyncListener != null) iAsyncListener.onResult(2, "订阅主题为空");
                return;
            }

            LogsUtil.info(TAG, "[订阅] - 主题：%s QoS：%d", topic, qos);

            if (mSubClient == null) {
                if (iAsyncListener != null) iAsyncListener.onResult(3, "订阅客户端未初始化");
                return;
            }

            if (!mSubClient.getState().isConnected()) {
                LogsUtil.warn(TAG, "[订阅] - 订阅客户端未连接，取消订阅");
                if (iAsyncListener != null) iAsyncListener.onResult(4, "订阅客户端未连接，取消订阅");
//                reinitialize(); // 重新初始化
                return;
            }

            // 设置消息质量
            MqttQos mqttQos = MqttQos.fromCode(qos);
            if (mqttQos == null) mqttQos = MqttQos.AT_MOST_ONCE;

            // 注册订阅监听器（onMessage）
            mSubClient.toAsync().subscribeWith().topicFilter(topic).qos(mqttQos).callback(publish -> {
                String message = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                if (subMessageListener != null) {
                    subMessageListener.onMessage(publish.getTopic().toString(), publish.getQos().getCode(), message);
                }
            }).send().whenComplete((ack, throwable) -> {
                if (throwable != null) {
                    if (iAsyncListener != null) {
                        iAsyncListener.onResult(10, "订阅失败：%s" + throwable.getMessage());
                    }
                } else if (iAsyncListener != null) iAsyncListener.onResult(0, "");
            });
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[订阅] - 异常");
            if (iAsyncListener != null) iAsyncListener.onResult(5, "订阅发生异常：%s" + e.getMessage());
        }
    }

    /**
     * 取消订阅消息主题（异步）
     *
     * @param topic 要取消的主题
     * @return 是否触发取消订阅
     */
    @Override
    public boolean unsubscribe(@NonNull String topic) {
        try {
            topic = replaceTopicEnvParams(topic); // 替换变量
            topic = XMQTTUtils.formatTopic(topic); // 格式化主题

            if (StringUtil.isEmpty(topic)) {
                LogsUtil.warn(TAG, "[取消订阅] - 主题为空");
                return false;
            }

            LogsUtil.info(TAG, "[取消订阅] - 主题：%s", topic);

            if (mSubClient == null || !mSubClient.getState().isConnected()) {
                LogsUtil.warn(TAG, "[取消订阅] - 客户端未连接或未初始化");
                return false;
            }

            @NonNull String finalTopic = topic;
            mSubClient.unsubscribeWith().topicFilter(topic).send().whenComplete((ack, throwable) -> {
                if (throwable != null) {
                    LogsUtil.error(TAG, "[取消订阅] 主题：%s 操作失败：%s", finalTopic, throwable.getMessage());
                }
            });
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[取消订阅] 异常");
            return false;
        }
    }

    /**
     * 批量取消订阅消息主题（异步）
     *
     * @param topics 要取消的主题集合
     * @return 是否触发取消
     */
    @Override
    public boolean unsubscribe(@NonNull String[] topics) {
        if (topics.length == 0) {
            LogsUtil.warn(TAG, "[取消订阅] - 空的主题列表");
            return false;
        }

        if (mSubClient == null || !mSubClient.getState().isConnected()) {
            LogsUtil.warn(TAG, "[取消订阅] - 客户端未连接或未初始化");
            return false;
        }

        try {
            List<MqttTopicFilter> filters = Arrays.stream(topics).map(t -> MqttTopicFilter.of(XMQTTUtils.formatTopic(replaceTopicEnvParams(t)))).collect(Collectors.toList());

            mSubClient.unsubscribeWith().addTopicFilters(filters).send().whenComplete((ack, throwable) -> {
                if (throwable != null) {
                    LogsUtil.error(TAG, "[批量取消订阅] 失败:%s", throwable.getMessage());
                } else {
                    LogsUtil.info(TAG, "[批量取消订阅] 成功 - 共 %d 个主题", filters.size());
                }
            });
            return true;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[批量取消订阅] 异常");
            return false;
        }
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
        Method[] methods = target.getMethods();
        for (Method m : methods) {
            XMQTTSub xmqttSub = m.getAnnotation(XMQTTSub.class);
            if (xmqttSub == null) continue;
            String topic = xmqttSub.Topic();
            subscribe(topic, xmqttSub.Qos(), (topic1, qos, message) -> {
                try {
                    if (xmqttSub.openLog()) {
                        LogsUtil.info(TAG, "收到订阅消息 - 主题：%s Qos:%s 消息", topic1, qos, message);
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
                    LogsUtil.error(e, this.TAG, "处理MQTT订阅消息过程中发生错误");
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
