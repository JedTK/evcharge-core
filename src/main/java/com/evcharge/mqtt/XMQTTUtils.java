package com.evcharge.mqtt;

import com.xyzs.utils.StringUtil;

/**
 * XMQTT 辅助类
 */
public class XMQTTUtils {
    /**
     * 格式化 MQTT 主题，以确保它不包含不必要的字符，同时保留 MQTT 通配符。
     *
     * @param topic 原始 MQTT 主题
     * @return 格式化后的 MQTT 主题
     */
    public static String formatTopic(String topic) {
        if (StringUtil.isEmpty(topic)) return "";
        // 替换空格和其他潜在问题字符，同时保留 MQTT 通配符 '+' 和 '#'
        // 将空格替换为下划线
        // 将.替换为下划线
        // 删除除字母、数字、下划线、斜杠以及 MQTT 通配符之外的所有字符
        return topic.trim()
                .replace(" ", "_") // 将空格替换为下划线
                .replace(".", "_") // 将.替换为下划线
                .replaceAll("[^a-zA-Z0-9_/#+$]", "");
    }
}
