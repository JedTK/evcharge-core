package com.evcharge.rocketmq.consumer.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.entity.megadata.MDCommunitiesSummaryEntity;
import com.evcharge.entity.megadata.MDStreetDailySummaryEntity;
import com.evcharge.entity.megadata.MDStreetSummaryEntity;
import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;


/**
 * 以地区纬度的 RocketMQ 消费者类，处理从 RocketMQ 订阅并消费的消息。
 * 本类通过监听并处理消息队列中的不同任务来执行相应的业务逻辑。
 */
public class AreaXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "地区RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_Area";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "AreaTask";

    private volatile static AreaXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static AreaXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (AreaXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new AreaXRocketMQConsumerV2();
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
        super.TOPIC = AreaXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = AreaXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.HOUR;
    }

    /**
     * 街道 日数据统计 任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("StreetDaySummaryTask")
    public static SyncResult StreetDaySummaryTask(JSONObject message) {
        String street = JsonUtil.getString(message, "street");
        String street_code = JsonUtil.getString(message, "street_code");
        long start_time = JsonUtil.getLong(message, "start_time");
        long end_time = JsonUtil.getLong(message, "end_time");

        if (start_time == 0) start_time = TimeUtil.getTime00(-1);
        if (end_time == 0) end_time = TimeUtil.getTime00();

        LogsUtil.info("街道-日数据统计", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , street
                , street_code
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
        );

        MDStreetDailySummaryEntity.getInstance().syncData(street, street_code, start_time, end_time);
        return new SyncResult(0, "");
    }

    /**
     * 街道 数据统计 任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("StreetSummaryTask")
    public static SyncResult StreetSummaryTask(JSONObject message) {
        String street = JsonUtil.getString(message, "street");
        String street_code = JsonUtil.getString(message, "street_code");

        LogsUtil.info("街道-数据统计", "(%s)%s - 正在执行任务中..."
                , street
                , street_code
        );

        MDStreetSummaryEntity.getInstance().syncTaskJob(street, street_code);
        return new SyncResult(0, "");
    }

    /**
     * 社区 数据统计 任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("CommunitiesSummaryTask")
    public static SyncResult CommunitiesSummaryTask(JSONObject message) {
        String street = JsonUtil.getString(message, "street");
        String street_code = JsonUtil.getString(message, "street_code");
        String communities = JsonUtil.getString(message, "communities");

        LogsUtil.info("街道-数据统计", "(%s)%s %s - 正在执行任务中...", street_code, street, communities);

        MDCommunitiesSummaryEntity.getInstance().syncTaskJob(street, street_code, communities);
        return new SyncResult(0, "");
    }
}