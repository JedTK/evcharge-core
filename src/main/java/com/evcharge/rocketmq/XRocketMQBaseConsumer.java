package com.evcharge.rocketmq;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 投资者 RocketMQ 消费者类，处理从 RocketMQ 中订阅并消费的投资者相关任务消息。
 * <p>
 * 本类主要完成以下职责：
 * - 基于自定义注解 @CommandMapping 注册任务处理函数；
 * - 初始化 RocketMQ 消费者客户端并订阅主题与标签；
 * - 提供标签与处理逻辑的映射关系，通过 TAGS_ACTION 路由消息；
 * - 实现消息幂等性校验与并发限制，避免重复处理；
 * - 响应多个任务类型（如：设备日收益、月收益等）；
 */
public abstract class XRocketMQBaseConsumer {

    // region 消费者配置属性

    /**
     * 日志标签（用于标识当前消费者在日志输出中的前缀）
     */
    public String TAG = "RocketMQ基础消费者";

    /**
     * 订阅的 RocketMQ 主题，支持环境变量动态替换（如 dev_Base、prod_Base）
     */
    public String TOPIC = "{ENV}_Base";

    /**
     * 消费者分组名称（多个消费者组不能重复，组内消费者将进行负载均衡）
     */
    public String GROUP_NAME = "Base";

    /**
     * 封装后的 RocketMQ 客户端对象（内置创建、订阅、监听逻辑）
     */
    private XRocketMQ X_ROCKET_MQ = null;

    /**
     * 消费消息监听类型（并发消费 or 有序消费），默认为并发
     */
    public XRocketMQConsumerMessageListenerType xRocketMQConsumerMessageListenerType = XRocketMQConsumerMessageListenerType.Concurrently;

    /**
     * 消费起始位置策略，默认从上一次消费进度开始
     */
    public ConsumeFromWhere consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;

    /**
     * 消费模式（集群消费 or 广播消费），默认使用 CLUSTERING 模式
     */
    public MessageModel messageModel = MessageModel.CLUSTERING;

    /**
     * 消息过期时间（单位：毫秒），用于过滤掉过期消息，默认 30 分钟
     */
    public long MessageExpiredTime = ECacheTime.MINUTE * 30;

    /**
     * 标签 -> 执行逻辑函数 的映射表
     * 消息消费时通过 tags 匹配对应处理函数进行业务处理
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, BiFunction<JSONObject, Void, ISyncResult>> TAGS_ACTION = new ConcurrentHashMap<>();

    /**
     * 执行限流器，控制高频消费或重复消费（底层基于 Redis 锁实现）
     */
    private ExecutionThrottle Throttle;

    // endregion

    /**
     * 构造函数：执行初始化逻辑
     * - 调用 init() 由子类设置 Topic、GroupName、TAG 等参数；
     * - 自动注册当前类中带有 @CommandMapping 注解的方法到 TAGS_ACTION；
     */
    public XRocketMQBaseConsumer() {
        init();
        registerCommandMappings();
    }

    /**
     * 抽象方法：子类需重写此方法初始化配置
     * 通常用于设置 Topic、GroupName、TAG 等参数
     */
    protected abstract void init();

    /**
     * 启动消费者监听：
     * - 创建 RocketMQ 客户端并配置 NameServer、消费参数；
     * - 注册监听器（并发或有序）；
     * - 订阅所有注册的标签；
     * - 启动消费者客户端连接与监听。
     */
    public void start() {
        if (X_ROCKET_MQ == null) {
            String RocketMQNameServer = ConfigManager.getString("RocketMQ.NameServer", SysGlobalConfigEntity.getString("RocketMQ:NameServer"));
            String RocketMQSubAccessKey = ConfigManager.getString("RocketMQ.Sub.AccessKey", SysGlobalConfigEntity.getString("RocketMQ:Sub:AccessKey"));
            String RocketMQSubSecretKey = ConfigManager.getString("RocketMQ.Sub.SecretKey", SysGlobalConfigEntity.getString("RocketMQ:Sub:SecretKey"));
            boolean VipChannelEnabled = ConfigManager.getBoolean("RocketMQ.Sub.VIPChannel", SysGlobalConfigEntity.getBool("RocketMQ:Sub:VIPChannel", false));

            X_ROCKET_MQ = new XRocketMQ();
            X_ROCKET_MQ.setEnv(ConfigManager.getString("config.type"))
                    .setTAG(TAG)
                    .createConsumerClient(
                            RocketMQNameServer,
                            GROUP_NAME,
                            RocketMQSubAccessKey,
                            RocketMQSubSecretKey,
                            consumeFromWhere,
                            messageModel
                    );

            // 配置线程池与消费参数
            X_ROCKET_MQ.getConsumerClient().setVipChannelEnabled(VipChannelEnabled); // 设置VIP通道
            X_ROCKET_MQ.getConsumerClient().setConsumeThreadMax(20);     // 最大并发线程数
            X_ROCKET_MQ.getConsumerClient().setConsumeThreadMin(1);      // 最小线程数
            X_ROCKET_MQ.getConsumerClient().setMaxReconsumeTimes(3);     // 最大重试次数
            X_ROCKET_MQ.getConsumerClient().setPullInterval(500);        // 拉取间隔（毫秒）
            X_ROCKET_MQ.getConsumerClient().setPullBatchSize(10);        // 每批拉取消息数量
            X_ROCKET_MQ.getConsumerClient().setConsumeTimeout(5);        // 消费超时时间（分钟）
        }

        // 注册监听器
        if (xRocketMQConsumerMessageListenerType == XRocketMQConsumerMessageListenerType.Concurrently) {
            X_ROCKET_MQ.registerMessageListener(messageListenerConcurrently);
        } else {
            X_ROCKET_MQ.registerMessageListener(messageListenerOrderly);
        }

        // 注册所有 TAG
        X_ROCKET_MQ.subscribe(TOPIC, TAGS_ACTION.keySet().toArray(new String[0]));
        X_ROCKET_MQ.startConsumerClient();

        LogsUtil.info(TAG, "启动消费者 Group=%s, Topic=%s", X_ROCKET_MQ.getConsumerClient().getConsumerGroup(), this.TOPIC);
    }

    /**
     * 注册 @CommandMapping 注解的执行方法到 TAGS_ACTION 映射表
     * 用于通过标签触发对应的处理逻辑
     */
    private void registerCommandMappings() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(CommandMapping.class)) {
                CommandMapping command = method.getAnnotation(CommandMapping.class);
                if (command == null) continue;
                String commandValue = command.value();
                TAGS_ACTION.put(commandValue, (json, unused) -> {
                    try {
                        return (ISyncResult) method.invoke(null, json);
                    } catch (Exception e) {
                        LogsUtil.error(e, TAG, "执行命令 %s 时出错", commandValue);
                        return new SyncResult(1, "执行失败");
                    }
                });
            }
        }
    }

    /**
     * RocketMQ 并发消息监听器实现：
     * - 适用于大多数无顺序要求的任务；
     * - 检查消息是否过期；
     * - 限制重试次数；
     * - 通过 tag 找到函数执行；
     */
    protected MessageListenerConcurrently messageListenerConcurrently = (list, context) -> {
        try {
            for (MessageExt messageExt : list) {
                long create_time = Convert.toLong(messageExt.getUserProperty("create_time"));
                if (create_time != 0 && create_time + MessageExpiredTime <= TimeUtil.getTimestamp()) {
                    LogsUtil.warn(TAG, "[消息过期] - %s/%s/%s 创建时间：%s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), TimeUtil.toTimeString(create_time));
                    continue;
                }

                if (messageExt.getReconsumeTimes() > 1) continue;

                String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                LogsUtil.info(TAG, "[订阅消息] - /%s/%s/%s %s - %s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), body);
                if (!StringUtil.hasLength(body)) continue;

                JSONObject message = JSONObject.parse(body);
                if (message == null) continue;

                BiFunction<JSONObject, Void, ISyncResult> function = TAGS_ACTION.get(messageExt.getTags());
                if (function == null) {
                    LogsUtil.info(X_ROCKET_MQ.TAG, "[订阅消息] - 无效主题 - %s/%s/%s %s - %s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), body);
                    continue;
                }

                ISyncResult result = function.apply(message, null);
                if (!result.isSuccess()) LogsUtil.warn(TAG, result.getMsg());
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "处理消费消息发生错误");
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    };

    /**
     * RocketMQ 有序消息监听器实现：
     * - 适用于需要顺序处理的任务；
     * - 逻辑与并发监听器类似，但保证同一队列按顺序消费；
     */
    protected MessageListenerOrderly messageListenerOrderly = (list, context) -> {
        try {
            for (MessageExt messageExt : list) {
                long create_time = Convert.toLong(messageExt.getUserProperty("create_time"));
                if (create_time != 0 && create_time + MessageExpiredTime <= TimeUtil.getTimestamp()) {
                    LogsUtil.warn(TAG, "[消息过期] - %s/%s/%s 创建时间：%s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), TimeUtil.toTimeString(create_time));
                    continue;
                }

                if (messageExt.getReconsumeTimes() > 1) continue;

                String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                LogsUtil.info(TAG, "[订阅消息] - /%s/%s/%s %s - %s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), body);
                if (!StringUtil.hasLength(body)) continue;

                JSONObject message = JSONObject.parse(body);
                if (message == null) continue;

                BiFunction<JSONObject, Void, ISyncResult> function = TAGS_ACTION.get(messageExt.getTags());
                if (function == null) {
                    LogsUtil.info(X_ROCKET_MQ.TAG, "[订阅消息] - 无效主题 - %s/%s/%s %s - %s", GROUP_NAME, messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), body);
                    continue;
                }

                ISyncResult result = function.apply(message, null);
                if (!result.isSuccess()) LogsUtil.warn(TAG, result.getMsg());
            }
            return ConsumeOrderlyStatus.SUCCESS;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "处理消费消息发生错误");
            return ConsumeOrderlyStatus.SUCCESS;
        }
    };

    /**
     * 获取消息消费限流器实例（惰性初始化），用于防止重复消费或高频调用
     *
     * @return ExecutionThrottle 限流控制器
     */
    public ExecutionThrottle getExecutionThrottle() {
        if (this.Throttle == null) {
            this.Throttle = new ExecutionThrottle(true, "", ECacheTime.DAY, false);
        }
        return this.Throttle;
    }
}
