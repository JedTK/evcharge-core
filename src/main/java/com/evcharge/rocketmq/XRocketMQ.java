package com.evcharge.rocketmq;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import lombok.Getter;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RocketMQ 生产者和消费者服务类
 * 提供了生产者和消费者的配置及消息推送和订阅功能。
 */
public class XRocketMQ {
    /**
     * 设置系统标签，用于方便查看日志
     */
    @Getter
    public String TAG = XRocketMQ.class.getSimpleName();

    /**
     * 生产者客户端实例
     */
    @Getter
    private DefaultMQProducer ProducerClient;

    /**
     * 消费者客户端实例
     */
    @Getter
    private DefaultMQPushConsumer ConsumerClient;

    /**
     * 全局唯一的 XRocketMQ 实例
     */
    private static volatile XRocketMQ instance;

    /**
     * 获取全局唯一的 XRocketMQ 实例
     * 使用双重检查锁定确保线程安全
     *
     * @return XRocketMQ 实例
     */
    public static XRocketMQ getGlobal() {
        if (instance == null) {
            synchronized (XRocketMQ.class) {
                if (instance == null) {
                    instance = new XRocketMQ();
                }
            }
        }
        return instance;
    }

    public XRocketMQ setTAG(String TAG) {
        this.TAG = TAG;
        return this;
    }

    //region 生产者 服务

    /**
     * 创建生产者
     *
     * @param nameServer NameServer地址
     * @param group      生产者组，用于将一组生产者实例进行逻辑上的分组，对生产者进行统一管理和监控
     */
    public XRocketMQ createProducerClient(String nameServer, String group) {
        return createProducerClient(nameServer, group, "", "");
    }

    /**
     * 创建生产者
     *
     * @param nameServer NameServer地址
     * @param group      生产者组，用于将一组生产者实例进行逻辑上的分组，对生产者进行统一管理和监控
     * @param accessKey  访问key
     * @param secretKey  密钥key
     */
    public XRocketMQ createProducerClient(String nameServer, String group, String accessKey, String secretKey) {
        if (this.ProducerClient != null) return this;

        if (StringUtils.hasLength(accessKey) && StringUtils.hasLength(secretKey)) {
            // 设置访问密钥和秘密密钥
            RPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
            // 实例化消息生产者Producer
            this.ProducerClient = new DefaultMQProducer(group, rpcHook);
        } else {
            // 实例化消息生产者Producer
            this.ProducerClient = new DefaultMQProducer(group);
        }
        // 设置NameServer的地址
        this.ProducerClient.setNamesrvAddr(nameServer);
        // 设置消息同步发送失败时的重试次数，默认为 2
//            this.ProducerClient.setRetryTimesWhenSendFailed(1);
        // 设置消息发送超时时间，默认3000ms
        this.ProducerClient.setSendMsgTimeout(5000);
        return this;
    }

    /**
     * 开启生产者
     */
    public XRocketMQ startProducerClient() {
        if (this.ProducerClient == null) {
            LogsUtil.warn(TAG, "[启动生产者] - 消费者客户端未创建");
            return this;
        }
        try {
            this.ProducerClient.start();
            LogsUtil.info(TAG, "\033[1;92m [生产者] - %s[%s] - 启动成功\033[0m"
                    , this.ProducerClient.getNamesrvAddr()
                    , this.ProducerClient.getProducerGroup());
        } catch (MQClientException e) {
            LogsUtil.error(e, TAG, "[生产者] - %s - 启动失败", this.ConsumerClient.getNamesrvAddr());
        }
        return this;
    }
    //endregion

    //region 消费者 服务

    /**
     * 创建消费者
     *
     * @param nameServer NameServer地址
     * @param group      消费者组，用于将一组消费者实例进行逻辑上的分组，对消费者进行统一管理和监控
     */
    public XRocketMQ createConsumerClient(String nameServer, String group) {
        return createConsumerClient(nameServer, group, "", "", ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET, MessageModel.CLUSTERING);
    }

    /**
     * 创建消费者
     *
     * @param nameServer NameServer地址
     * @param accessKey  访问key
     * @param secretKey  密钥key
     * @param group      消费者组，用于将一组消费者实例进行逻辑上的分组，对消费者进行统一管理和监控
     */
    public XRocketMQ createConsumerClient(String nameServer
            , String group
            , String accessKey
            , String secretKey) {
        return createConsumerClient(nameServer, group, accessKey, secretKey, ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET, MessageModel.CLUSTERING);
    }

    /**
     * 创建消费者
     *
     * @param nameServer       NameServer地址
     * @param group            消费者组，用于将一组消费者实例进行逻辑上的分组，对消费者进行统一管理和监控
     * @param accessKey        访问key
     * @param secretKey        密钥key
     * @param consumeFromWhere 消费策略，指定了消费者从哪里开始消费消息的策略
     *                         CONSUME_FROM_LAST_OFFSET：从上次消费的位置开始（默认）。如果是新的消费者组，消费从最新消息开始，适用于生产环境的正常消费，消费者可以从上次消费的位置继续消费。
     *                         CONSUME_FROM_FIRST_OFFSET：从队列的起始位置开始消费，适用于新消费者组第一次消费时需要处理所有历史消息的场景。
     *                         CONSUME_FROM_TIMESTAMP：从指定时间戳的消息开始消费，适用于需要从特定时间点开始消费消息的场景，如重新处理某一时间段内的消息。
     * @param messageModel     消息的消费模式
     *                         CLUSTERING：集群消费模式，同一个消费组内的消费者会负载均衡地消费消息
     *                         BROADCASTING：广播消费模式，同一个消费组内的每个消费者都会消费到所有的消息
     */
    public XRocketMQ createConsumerClient(String nameServer
            , String group
            , String accessKey
            , String secretKey
            , ConsumeFromWhere consumeFromWhere
            , MessageModel messageModel) {
        try {
            // 如果消费者客户端已经存在，则直接返回当前实例
            if (this.ConsumerClient != null) {
                // 已存在则更新 NameServer，避免“connect to null”
//                if (StringUtil.hasText(nameServer)) {
//                    this.ProducerClient.setNamesrvAddr(nameServer.trim());
//                }
                return this;
            }
            if (StringUtils.hasLength(accessKey) && StringUtils.hasLength(secretKey)) {
                // 设置访问密钥和秘密密钥
                RPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(accessKey, secretKey));
                // 实例化消息生产者Producer
                this.ConsumerClient = new DefaultMQPushConsumer(group, rpcHook);
            } else {
                // 实例化消息生产者Producer
                this.ConsumerClient = new DefaultMQPushConsumer(group);
            }

            // 设置NameServer地址
            this.ConsumerClient.setNamesrvAddr(nameServer);

            // 设置消费策略，默认为从上次消费的位置开始
            if (consumeFromWhere != null) {
                this.ConsumerClient.setConsumeFromWhere(consumeFromWhere);
            }

            // 设置消息消费模式，默认为集群消费模式
            if (messageModel != null) {
                this.ConsumerClient.setMessageModel(messageModel);
            }

//            // 设置线程最大使用量
//            X_ROCKET_MQ.getConsumerClient().setConsumeThreadMax(10);
//            // 设置现场最小使用量
//            X_ROCKET_MQ.getConsumerClient().setConsumeThreadMin(5);
//            // 设置最大恢复次数，默认值：16
//            X_ROCKET_MQ.getConsumerClient().setMaxReconsumeTimes(3);
//            // 消息拉取间隔，默认值：0（单位：毫秒）
//            X_ROCKET_MQ.getConsumerClient().setPullInterval(500);
//            // 单次最大拉取消息数量,默认值：32
//            X_ROCKET_MQ.getConsumerClient().setPullBatchSize(10);
//            // 消费者本地缓存消息数量,默认值：1
//            X_ROCKET_MQ.getConsumerClient().setConsumeMessageBatchMaxSize(1);
//            // 设置消息的消费超时时间，默认值：15分钟
//            X_ROCKET_MQ.getConsumerClient().setConsumeTimeout(5);
//            // 费者负载均衡的间隔时间：
//            X_ROCKET_MQ.getConsumerClient().setAdjustThreadPoolNumsThreshold(1000);


            LogsUtil.info(TAG, "\033[1;92m [消费者] - %s[%s] - 已被创建,等待订阅和开启服务\033[0m"
                    , nameServer
                    , this.ConsumerClient.getConsumerGroup());
        } catch (Exception e) {
            // 记录错误日志，表明在启动新的消费者服务时发生了错误
            LogsUtil.error(e, TAG, "[消费者] - %s - 启动失败", nameServer);
        }
        return this;
    }

    /**
     * 开启 消费者 服务（开启之前必须要设置好订阅）
     */
    public XRocketMQ startConsumerClient() {
        if (this.ConsumerClient == null) {
            LogsUtil.warn(TAG, "[启动消费者] - 消费者客户端未创建");
            return this;
        }
        try {
            LogsUtil.info(TAG, "\033[1;92m [消费者] - %s - 正在连接...\033[0m", this.ConsumerClient.getNamesrvAddr());

            this.ConsumerClient.start();

            LogsUtil.info(TAG, "\033[1;92m [消费者] - %s[%s] - 启动成功\033[0m"
                    , this.ConsumerClient.getNamesrvAddr()
                    , this.ConsumerClient.getConsumerGroup());
        } catch (MQClientException e) {
            LogsUtil.error(e, TAG, "[消费者] - %s - 启动失败", this.ConsumerClient.getNamesrvAddr());
        }
        return this;
    }
    //endregion

    // region 生产者 - 推送消息

    // region 同步推送消息

    //region 同步推送消息字符串格式

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull String content) {
        return pushSync(topic, tags, content, getUniqueKey(), 3000, 0);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @param timeout 超时时间
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull String content, long timeout) {
        return pushSync(topic, tags, content, getUniqueKey(), timeout, 0);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @param timeout 超时时间
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull String content, long timeout, long delayTimeMs) {
        return pushSync(topic, tags, content, getUniqueKey(), timeout, delayTimeMs);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param keys        唯一键
     * @param timeout     超时时间
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull String content, @NonNull String keys, long timeout, long delayTimeMs) {
        try {
            Message message = new Message(topic // 主题：要做到消息区分，不同同一消费组名和不同主题
                    , tags // 子主题：区分不同业务
                    , keys // 用于保证消息唯一性
                    , content.getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            return pushSync(message, timeout);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[同步推] - 发生错误");
        }
        return new SyncResult(1, "发送失败");
    }
    //endregion

    //region 同步推送消息 JSON格式

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content) {
        return pushSync(topic, tags, content, getUniqueKey(), 3000, 0);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @param timeout 超时时间
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, long timeout) {
        return pushSync(topic, tags, content, getUniqueKey(), timeout, 0);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param timeout     超时时间
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, long timeout, long delayTimeMs) {
        return pushSync(topic, tags, content, getUniqueKey(), timeout, delayTimeMs);
    }

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param keys        唯一键
     * @param timeout     超时时间
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, @NonNull String keys, long timeout, long delayTimeMs) {
        try {
            Message message = new Message(topic // 主题：要做到消息区分，不同同一消费组名和不同主题
                    , tags // 子主题：区分不同业务
                    , keys // 用于保证消息唯一性
                    , content.toJSONString().getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            return pushSync(message, timeout);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[同步推] - 发生错误");
        }
        return new SyncResult(1, "发送失败");
    }
    //endregion

    /**
     * 同步推送消息（线程阻塞）
     * 可靠性：更高的可靠性，生产者可以确认消息的发送状态
     * 响应时间：较长
     * 适用场景：适用于关键业务操作，需要确保消息发送成功的场景
     *
     * @param message 消息体
     * @param timeout 发送超时
     * @return 发送结果
     */
    public SyncResult pushSync(@NonNull Message message, long timeout) {
        if (this.ProducerClient == null) {
            LogsUtil.warn(TAG, "[同步推送] - 请先启动生产者服务");
            return new SyncResult(70001, "请先启动生产者服务");
        }
        try {
            // 替换主题中的变量
            String topic = applyEnvParamsToTopic(message.getTopic());
            String tags = applyEnvParamsToTopic(message.getTags());
            message.setTopic(topic);
            message.setTags(tags);
            message.putUserProperty("create_time", String.format("%s", TimeUtil.getTimestamp())); //消息创建时间

            // 记录详细信息
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            LogsUtil.info(TAG, "\033[1;92m [同步推送] - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                    , this.ProducerClient.getProducerGroup()
                    , topic
                    , tags
                    , message.getKeys()
                    , messageBody);

            SendResult r = this.ProducerClient.send(message, timeout);
            switch (r.getSendStatus()) {
                case SEND_OK:
                    LogsUtil.info(TAG, "\033[1;92m [同步推送]- 成功 - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                            , this.ProducerClient.getProducerGroup()
                            , topic
                            , tags
                            , message.getKeys()
                            , messageBody);
                    return new SyncResult(0, "");
                case FLUSH_DISK_TIMEOUT:
                    LogsUtil.warn(TAG, "\033[1;92m [同步推送]- 发生错误 - 刷新磁盘超时 - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                            , this.ProducerClient.getProducerGroup()
                            , topic
                            , tags
                            , message.getKeys()
                            , messageBody);
                    break;
                case FLUSH_SLAVE_TIMEOUT:
                    LogsUtil.warn(TAG, "\033[1;92m [同步推送]- 发生错误 - 刷新从属超时 - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                            , this.ProducerClient.getProducerGroup()
                            , topic
                            , tags
                            , message.getKeys()
                            , messageBody);
                    break;
                case SLAVE_NOT_AVAILABLE:
                    LogsUtil.warn(TAG, "\033[1;92m [同步推送]- 发生错误 - 从站不可用 - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                            , this.ProducerClient.getProducerGroup()
                            , topic
                            , tags
                            , message.getKeys()
                            , messageBody);
                    break;
            }
        } catch (RemotingException | InterruptedException | MQClientException | MQBrokerException e) {
            LogsUtil.error(e, TAG, "[同步推送] - 发生错误");
        }
        return new SyncResult(1, "发送失败");
    }

    // endregion

    // region 异步推送消息

    // region 异步推送消息字符串格式

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull String content, SendCallback sendCallback) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, 3000, 0);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull String content, SendCallback sendCallback, long timeout) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, timeout, 0);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     * @param delayTimeMs  延迟推送时间（毫秒）
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull String content, SendCallback sendCallback, long timeout, long delayTimeMs) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, timeout, delayTimeMs);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param keys         唯一键
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     * @param delayTimeMs  延迟推送时间（毫秒）
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull String content, @NonNull String keys, SendCallback sendCallback, long timeout, long delayTimeMs) {
        try {
            Message message = new Message(topic, tags, keys, content.getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            pushAsync(message, sendCallback, timeout);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[异步推] - 发生错误");
        }
    }

    // endregion

    // region 异步推送消息 JSON格式

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, SendCallback sendCallback) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, 3000, 0);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, SendCallback sendCallback, long timeout) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, timeout, 0);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     * @param delayTimeMs  延迟推送时间（毫秒）
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, SendCallback sendCallback, long timeout, long delayTimeMs) {
        pushAsync(topic, tags, content, getUniqueKey(), sendCallback, timeout, delayTimeMs);
    }

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param topic        主题
     * @param tags         标签
     * @param content      内容
     * @param keys         唯一键
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     * @param delayTimeMs  延迟推送时间（毫秒）
     */
    public void pushAsync(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, @NonNull String keys, SendCallback sendCallback, long timeout, long delayTimeMs) {
        try {
            Message message = new Message(topic, tags, keys, content.toJSONString().getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            pushAsync(message, sendCallback, timeout);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[异步推] - 发生错误");
        }
    }

    // endregion

    /**
     * 异步推送消息（线程非阻塞）
     * 可靠性：需要通过回调函数处理可能的发送失败情况
     * 响应时间：短
     * 适用场景：适用于非关键业务操作，对响应时间要求高的场景
     *
     * @param message      消息体
     * @param sendCallback 发送回调
     * @param timeout      发送超时
     */
    public void pushAsync(@NonNull Message message, SendCallback sendCallback, long timeout) {
        if (this.ProducerClient == null) {
            LogsUtil.warn(TAG, "[异步推送] - 请先启动生产者服务");
            return;
        }
        try {
            // 替换主题中的变量
            String topic = applyEnvParamsToTopic(message.getTopic());
            String tags = applyEnvParamsToTopic(message.getTags());
            message.setTopic(topic);
            message.setTags(tags);
            message.putUserProperty("create_time", String.format("%s", TimeUtil.getTimestamp())); //消息创建时间

            // 记录详细信息
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            LogsUtil.info(TAG, "\033[1;92m [异步推送] - Group: %s  %s/%s 消息[%s]：%s \033[0m"
                    , this.ProducerClient.getProducerGroup()
                    , topic
                    , tags
                    , message.getKeys()
                    , messageBody);

            this.ProducerClient.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult r) {
                    switch (r.getSendStatus()) {
                        case FLUSH_DISK_TIMEOUT:
                            LogsUtil.warn(TAG, "[异步推送] - 发生错误 - 刷新磁盘超时");
                            break;
                        case FLUSH_SLAVE_TIMEOUT:
                            LogsUtil.warn(TAG, "[异步推送] - 发生错误 - 刷新从属超时");
                            break;
                        case SLAVE_NOT_AVAILABLE:
                            LogsUtil.warn(TAG, "[异步推送] - 发生错误 - 从站不可用");
                            break;
                    }
                    if (sendCallback != null) sendCallback.onSuccess(r);
                }

                @Override
                public void onException(Throwable throwable) {
                    LogsUtil.error((Exception) throwable, TAG, "[异步推送] - 发生错误");
                    if (sendCallback != null) sendCallback.onException(throwable);
                }
            }, timeout);
        } catch (RemotingException | InterruptedException | MQClientException e) {
            LogsUtil.error(e, TAG, "[异步推送] - 发生错误");
        }
    }
    // endregion

    // region 单向推送消息

    // region 单向推送消息字符串格式

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull String content) {
        return pushOneway(topic, tags, content, getUniqueKey(), 0);
    }

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull String content, long delayTimeMs) {
        return pushOneway(topic, tags, content, getUniqueKey(), delayTimeMs);
    }

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param keys        唯一键
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull String content, @NonNull String keys, long delayTimeMs) {
        try {
            Message message = new Message(topic, tags, keys, content.getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            return pushOneway(message);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[单向推] - 发生错误");
        }
        return false;
    }
    // endregion

    // region 单向推送消息 JSON格式

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic   主题
     * @param tags    标签
     * @param content 内容
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content) {
        return pushOneway(topic, tags, content, getUniqueKey(), 0);
    }

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, long delayTimeMs) {
        return pushOneway(topic, tags, content, getUniqueKey(), delayTimeMs);
    }

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param topic       主题
     * @param tags        标签
     * @param content     内容
     * @param keys        唯一键
     * @param delayTimeMs 延迟推送时间（毫秒）
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull String topic, @NonNull String tags, @NonNull JSONObject content, @NonNull String keys, long delayTimeMs) {
        try {
            Message message = new Message(topic, tags, keys, content.toJSONString().getBytes(RemotingHelper.DEFAULT_CHARSET));
            if (delayTimeMs > 0) message.setDelayTimeMs(delayTimeMs);
            return pushOneway(message);
        } catch (UnsupportedEncodingException e) {
            LogsUtil.error(e, TAG, "[单向推] - 发生错误");
        }
        return false;
    }

    // endregion

    /**
     * 单向推送消息（线程非阻塞） - 生产者发送消息后，不等待Broker的响应，也不进行任何回调处理，消息发送立即返回
     * 可靠性：因为不等待任何响应，也没有回调函数处理，无法确认消息是否成功到达
     * 响应时间：短
     * 适用场景：适用于对消息可靠性要求不高，但需要极高吞吐量的场景，例如日志收集、统计数据等。
     *
     * @param message 消息体
     * @return 是否发送成功
     */
    public boolean pushOneway(@NonNull Message message) {
        if (this.ProducerClient == null) {
            LogsUtil.warn(TAG, "[单向推送] - 请先启动生产者服务");
            return false;
        }
        try {
            // 替换主题中的变量
            String topic = applyEnvParamsToTopic(message.getTopic());
            String tags = applyEnvParamsToTopic(message.getTags());
            message.setTopic(topic);
            message.setTags(tags);
            message.putUserProperty("create_time", String.format("%s", TimeUtil.getTimestamp())); //消息创建时间

            // 记录详细信息
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            LogsUtil.info(TAG, "\033[1;92m [单向推送] - Group: %s %s/%s 消息[%s]：%s \033[0m"
                    , this.ProducerClient.getProducerGroup()
                    , topic
                    , tags
                    , message.getKeys()
                    , messageBody);

            this.ProducerClient.sendOneway(message);
            return true;
        } catch (RemotingException | InterruptedException | MQClientException e) {
            LogsUtil.error(e, TAG, "[单向推送] - 发生错误");
        }
        return false;
    }
    // endregion

    // endregion

    // region 消费者 - 订阅消息

    /**
     * 注册 有序消费 消息监听器
     *
     * @param listener 消息监听器
     */
    public XRocketMQ registerMessageListener(@NonNull MessageListenerOrderly listener) {
        if (this.ConsumerClient == null) {
            LogsUtil.warn(TAG, "[订阅消息] - 请先启动消费者服务");
            return this;
        }
        this.ConsumerClient.registerMessageListener(listener);
        return this;
    }

    /**
     * 注册 并发消费 消息监听器
     *
     * @param listener 消息监听器
     */
    public XRocketMQ registerMessageListener(@NonNull MessageListenerConcurrently listener) {
        if (this.ConsumerClient == null) {
            LogsUtil.warn(TAG, "[订阅消息] - 请先启动消费者服务");
            return this;
        }
        this.ConsumerClient.registerMessageListener(listener);
        return this;
    }

    /**
     * 消费者 订阅 主题
     *
     * @param topic 主题
     * @param tags  消息标签数组
     */
    public XRocketMQ subscribe(@NonNull String topic, @NonNull String[] tags) {
        if (this.ConsumerClient == null) {
            LogsUtil.warn(TAG, "[订阅消息] - 请先启动消费者服务");
            return this;
        }
        try {
            topic = applyEnvParamsToTopic(topic);
            StringBuilder subExpression = new StringBuilder();
            for (String tag : tags) {
                tag = applyEnvParamsToTopic(tag);
                if (subExpression.length() > 0) {
                    subExpression.append(" || ");
                }
                subExpression.append(tag);

                LogsUtil.info(TAG, "\033[1;92m [订阅消息] - Group: %s   %s/%s \033[0m", this.ConsumerClient.getConsumerGroup(), topic, tag);
            }
            this.ConsumerClient.subscribe(topic, subExpression.toString());
        } catch (MQClientException e) {
            LogsUtil.error(e, TAG, "[订阅消息] - 发生错误");
        }
        return this;
    }

    /**
     * 消费者 订阅 主题
     *
     * @param topic           主题
     * @param messageSelector 消息选择器
     */
    public XRocketMQ subscribe(@NonNull String topic, MessageSelector messageSelector) {
        if (this.ConsumerClient == null) {
            LogsUtil.warn(TAG, "[订阅消息] - 请先启动消费者服务");
            return this;
        }
        try {
            topic = applyEnvParamsToTopic(topic);
            LogsUtil.info(TAG, "\033[1;92m [订阅消息] - Group: %s   %s/* \033[0m", this.ConsumerClient.getConsumerGroup(), topic);
            this.ConsumerClient.subscribe(topic, messageSelector);
        } catch (MQClientException e) {
            LogsUtil.error(e, TAG, "[订阅消息] - 发生错误");
        }
        return this;
    }

    // endregion

    // region 辅助函数

    /**
     * 替换主题环境变量
     */
    private String applyEnvParamsToTopic(String topic) {
        for (Map.Entry<String, Object> entry : this.EnvParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 预编译正则表达式并使用Matcher.quoteReplacement来处理特殊字符
            Pattern pattern = Pattern.compile(String.format("(?i)\\{%s\\}", Pattern.quote(key)));
            Matcher matcher = pattern.matcher(topic);
            topic = matcher.replaceAll(Matcher.quoteReplacement(value.toString()));
        }
        return formatTopic(topic);
    }

    /**
     * 格式化主题，以确保它不包含不必要的字符，同时保留 MQTT 通配符。
     *
     * @param topic 原始主题
     * @return 格式化后的主题
     */
    public static String formatTopic(String topic) {
        if (!StringUtils.hasLength(topic)) return "";
        // 替换空格和其他潜在问题字符，同时保留合法字符
        return topic.trim().replace(" ", "_")
                .replace(".", "_")
                .replace("/", "_")
                .replaceAll("[^a-zA-Z0-9_%-]", ""); // 仅保留字母、数字、下划线、中划线、百分号和MQTT通配符
    }

    /**
     * 获取唯一key
     */
    private static String getUniqueKey() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    // endregion

    //region （可选）定义全局环境变量
    /**
     * 自定义环境参数，可以通过addEnvParams插入环境参数，并且在主题中利用{参数名}来使用定义好的参数值
     */
    private final Map<String, Object> EnvParams = new LinkedHashMap<>();

    /**
     * （可选）设置平台变量（可用于区分平台订阅或推送）
     *
     * @param Platform 平台代码
     */
    public XRocketMQ setPlatform(String Platform) {
        return addEnvParams("Platform", Platform);
    }

    /**
     * （可选）设置环境变量（可以用于区分测试服务器、本地服务器、生产服务器）
     */
    public XRocketMQ setEnv(String env) {
        return addEnvParams("ENV", env);
    }

    /**
     * 新增环境参数
     *
     * @param key   参数名
     * @param value 参数值
     * @return 返回
     */
    public XRocketMQ addEnvParams(String key, Object value) {
        this.EnvParams.put(key, value);
        return this;
    }
    //endregion
}
