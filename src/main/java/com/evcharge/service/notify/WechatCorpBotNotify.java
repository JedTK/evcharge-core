package com.evcharge.service.notify;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.notify.NotifyConfigEntity;
import com.evcharge.entity.notify.NotifyLogsEntity;
import com.evcharge.entity.notify.NotifyTemplateEntity;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBotPool;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * WechatCorpBotNotify - 企业微信机器人通知类。
 * <p>
 * 该类用于通过企业微信的机器人接口发送通知消息。通过分析通知配置和模板内容，
 * 动态生成并发送消息。支持通过不同的 Webhook URL 向多个企业微信机器人发送通知。
 * <p>
 * 功能：
 * - 根据 NotifyConfig 和 NotifyTemplate 动态生成消息
 * - 处理 Webhook URL 配置，支持多个机器人发送
 * - 支持企业微信机器人发送失败后的频率超限处理
 */
public class WechatCorpBotNotify {

    /**
     * 创建 WechatCorpBotNotify 实例。
     *
     * @return WechatCorpBotNotify 的新实例
     */
    public static WechatCorpBotNotify getInstance() {
        return new WechatCorpBotNotify();
    }

    /**
     * 发送消息 - 根据通知配置、模板和数据，通过企业微信机器人发送消息。
     *
     * @param notifyConfig   通知配置实体，包含了发送通知的配置信息
     * @param notifyTemplate 通知模板实体，定义了通知消息的内容格式
     * @param data           发送通知时附加的数据，JSON 格式
     * @return SyncResult 发送结果，包含状态码和描述信息
     */
    public SyncResult send(NotifyLogsEntity logsEntity, NotifyConfigEntity notifyConfig, NotifyTemplateEntity notifyTemplate, JSONObject data) {
        // 检查通知配置是否包含有效的配置内容
        if (!StringUtils.hasLength(notifyConfig.config)) {
            return new SyncResult(2, String.format("%s %s-%s 缺少配置", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 将配置解析为 JSON 对象
        JSONObject config = JSONFormatConfig.format(JSONArray.parse(notifyConfig.config));
        if (config == null) {
            return new SyncResult(2, String.format("%s %s-%s 配置格式错误，无效JSON", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 使用通知模板的内容，动态替换模板中的变量
        String message = notifyTemplate.content;
        for (String key : data.keySet()) {
            // 将模板中的占位符替换为实际数据
            message = message.replace(String.format("${%s}", key), MapUtil.getString(data, key));
        }

        // region 处理配置，获得有效的 Webhook URL 集合
        List<String> WebhookUrlQueue = new LinkedList<>();
        // 遍历 JSON 配置中的 Webhook URL 列表，并将其加入队列
        for (Object obj : JsonUtil.getJSONArray(config, "WebhookUrl")) {
            WebhookUrlQueue.add(String.format("%s", obj));
        }
        // 如果没有有效的 Webhook URL，则返回错误
        if (WebhookUrlQueue.isEmpty()) {
            return new SyncResult(2, String.format("%s %s-%s 配置错误，无效的Webhook URL集合", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }
        // endregion

        // 创建 WechatCorpBotPool 实例，负责发送消息到指定的 Webhook URL
        WechatCorpBotPool botPool = new WechatCorpBotPool(WebhookUrlQueue);
        SyncResult r = botPool.send(message); // 发送消息

        // 如果返回的错误代码是 45009，表示消息发送频率超限，需要延迟发送
        if (r.code == 45009) {
            return new SyncResult(11, "消息延迟发送"); // 消息延迟由上级处理逻辑
        }
        return new SyncResult(0, ""); // 成功发送消息
    }
}