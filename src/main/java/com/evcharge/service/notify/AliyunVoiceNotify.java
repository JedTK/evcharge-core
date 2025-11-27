package com.evcharge.service.notify;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.notify.NotifyConfigEntity;
import com.evcharge.entity.notify.NotifyLogsEntity;
import com.evcharge.entity.notify.NotifyTemplateEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.libsdk.aliyun.AliyunVoiceSDK;
import com.evcharge.utils.JSONFormatConfig;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.ExecutionThrottle;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.StringUtil;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AliyunVoiceNotify - 阿里云语音通知类。
 * <p>
 * 该类用于通过阿里云的语音通知服务发送语音呼叫通知。它主要负责从配置文件中获取
 * 阿里云的 AccessKeyId 和 AccessKeySecret，并根据提供的模板和数据发送语音呼叫。
 */
public class AliyunVoiceNotify {

    /**
     * 获取 AliyunVoiceNotify 的实例。
     *
     * @return AliyunVoiceNotify 实例
     */
    public static AliyunVoiceNotify getInstance() {
        return new AliyunVoiceNotify();
    }

    /**
     * 发送语音通知。
     * <p>
     * 根据传入的通知日志、通知配置、模板以及透传的数据，使用阿里云语音通知服务
     * 进行呼叫。如果配置了节流时间，会在一定时间内限制多次调用。
     *
     * @param logsEntity     通知日志实体，记录通知发送的详细日志
     * @param notifyConfig   通知配置实体，包含通知所需的配置，如接收者号码、AccessKey 信息等
     * @param notifyTemplate 通知模板实体，定义了语音通知使用的模板代码
     * @param transData      透传数据，动态数据会传入模板中进行替换
     * @return SyncResult 返回一个 SyncResult 对象，包含发送状态码和描述信息
     */
    public SyncResult send(NotifyLogsEntity logsEntity, NotifyConfigEntity notifyConfig, NotifyTemplateEntity notifyTemplate, JSONObject transData) {
        // 如果未设置节流超时时间，则直接发送语音通知
        if (notifyConfig.throttling_timeout_ms <= 0) {
            return send(notifyConfig, notifyTemplate, transData);
        }

        // 如果设置了节流时间，则使用 ExecutionThrottle 进行控制，避免多次重复调用
        SyncResult r = ExecutionThrottle.getInstance().run(data -> send(notifyConfig, notifyTemplate, transData), logsEntity.unique_code, notifyConfig.throttling_timeout_ms, null);
        if (r.code == -1) {
            return new SyncResult(0, ""); // 如果调用受限，返回默认成功结果
        }
        return r;
    }

    /**
     * 发送语音通知（内部调用）。
     * <p>
     * 根据通知配置、模板和透传的数据，直接通过阿里云语音通知服务进行呼叫。
     * 该方法主要负责处理 AccessKeyId 和 AccessKeySecret 的解析，以及调用阿里云服务。
     *
     * @param notifyConfig   通知配置实体，包含了发送语音通知的配置信息
     * @param notifyTemplate 通知模板实体，定义了语音通知的模板代码
     * @param transData      透传数据，模板中的占位符会通过此数据进行替换
     * @return SyncResult 返回一个 SyncResult 对象，包含发送状态码和描述信息
     */
    private SyncResult send(NotifyConfigEntity notifyConfig, NotifyTemplateEntity notifyTemplate, JSONObject transData) {
        // 检查通知配置是否包含有效的配置信息
        if (!StringUtils.hasLength(notifyConfig.config)) {
            return new SyncResult(2, String.format("%s %s-%s 缺少配置", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 将配置字符串解析为 JSON 对象
        JSONObject config = JSONFormatConfig.format(JSONArray.parse(notifyConfig.config));
        if (config == null) {
            return new SyncResult(2, String.format("%s %s-%s 配置格式错误，无效JSON", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 获取接收语音通知的号码列表和模板代码
        String accept_list = notifyConfig.accept_list; // 通知接收者的手机号列表
        if (accept_list.equalsIgnoreCase("[auto]") || StringUtil.isEmpty(accept_list)) {
            accept_list = transData.getString("accept_list");
        }

        String template_code = notifyTemplate.template_code; // 阿里云语音通知的模板代码
        String content = notifyTemplate.content; // 模板内容，如：${name}发生火灾，请及时赶往现场，地址${address}

        // 获取阿里云的 AccessKeyId 和 AccessKeySecret，用于认证
        String AccessKeyId = JsonUtil.getString(config, "AccessKeyId");
        String AccessKeySecret = JsonUtil.getString(config, "AccessKeySecret");

        // region 处理 AccessKeyId 和 AccessKeySecret 的密钥，支持从全局配置获取
        if (!StringUtils.hasLength(AccessKeyId) || !StringUtils.hasLength(AccessKeySecret)) {
            return new SyncResult(2, String.format("%s %s-%s 配置错误，缺少 AccessKeyId 或 AccessKeySecret", notifyConfig.title, notifyConfig.config_code, notifyConfig.method_code));
        }

        // 如果 AccessKeyId 或 AccessKeySecret 包含 ${} 的占位符，从全局配置中获取实际值
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

        // 分割接收者列表为单个号码
        String[] phone_list = accept_list.split(",");
        SyncResult sendResult = new SyncResult(1, ""); // 初始化默认发送结果
        for (String phone_number : phone_list) {
            if (!StringUtils.hasLength(phone_number)) continue; // 如果号码为空，跳过
            // 通过阿里云语音 SDK 调用阿里云语音通知服务进行拨号
            sendResult = AliyunVoiceSDK.getInstance()
                    .setAccessKeyId(AccessKeyId)
                    .setAccessKeySecret(AccessKeySecret)
                    .singleCall(phone_number, template_code, template_data);
//            if (sendResult.code != 0) break; // 如果发生错误，则停止继续拨号
        }

        // 检查最后的拨号结果并返回相应的状态信息
        if (sendResult.code == 0) {
            return new SyncResult(0, "呼叫成功");
        }
        return new SyncResult(1, "呼叫失败");
    }
}