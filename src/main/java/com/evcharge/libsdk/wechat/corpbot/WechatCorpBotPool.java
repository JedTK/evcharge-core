package com.evcharge.libsdk.wechat.corpbot;

import com.xyzs.entity.SyncResult;

import java.util.List;

/**
 * 企业微信机器人池类，用于管理和发送消息到多个企业微信机器人。
 * <p>
 * 该类继承自 WechatCorpBot，通过维护一个 Webhook URL 队列，支持在多个机器人之间循环发送消息。
 * 当一个 Webhook URL 发送消息失败时，该类会自动切换到下一个 URL 继续尝试，直到消息发送成功或队列中的所有 URL 都尝试过。
 * <p>
 * 文档参考：
 * <a href="https://developer.work.weixin.qq.com/document/path/91770#%E5%A6%82%E4%BD%95%E4%BD%BF%E7%94%A8%E7%BE%A4%E6%9C%BA%E5%99%A8%E4%BA%BA">企业微信机器人文档</a>
 */
public class WechatCorpBotPool extends WechatCorpBot {

    /**
     * 存储机器人 Webhook URL 的队列，用于循环尝试发送消息。
     * Webhook URLs 是企业微信机器人的接收端点，消息将被发送到这些端点。
     * <p>
     * 在实际应用中，WebhookUrlQueue 通常由外部配置提供，可以包含一个或多个 Webhook URL，
     * 这些 URL 分别对应不同的企业微信机器人。消息发送时，类会从队列中依次取出 URL 尝试发送，
     * 直到发送成功或遍历完所有 URL。
     */
    private final List<String> WebhookUrlQueue;

    /**
     * 构造函数，用于初始化企业微信机器人池。
     *
     * @param WebhookUrlQueue 包含多个 Webhook URL 的队列。
     *                        该队列用于存储多个企业微信机器人的 Webhook URL，这些 URL 将在消息发送时被依次尝试。
     *                        使用该队列的主要目的是确保在某些 Webhook URL 无法使用时，依然能够尝试其他 URL 来发送消息，提高消息发送的成功率。
     */
    public WechatCorpBotPool(List<String> WebhookUrlQueue) {
        this.WebhookUrlQueue = WebhookUrlQueue;
    }

    @Override
    public SyncResult send(String messageJSONBody) {
        // 默认返回值，表示未发送成功
        SyncResult r = new SyncResult(1, "");
        // 遍历 Webhook URL 队列
        for (String url : WebhookUrlQueue) {
            // 设置当前 Webhook URL
            setWebhookUrl(url);
            // 尝试发送消息，使用父类的 send 方法
            r = super.send(messageJSONBody);
            // 如果发送成功，直接返回结果
            if (r.code == 0) return r;
        }
        // 表示池中的资源已经消耗完毕
        // r.code == 45009 表示消息被限制，需要1分钟后再可以重新发送
        // 这个时候应该加入到延迟发送消息队列中
        return r;
    }
}