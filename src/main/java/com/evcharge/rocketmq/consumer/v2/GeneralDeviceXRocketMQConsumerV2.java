package com.evcharge.rocketmq.consumer.v2;

import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.xyzs.cache.ECacheTime;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;


/**
 * 通用设备 RocketMQ 消费者类，处理从 RocketMQ 订阅并消费的消息。
 * 本类通过监听并处理消息队列中的不同任务来执行相应的业务逻辑。
 */
public class GeneralDeviceXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "通用设备RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_GeneralDevice";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "GeneralDeviceTask";

    private volatile static GeneralDeviceXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static GeneralDeviceXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (GeneralDeviceXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new GeneralDeviceXRocketMQConsumerV2();
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
        super.TOPIC = GeneralDeviceXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = GeneralDeviceXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.MINUTE * 30;
    }
}