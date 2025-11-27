package com.evcharge.service.notify;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.notify.NotifyConfigEntity;
import com.evcharge.entity.notify.NotifyLogsEntity;
import com.evcharge.entity.notify.NotifyTemplateEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.libsdk.aliyun.AliyunSmsSDK;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.ExecutionThrottle;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.StringUtil;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AliyunSMSNotify - 阿里云短信通知类。
 * <p>
 * 该类用于通过阿里云短信服务发送通知短信。它会根据通知配置 (NotifyConfig) 和
 * 短信模板 (NotifyTemplate)，以及传入的动态数据，向目标接收者发送短信通知。
 * <p>
 * 功能：
 * - 动态从通知配置中读取短信相关的参数
 * - 支持从全局配置中获取 AccessKeyId 和 AccessKeySecret
 * - 使用阿里云短信 SDK 进行短信发送
 */
public class AliyunSMSNotify {

    /**
     * 创建 AliyunSMSNotify 实例。
     *
     * @return AliyunSMSNotify 的新实例
     */
    public static AliyunSMSNotify getInstance() {
        return new AliyunSMSNotify();
    }

    /**
     * 发送短信 - 根据通知配置、短信模板和传入的数据，通过阿里云短信服务发送短信。
     *
     * @param logsEntity     通知日志
     * @param notifyConfig   通知配置实体，包含了发送通知的配置信息
     * @param notifyTemplate 短信模板实体，定义了短信内容的格式
     * @param transData      透传数据
     * @return SyncResult 发送结果，包含状态码和描述信息
     */
    public SyncResult send(NotifyLogsEntity logsEntity, NotifyConfigEntity notifyConfig, NotifyTemplateEntity notifyTemplate, JSONObject transData) {
        if (notifyConfig.throttling_timeout_ms <= 0) return send(notifyConfig, notifyTemplate, transData);

        SyncResult r = ExecutionThrottle.getInstance().run(data -> send(notifyConfig, notifyTemplate, transData), logsEntity.unique_code, notifyConfig.throttling_timeout_ms, null);
        if (r.code == -1) return new SyncResult(0, "");
        return r;
    }

    /**
     * 发送短信 - 根据通知配置、短信模板和传入的数据，通过阿里云短信服务发送短信。
     *
     * @param notifyConfig   通知配置实体，包含了发送通知的配置信息
     * @param notifyTemplate 短信模板实体，定义了短信内容的格式
     * @param transData      透传数据
     * @return SyncResult 发送结果，包含状态码和描述信息
     */
    private SyncResult send(NotifyConfigEntity notifyConfig, NotifyTemplateEntity notifyTemplate, JSONObject transData) {
        // 检查通知配置是否包含有效的配置信息
        if (!StringUtils.hasLength(notifyConfig.config)) {
            return new SyncResult(2, String.format("%s %s-%s 缺少配置", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 将配置解析为 JSON 对象
        JSONObject config = JSONFormatConfig.format(JSONArray.parse(notifyConfig.config));
        if (config == null) {
            return new SyncResult(2, String.format("%s %s-%s 配置格式错误，无效JSON", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 获取短信接收者列表、签名和模板代码
        String accept_list = notifyConfig.accept_list; // 短信接收者列表
        if (accept_list.equalsIgnoreCase("[auto]") || StringUtil.isEmpty(accept_list)) {
            accept_list = transData.getString("accept_list");
        }

        String sign_name = JsonUtil.getString(config, "sign_name"); // 短信签名
        String template_code = notifyTemplate.template_code; // 短信模板代码
        String content = notifyTemplate.content;// 模板内容，如：${name}发生火灾，请及时赶往现场，地址${address}

        // 获取 AccessKeyId 和 AccessKeySecret 用于阿里云认证
        String AccessKeyId = JsonUtil.getString(config, "AccessKeyId");
        String AccessKeySecret = JsonUtil.getString(config, "AccessKeySecret");

        // region 处理 AccessKeyId 和 AccessKeySecret 的密钥，支持从全局配置获取
        if (!StringUtils.hasLength(AccessKeyId) || !StringUtils.hasLength(AccessKeySecret)) {
            return new SyncResult(2, String.format("%s %s-%s 配置错误，缺少 AccessKeyId 或 AccessKeySecret", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 如果 AccessKeyId 或 AccessKeySecret 使用的是全局变量的格式（如 ${key}），则从全局配置中获取实际的密钥
        if (AccessKeyId.contains("${")) {
            AccessKeyId = SysGlobalConfigEntity.getString(AccessKeyId.replace("${", "").replace("}", ""));
        }
        if (AccessKeySecret.contains("${")) {
            AccessKeySecret = SysGlobalConfigEntity.getString(AccessKeySecret.replace("${", "").replace("}", ""));
        }
        // endregion

        JSONObject template_data = new JSONObject();
        // region 提取模板内容中的占位符并从 transData 获取对应的数据
        // 使用正则表达式匹配模板中的占位符，形式为 ${key}
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)}");
        Matcher matcher = pattern.matcher(content);

        // 遍历所有匹配的占位符
        while (matcher.find()) {
            String key = matcher.group(1); // 获取占位符中的 key，如 name, address
            String value = transData.getString(key); // 从 transData 中获取对应的值
            if (!StringUtils.hasLength(value)) value = "";
            template_data.put(key, value);
        }
        // endregion

        // 使用阿里云短信 SDK 发送短信
        boolean sendResult = AliyunSmsSDK.getInstance(AccessKeyId, AccessKeySecret)
                .send(accept_list, sign_name, template_code, template_data);

        // 返回短信发送的结果，成功返回 0，失败返回 1
        if (sendResult) return new SyncResult(0, "短信发送成功");
        return new SyncResult(1, "短信发送失败");
    }
}