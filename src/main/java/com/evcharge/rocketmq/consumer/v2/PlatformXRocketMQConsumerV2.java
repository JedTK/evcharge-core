package com.evcharge.rocketmq.consumer.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.evcharge.task.PlatformDaySummaryTaskJobV2;
import com.evcharge.task.PlatformMonthSummaryTaskJobV2;
import com.evcharge.task.PlatformSummaryTaskJobV2;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

/**
 * 平台 RocketMQ 消费者类，处理从 RocketMQ 订阅并消费的消息。
 * 本类通过监听并处理消息队列中的不同任务来执行相应的业务逻辑。
 */
public class PlatformXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "平台RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_Platform";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "PlatformTask";

    private volatile static PlatformXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static PlatformXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (PlatformXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new PlatformXRocketMQConsumerV2();
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
        super.TOPIC = PlatformXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = PlatformXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.MINUTE * 30;
    }

    /**
     * 平台日数据统计任务的处理函数
     * 该任务用于生成平台的日统计数据
     *
     * @param message 消息内容
     * @return 任务执行结果
     */
    @CommandMapping("DaySummaryTaskV2")
    public static SyncResult DaySummaryTaskV2(JSONObject message) {
        String organize_code = JsonUtil.getString(message, "organize_code");
        String organize_name = JsonUtil.getString(message, "organize_name");
        long start_time = JsonUtil.getLong(message, "start_time");
        long end_time = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("平台日数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , organize_code
                , organize_name
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
        );

        // 执行日统计任务
        PlatformDaySummaryTaskJobV2.execute(organize_code, start_time, end_time);
        return new SyncResult(0, "");
    }

    /**
     * 平台月数据统计任务的处理函数
     * 该任务用于生成平台的月统计数据
     *
     * @param message 消息内容
     * @return 任务执行结果
     */
    @CommandMapping("MonthSummaryTaskV2")
    public static SyncResult MonthSummaryTaskV2(JSONObject message) {
        String organize_code = JsonUtil.getString(message, "organize_code");
        String organize_name = JsonUtil.getString(message, "organize_name");
        long start_time = JsonUtil.getLong(message, "start_time");
        long end_time = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("平台月数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , organize_code
                , organize_name
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
        );

        PlatformMonthSummaryTaskJobV2.execute(organize_code, start_time, end_time);
        return new SyncResult(0, "");
    }

    /**
     * 平台总数据统计任务的处理函数
     * 该任务用于生成平台的总统计数据
     *
     * @param message 消息内容
     * @return 任务执行结果
     */
    @CommandMapping("SummaryTaskV2")
    public static SyncResult SummaryTaskV2(JSONObject message) {
        String organize_code = JsonUtil.getString(message, "organize_code");
        String organize_name = JsonUtil.getString(message, "organize_name");

        LogsUtil.info("平台总数据统计任务", "(%s)%s 正在执行任务中...", organize_code, organize_name);

        // 执行总统计任务
        PlatformSummaryTaskJobV2.execute(organize_code);
        return new SyncResult(0, "");
    }
}