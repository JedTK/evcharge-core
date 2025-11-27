package com.evcharge.rocketmq;

/**
 * 消费者消息监听模型类型定义。
 * <p>
 * 用于指定 RocketMQ 消费者在消费消息时采用的处理策略：
 * - 并发消费（Concurrently）：多个线程并发拉取并处理消息，适用于高吞吐、无顺序要求的场景；
 * - 有序消费（Orderly）：确保消息按照队列中的顺序逐条处理，适用于订单、支付等有顺序要求的业务。
 */
public enum XRocketMQConsumerMessageListenerType {
    /**
     * 并发消费模式：
     * - 默认推荐使用；
     * - 消费端可并行处理多个消息；
     * - 不保证消息顺序。
     */
    Concurrently,

    /**
     * 有序消费模式：
     * - 单线程逐条消费某个队列中的消息；
     * - 保证消息顺序性；
     * - 适合对顺序要求严格的业务。
     */
    Orderly;
}
