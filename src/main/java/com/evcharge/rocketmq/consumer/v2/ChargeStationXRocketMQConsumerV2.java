package com.evcharge.rocketmq.consumer.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.evcharge.task.*;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;

/**
 * 充电桩 RocketMQ 消费者类，处理从 RocketMQ 订阅并消费的消息。
 * 本类通过监听并处理消息队列中的不同任务来执行相应的业务逻辑。
 */
public class ChargeStationXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "充电桩RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_ChargeStation";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "ChargeStationTask";

    /**
     * 并发限制器：通过 Redis 缓存实现并发请求的限制，防止高频任务重复执行
     */
    private final static ExecutionThrottle mExecutionThrottle = new ExecutionThrottle(true
            , ""
            , ECacheTime.DAY
            , false);

    private volatile static ChargeStationXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static ChargeStationXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (ChargeStationXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new ChargeStationXRocketMQConsumerV2();
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
        super.TOPIC = ChargeStationXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = ChargeStationXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.MINUTE * 30;
    }

    /**
     * 充电桩离线监控任务的处理函数
     * 该任务用于检测指定充电桩的离线状态，并执行相应的操作
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("OfflineMonitorTask")
    public static SyncResult OfflineMonitorTask(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        String name = JsonUtil.getString(message, "name");
        // 减少频率
//        mExecutionThrottle.run(false, data -> {
        LogsUtil.info("充电桩离线监控任务", "(%s)%s - 正在执行任务中...", CSId, name);

        // 执行离线监控任务
        return ChargeStationOfflineMonitorTaskJobV2.execute(CSId);
//            return new SyncResult(0, "");
//        }, String.format("%s:OfflineMonitor:%s", ChargeStationXRocketMQConsumerV2.GROUP_NAME, CSId), ECacheTime.HOUR * 8, null);
//        return new SyncResult(0, "");
    }

    /**
     * 充电桩异常监控任务的处理函数
     * 该任务用于处理充电桩的异常状态
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("AbnormalTask")
    public static SyncResult AbnormalTask(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        String name = JsonUtil.getString(message, "name");

        LogsUtil.info("充电桩异常任务", "(%s)%s - 正在执行任务中...", CSId, name);
        // 执行异常任务
        return ChargeStationAbnormalTaskJobV2.execute(CSId);
    }

    /**
     * 充电桩日数据统计任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("DaySummaryTaskV2")
    public static SyncResult DaySummaryTaskV2(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        String name = JsonUtil.getString(message, "name");
        long start_time = JsonUtil.getLong(message, "start_time");
        long end_time = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("充电桩日数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , CSId
                , name
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
        );

        // 执行日统计任务
        ChargeStationDaySummaryTaskJobV2.execute(CSId, start_time, end_time);
        return new SyncResult(0, "");
    }

    /**
     * 充电桩月数据统计任务的处理函数
     * 该任务用于生成充电桩的月统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("MonthSummaryTaskV2")
    public static SyncResult MonthSummaryTaskV2(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        String name = JsonUtil.getString(message, "name");
        long start_time = JsonUtil.getLong(message, "start_time");
        long end_time = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("充电桩月数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , CSId
                , name
                , TimeUtil.toTimeString(start_time)
                , TimeUtil.toTimeString(end_time)
        );

        // 执行月统计任务
        ChargeStationMonthSummaryTaskJobV2.execute(CSId, start_time, end_time);
        return new SyncResult(0, "");
    }

    /**
     * 充电桩总数据统计任务的处理函数
     * 该任务用于生成充电桩的总统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("SummaryTaskV2")
    public static SyncResult SummaryTaskV2(JSONObject message) {
        String CSId = JsonUtil.getString(message, "CSId");
        String name = JsonUtil.getString(message, "name");

        LogsUtil.info("充电桩总数据统计任务", "(%s)%s - 正在执行任务中...", CSId, name);

        // 执行总统计任务
        ChargeStationSummaryTaskJobV2.execute(CSId);
        return new SyncResult(0, "");
    }
}