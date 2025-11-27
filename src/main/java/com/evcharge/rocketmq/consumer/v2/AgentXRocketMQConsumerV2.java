package com.evcharge.rocketmq.consumer.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.rocketmq.XRocketMQBaseConsumer;
import com.evcharge.task.agent.AgentStationDailyIncomeV1TaskJob;
import com.evcharge.task.agent.AgentStationMonthlyIncomeV1TaskJob;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;


public class AgentXRocketMQConsumerV2 extends XRocketMQBaseConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    public String TAG = "代理-充电桩RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_Agent";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "AgentTask";

    private volatile static AgentXRocketMQConsumerV2 instance;

    /**
     * 获取单例对象，确保消费者初始化只执行一次。
     */
    public static AgentXRocketMQConsumerV2 getInstance() {
        if (instance == null) {
            synchronized (AgentXRocketMQConsumerV2.class) {
                if (instance == null) {
                    instance = new AgentXRocketMQConsumerV2();
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
        super.TOPIC = AgentXRocketMQConsumerV2.TOPIC;
        super.GROUP_NAME = AgentXRocketMQConsumerV2.GROUP_NAME;
        this.consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        this.messageModel = MessageModel.CLUSTERING;
        this.MessageExpiredTime = ECacheTime.MINUTE * 30;
    }

    /**
     * 充电桩日数据统计任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("DaySummaryTaskV1")
    public static SyncResult DaySummaryTaskV1(JSONObject message) {
        String organizeCode = JsonUtil.getString(message, "organize_code");
        String CSId = JsonUtil.getString(message, "CSId");
        long startTime = JsonUtil.getLong(message, "start_time");
        long endTime = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("代理-充电桩日消费数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , CSId
//                , name
                , TimeUtil.toTimeString(startTime)
                , TimeUtil.toTimeString(endTime)
        );

        // 执行日统计任务
        AgentStationDailyIncomeV1TaskJob.execute(organizeCode, CSId, startTime, endTime);
//        ChargeStationMonthSummaryTaskJobV2.execute(CSId, start_time, end_time);
//        ChargeStationSummaryTaskJobV2.execute(CSId);
        return new SyncResult(0, "");
    }

    /**
     * 充电桩日数据统计任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("MonthSummaryTaskV1")
    public static SyncResult MonthSummaryTaskV1(JSONObject message) {
        String organizeCode = JsonUtil.getString(message, "organize_code");
        String CSId = JsonUtil.getString(message, "CSId");
        long startTime = JsonUtil.getLong(message, "start_time");
        long endTime = JsonUtil.getLong(message, "end_time");

        LogsUtil.info("代理-充电桩月消费数据统计任务", "(%s)%s - %s ~ %s - 正在执行任务中..."
                , CSId
//                , name
                , TimeUtil.toTimeString(startTime)
                , TimeUtil.toTimeString(endTime)
        );

        // 执行日统计任务
        AgentStationMonthlyIncomeV1TaskJob.execute(organizeCode, CSId, startTime, endTime);
//        ChargeStationMonthSummaryTaskJobV2.execute(CSId, start_time, end_time);
//        ChargeStationSummaryTaskJobV2.execute(CSId);
        return new SyncResult(0, "");
    }


}
