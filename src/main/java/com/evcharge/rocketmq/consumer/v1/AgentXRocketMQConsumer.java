package com.evcharge.rocketmq.consumer.v1;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.annotation.CommandMapping;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.rocketmq.XRocketMQ;
import com.evcharge.task.agent.AgentStationDailyIncomeV1TaskJob;
import com.evcharge.task.agent.AgentStationMonthlyIncomeV1TaskJob;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class AgentXRocketMQConsumer {

    /**
     * 日志标签，用于标识日志信息来源
     */
    private final static String TAG = "代理-充电桩RocketMQ消费者";

    /**
     * 消费者订阅的主题，环境变量控制不同环境的主题配置
     */
    public final static String TOPIC = "{ENV}_Agent";

    /**
     * 消费者所在的消费分组，用于标识消费者组
     */
    private final static String GROUP_NAME = "AgentTask";

    /**
     * 消费标签和处理函数的映射，用于根据消息的标签选择对应的处理函数
     * 使用 ConcurrentHashMap 确保线程安全
     */
    private static final Map<String, BiFunction<JSONObject, Void, SyncResult>> TAGS_ACTION = new ConcurrentHashMap<>();

    /**
     * RocketMQ 实例，不同的消费者实例对应不同的消息消费
     */
    private final static XRocketMQ X_ROCKET_MQ = new XRocketMQ();


    /**
     * 初始化服务，扫描所有带有 @CommandMapping 注解的方法并注册到 TAGS_ACTION 映射中
     * 同时配置并启动 RocketMQ 消费者客户端
     */
    public static void init() {
        // 遍历当前类的所有方法，查找带有 @CommandMapping 注解的方法
        for (Method method : AgentXRocketMQConsumer.class.getDeclaredMethods()) {
            // 检查方法是否带有 @CommandMapping 注解
            if (method.isAnnotationPresent(CommandMapping.class)) {
                CommandMapping command = method.getAnnotation(CommandMapping.class);
                String commandValue = command.value();
                // 将命令值和对应的处理函数存入 TAGS_ACTION 映射
                TAGS_ACTION.put(commandValue, (json, unused) -> {
                    try {
                        // 调用对应的静态方法处理消息，返回处理结果
                        return (SyncResult) method.invoke(null, json);
                    } catch (Exception e) {
                        // 处理错误，记录日志并返回失败结果
                        LogsUtil.error(e, TAG, "执行命令 %s 时出错", commandValue);
                        return new SyncResult(1, "执行失败");
                    }
                });
            }
        }

        // 配置 RocketMQ 消费者客户端并启动
        X_ROCKET_MQ.setEnv(ConfigManager.getString("config.type"))
                .setTAG(TAG)
                .createConsumerClient(
                        SysGlobalConfigEntity.getString("RocketMQ:NameServer")
                        , GROUP_NAME
                        , SysGlobalConfigEntity.getString("RocketMQ:Sub:AccessKey")
                        , SysGlobalConfigEntity.getString("RocketMQ:Sub:SecretKey")
                )
                .registerMessageListener(messageListener)
                .subscribe(TOPIC, TAGS_ACTION.keySet().toArray(new String[0]));
        // 设置线程最大使用量
        X_ROCKET_MQ.getConsumerClient().setConsumeThreadMax(20);
        // 设置现场最小使用量
        X_ROCKET_MQ.getConsumerClient().setConsumeThreadMin(1);
        // 设置最大恢复次数，默认值：16
        X_ROCKET_MQ.getConsumerClient().setMaxReconsumeTimes(3);
        // 消息拉取间隔，默认值：0（单位：毫秒）
        X_ROCKET_MQ.getConsumerClient().setPullInterval(500);
        // 单次最大拉取消息数量,默认值：32
        X_ROCKET_MQ.getConsumerClient().setPullBatchSize(10);
        // 消费者本地缓存消息数量,默认值：1
//        X_ROCKET_MQ.getConsumerClient().setConsumeMessageBatchMaxSize(1);
        // 设置消息的消费超时时间，默认值：15分钟
        X_ROCKET_MQ.getConsumerClient().setConsumeTimeout(5);
        // 费者负载均衡的间隔时间：
//        X_ROCKET_MQ.getConsumerClient().setAdjustThreadPoolNumsThreshold(1000);

        // 开启消费者
        X_ROCKET_MQ.startConsumerClient();
    }

    /**
     * RocketMQ 消费者的消息处理机制
     * 该方法会在消费者接收到消息时调用，处理具体的消息内容
     */
    private static final MessageListenerConcurrently messageListener = (list, context) -> {
        try {
//            LogsUtil.info(X_ROCKET_MQ.TAG, "[订阅消息] - 收到 %s 条消费消息，主题：%s/%s "
//                    , list.size()
//                    , GROUP_NAME
//                    , context.getMessageQueue().getTopic());

            for (MessageExt messageExt : list) {
                // 检查消息是否过期，避免处理无效的过期消息
                long create_time = Convert.toLong(messageExt.getUserProperty("create_time"));
                if (create_time != 0 && create_time + ECacheTime.MINUTE * 30 <= TimeUtil.getTimestamp()) {
                    LogsUtil.warn(TAG, "[消息过期] - %s/%s/%s 创建时间：%s"
                            , GROUP_NAME
                            , messageExt.getTopic()
                            , messageExt.getTags()
                            , TimeUtil.toTimeString(create_time));
                    continue;
                }

                // 如果消息已被消费多次，跳过处理（避免重复处理）
                if (messageExt.getReconsumeTimes() > 1) continue;

                // 获取消息体并转换为 JSON 对象
                String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                LogsUtil.info(TAG, "[订阅消息] - /%s/%s/%s %s"
                        , GROUP_NAME
                        , messageExt.getTopic()
                        , messageExt.getTags()
                        , body);

                // 如果消息体为空，跳过该消息
                if (!StringUtil.hasLength(body)) continue;
                JSONObject message = JSONObject.parse(body);
                if (message == null) continue;

                // 获取对应标签的处理函数并执行
                BiFunction<JSONObject, Void, SyncResult> function = TAGS_ACTION.get(messageExt.getTags());
                if (function == null) {
                    LogsUtil.info(X_ROCKET_MQ.TAG, "[订阅消息] - 无效主题 - %s/%s/%s %s"
                            , GROUP_NAME
                            , messageExt.getTopic()
                            , messageExt.getTags()
                            , body);
                    continue;
                }

                // 执行对应的处理函数并处理结果
                SyncResult result = function.apply(message, null);
                if (result.code != 0) {
                    LogsUtil.warn(TAG, result.msg);
                }
            }
        } catch (Exception e) {
            // 捕获异常并记录日志
            LogsUtil.error(e, TAG, "处理消费消息发生错误");
        }
        // 返回消息处理成功的状态
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    };


    /**
     * 充电桩日数据统计任务的处理函数
     * 该任务用于生成充电桩的日统计数据
     *
     * @param message 消息内容，包含充电桩 ID 和名称
     * @return 任务执行结果
     */
    @CommandMapping("DaySummaryTaskV1")
    private static SyncResult DaySummaryTaskV1(JSONObject message) {
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
        AgentStationDailyIncomeV1TaskJob.execute(organizeCode,CSId, startTime, endTime);
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
    private static SyncResult MonthSummaryTaskV1(JSONObject message) {
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
        AgentStationMonthlyIncomeV1TaskJob.execute(organizeCode,CSId, startTime, endTime);
//        ChargeStationMonthSummaryTaskJobV2.execute(CSId, start_time, end_time);
//        ChargeStationSummaryTaskJobV2.execute(CSId);
        return new SyncResult(0, "");
    }



}
