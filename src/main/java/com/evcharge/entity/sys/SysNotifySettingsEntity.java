package com.evcharge.entity.sys;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBot;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBotPool;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 系统通知设置实体类，用于存储和管理系统通知的配置信息。
 *
 * <p>此类继承自 BaseEntity，并实现了 Serializable 接口，
 * 其主要功能包括根据组织代码、通知类型、通知方式获取相应的配置，
 * 并获取与企业微信机器人的相关配置。</p>
 *
 * <p>作者: JED</p>
 * <p>日期: 2024-6-17</p>
 */
public class SysNotifySettingsEntity extends BaseEntity implements Serializable {
    // region -- 实体类属性 --
    /**
     * 唯一标识符
     */
    public long id;

    /**
     * 通知类型代码
     */
    public String notifyTypeCode;

    /**
     * 通知类型描述，例如“设备离线通知”或“火警监控通知”
     */
    public String notifyTypeText;

    /**
     * 通知方式，如微信、短信、邮件等
     */
    public String method;

    /**
     * 通知配置信息，通常为 JSON 格式的字符串
     */
    public String config;

    /**
     * 组织代码，用于标识所属的组织
     */
    public String organize_code;

    /**
     * 创建时间戳，记录实体的创建时间
     */
    public long create_time;

    /**
     * 更新时间戳，记录实体的最后更新时间
     */
    public long update_time;
    // endregion

    /**
     * 获取 SysNotifySettingsEntity 的实例
     *
     * @return SysNotifySettingsEntity 实例
     */
    public static SysNotifySettingsEntity getInstance() {
        return new SysNotifySettingsEntity();
    }

    /**
     * 根据组织代码、通知类型代码和通知方式获取相应的配置信息。
     *
     * <p>此方法使用了缓存机制，通过缓存的键值来快速获取配置数据。</p>
     *
     * @param organize_code  组织代码
     * @param notifyTypeCode 通知类型代码
     * @param method         通知方式
     * @return 对应的配置信息，类型为 JSONObject
     */
    public JSONObject getConfigWithOrganize(String organize_code
            , String notifyTypeCode
            , String method) {
        return getConfigWithOrganize(organize_code, notifyTypeCode, method, null);
    }

    /**
     * 根据组织代码、通知类型代码和通知方式获取相应的配置信息，并提供默认值。
     *
     * <p>如果没有找到匹配的配置信息，将返回指定的默认值。</p>
     *
     * @param organize_code  组织代码
     * @param notifyTypeCode 通知类型代码
     * @param method         通知方式
     * @param defaultValue   如果未找到配置时返回的默认值
     * @return 对应的配置信息，类型为 JSONObject
     */
    public JSONObject getConfigWithOrganize(String organize_code
            , String notifyTypeCode
            , String method
            , JSONObject defaultValue) {
        try {
            // 从数据库中查找配置数据
            Map<String, Object> data = this
                    .field("id,notifyTypeCode,notifyTypeText,method,config,organize_code")
                    .cache(String.format("BaseData:SysNotifySettingsConfig:%s:%s:%s", organize_code, notifyTypeCode, method))
                    .where("organize_code", organize_code)
                    .where("notifyTypeCode", notifyTypeCode)
                    .where("method", method)
                    .find();
            if (data == null || data.isEmpty()) return defaultValue;

            // 解析 config 字段为 JSONObject 或 JSONArray
            JSONObject config = null;
            String configString = MapUtil.getString(data, "config").trim();
            if (!StringUtils.hasLength(configString)) return defaultValue;
            if (configString.startsWith("{")) {
                config = JSONObject.parseObject(configString);
            } else if (configString.startsWith("[")) {
                JSONArray configArray = JSONArray.parseArray(configString);
                config = JSONFormatConfig.format(configArray);
            }
            return config;
        } catch (Exception ignored) {
            // 忽略异常，返回默认值
        }
        return defaultValue;
    }

    /**
     * 获取企业微信机器人池配置。
     *
     * <p>此方法会根据组织代码和通知类型代码，从配置信息中获取多个企业微信机器人的 Webhook URL，
     * 并生成对应的 WechatCorpBotPool 实例。</p>
     *
     * @param organize_code  组织代码
     * @param notifyTypeCode 通知类型代码
     * @return WechatCorpBotPool 实例，如果未找到有效配置则返回 null
     */
    public WechatCorpBotPool getWechatCorpBotPool(String organize_code, String notifyTypeCode) {
        JSONObject config = SysNotifySettingsEntity.getInstance()
                .getConfigWithOrganize(organize_code, notifyTypeCode, "WechatCorpBot", null);
        if (config == null) {
            LogsUtil.warn(this.getClass().getSimpleName()
                    , "[%s] - 无法获取[%s]企业微信机器人配置"
                    , organize_code
                    , notifyTypeCode
            );
            return null;
        }
        List<String> WebhookUrlQueue = new LinkedList<>();
        // 遍历配置，收集所有有效的 Webhook URL
        for (Object obj : JsonUtil.getJSONArray(config, "WebhookUrl")) {
            WebhookUrlQueue.add(String.format("%s", obj));
        }
        if (WebhookUrlQueue.isEmpty()) return null;
        return new WechatCorpBotPool(WebhookUrlQueue);
    }


    /**
     * 获取企业微信机器人配置。
     *
     * <p>此方法会根据组织代码和通知类型代码，从配置信息中获取企业微信机器人的 Webhook URL，
     * 并生成对应的 WechatCorpBot 实例。</p>
     *
     * @param organize_code  组织代码
     * @param notifyTypeCode 通知类型代码
     * @return WechatCorpBot 实例，如果未找到配置则返回 null
     */
    @Deprecated
    public WechatCorpBot getWechatCorpBot(String organize_code, String notifyTypeCode) {
        return getWechatCorpBot(organize_code, notifyTypeCode, null);
    }

    /**
     * 获取企业微信机器人配置，并提供默认值。
     *
     * <p>此方法会根据组织代码和通知类型代码，从配置信息中获取企业微信机器人的 Webhook URL，
     * 并生成对应的 WechatCorpBot 实例。如果未找到配置，将返回指定的默认值。</p>
     *
     * @param organize_code  组织代码
     * @param notifyTypeCode 通知类型代码
     * @param defaultValue   如果未找到配置时返回的默认 WechatCorpBot 实例
     * @return WechatCorpBot 实例
     */
    @Deprecated
    public WechatCorpBot getWechatCorpBot(String organize_code, String notifyTypeCode, WechatCorpBot defaultValue) {
        // 获取企业微信机器人配置
        JSONObject config = SysNotifySettingsEntity.getInstance()
                .getConfigWithOrganize(organize_code, notifyTypeCode, "WechatCorpBot", null);
        if (config == null) {
            LogsUtil.warn(this.getClass().getSimpleName()
                    , "[%s] - 无法获取[%s]企业微信机器人配置"
                    , organize_code
                    , notifyTypeCode
            );
            return defaultValue;
        }
        String WebhookUrl = "";
        // 遍历配置，寻找有效的 Webhook URL
        for (String key : config.keySet()) {
            WebhookUrl = JsonUtil.getString(config, key);
            if (StringUtils.hasLength(WebhookUrl)) {
                if (WechatCorpBot.isValidWebhookUrl(WebhookUrl)) break;
            }
        }
        if (!StringUtils.hasLength(WebhookUrl)) return defaultValue;
        return new WechatCorpBot(WebhookUrl);
    }
}