package com.evcharge.rocketmq.consumer.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.evcharge.task.RSProfitTaskJobV2;
import com.evcharge.task.RSProfitTaskSummaryTaskJobV2;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;


/**
 * 分润 RocketMQ 消费者类，处理从 RocketMQ 订阅并消费的消息。
 * 本类通过监听并处理消息队列中的不同任务来执行相应的业务逻辑。
 */
public class RSProfitXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "分润RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_RSProfit";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "RSProfitTask";

    private volatile static RSProfitXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static RSProfitXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (RSProfitXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new RSProfitXRocketMQConsumerV2();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化消费者配置，包括：
     * - 设置消费者组名、主题、消费起始位点；
     * - 消费模式为集群模式；
     * - 设置消息过期时间。
     */
    @Override
    public void init() {
        super.TOPIC = RSProfitXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = RSProfitXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.HOUR;
    }

    /**
     * 站点分润任务
     *
     * @param message 消息内容
     * @return 任务执行结果
     */
    @CommandMapping("ChargeStation")
    public static SyncResult ChargeStation(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        RSProfitTaskJobV2.execute(CSId);
        return new SyncResult(0, "");
    }

    /**
     * 分润任务
     *
     * @param message 消息内容
     * @return 任务执行结果
     */
    @CommandMapping("ChannelSummaryTask")
    public static SyncResult ChannelSummaryTask(JSONObject message) {
        String channel_phone = JsonUtil.getString(message, "channel_phone");
        RSProfitTaskSummaryTaskJobV2.execute(channel_phone);
        return new SyncResult(0, "");
    }
}