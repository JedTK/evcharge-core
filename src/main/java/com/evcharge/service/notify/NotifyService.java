package com.evcharge.service.notify;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.entity.notify.*;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.ENotifyType;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知辅助类 - 该类主要用于处理系统中的通知发送逻辑，支持异步和同步两种通知发送方式。
 * <p>
 * 功能：
 * - 支持通知的异步和同步发送
 * - 支持站点、设备和系统级别的通知配置，优先考虑站点和设备的通知设置
 * - 处理通知的互斥配置，若某些通知互斥则只会发送站点或设备通知，忽略系统通知
 * - 支持不同通知方式（如微信企业机器人、阿里云短信、电话通知等）的发送
 * - 提供通知重发机制，处理发送失败的情况并进行重试
 */
public class NotifyService {
    private final static String TAG = NotifyService.class.getSimpleName(); // 日志标识

    /**
     * 获取 NotifyBuilder 实例
     *
     * @return NotifyBuilder 的实例对象
     */
    public static NotifyService getInstance() {
        return new NotifyService();
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code 唯一标识符（如站点编码、设备序列号）：CS_{站点id}、DE_{设备ID}、DG_{通用设备ID}
     * @param config_code 通知配置编码
     * @param transData   传输的数据，JSON 格式
     */
    public void asyncPush(String unique_code, String config_code, JSONObject transData) {
        asyncPush(unique_code, config_code, ENotifyType.NONE, transData, null, null);
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code       唯一标识符（如站点编码、设备序列号）：CS_{站点id}、DE_{设备ID}、DG_{通用设备ID}
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     * @param throttleKey       (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     */
    public void asyncPush(String unique_code, String config_code, JSONObject transData, ITransDataBuilder iTransDataBuilder, String throttleKey) {
        asyncPush(unique_code, config_code, ENotifyType.NONE, transData, iTransDataBuilder, null, throttleKey);
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code       唯一标识符（如站点编码、设备序列号）：CS_{站点id}、DE_{设备ID}、DG_{通用设备ID}
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     * @param iAsyncListener    回调监听器，用于处理通知发送结果
     * @param throttleKey       (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     */
    public void asyncPush(String unique_code, String config_code, JSONObject transData, ITransDataBuilder iTransDataBuilder, IAsyncListener iAsyncListener, String throttleKey) {
        asyncPush(unique_code, config_code, ENotifyType.NONE, transData, iTransDataBuilder, iAsyncListener, throttleKey);
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code 唯一标识符（如站点编码、设备序列号）
     * @param config_code 通知配置编码
     * @param transData   传输的数据，JSON 格式
     */
    public void asyncPush(String unique_code, String config_code, ENotifyType notifyType, JSONObject transData) {
        asyncPush(unique_code, config_code, notifyType, transData, null, null);
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code 唯一标识符（如站点编码、设备序列号）
     * @param config_code 通知配置编码
     * @param transData   传输的数据，JSON 格式
     * @param throttleKey (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     */
    public void asyncPush(String unique_code, String config_code, ENotifyType notifyType, JSONObject transData, String throttleKey) {
        asyncPush(unique_code, config_code, notifyType, transData, null, null, throttleKey);
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code       唯一标识符（如站点编码、设备序列号）
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     */
    public void asyncPush(String unique_code, String config_code, ENotifyType notifyType, JSONObject transData, ITransDataBuilder iTransDataBuilder) {
        asyncPush(unique_code, config_code, notifyType, transData, iTransDataBuilder, null, "");
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code       唯一标识符（如站点编码、设备序列号）
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     */
    public void asyncPush(String unique_code
            , String config_code
            , ENotifyType notifyType
            , JSONObject transData
            , ITransDataBuilder iTransDataBuilder
            , IAsyncListener iAsyncListener) {
        asyncPush(unique_code, config_code, notifyType, transData, iTransDataBuilder, iAsyncListener, "");
    }

    /**
     * 异步发送通知 - 推荐使用异步发送方式，以减少主线程阻塞。
     *
     * @param unique_code       唯一标识符（如站点编码、设备序列号）
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     * @param iAsyncListener    回调监听器，用于处理通知发送结果
     * @param throttleKey       (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     */
    public void asyncPush(String unique_code
            , String config_code
            , ENotifyType notifyType
            , JSONObject transData
            , ITransDataBuilder iTransDataBuilder
            , IAsyncListener iAsyncListener
            , String throttleKey
    ) {
        // 使用线程池异步执行通知发送逻辑，避免阻塞主线程
        ThreadPoolManager.getInstance().execute("", () -> {
            SyncResult r = syncPush(unique_code, config_code, notifyType, transData, iTransDataBuilder, throttleKey); // 同步发送通知
            if (iAsyncListener != null) iAsyncListener.onResult(r.code, r.data); // 通知发送结果回调
        });
    }

    /**
     * 同步发送通知 - 不推荐使用同步发送方式，因为同步操作会阻塞调用线程。
     *
     * @param unique_code       站点编码、设备序列号等唯一标识符
     * @param config_code       通知配置编码
     * @param transData         传输的数据，JSON 格式
     * @param iTransDataBuilder 传输的数据重组监听器
     * @param throttleKey       (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     * @return 通知发送结果，SyncResult 对象，包含返回码和信息
     */
    private SyncResult syncPush(String unique_code
            , String config_code
            , ENotifyType notifyType
            , JSONObject transData
            , ITransDataBuilder iTransDataBuilder
            , String throttleKey
    ) {
        try {
            // 通过 unique_code 查询是否存在忽略通知的设置
            if (NotifyIgnoreEntity.getInstance().ignore(unique_code, config_code)) {
                return new SyncResult(1, "忽略本通知");
            }

            // 先通过iTransDataBuilder重新组合transData
            if (transData == null) transData = new JSONObject();
            if (iTransDataBuilder != null) {
                JSONObject newTransData = iTransDataBuilder.build(unique_code, config_code, notifyType, transData);
                if (newTransData != null) {
                    // 合并newTransData到transData
                    transData.putAll(newTransData);
                }
            }

            // 通过透传参数获得组织代码
            String organize_code = JsonUtil.getString(transData, "organize_code");

            // 查询是否存在与 unique_code 相关的通知配置，检查是否有互斥通知
            List<NotifyConfigEntity> uniqueConfigList = NotifyMappingEntity.getInstance().getList(unique_code, config_code, notifyType);
            if (uniqueConfigList != null && !uniqueConfigList.isEmpty()) {
                // 遍历通知配置列表，并依次发送通知
                SyncResult queueResult = addSendQueue(unique_code, transData, uniqueConfigList, throttleKey);
                if (queueResult.code == 0) {
                    boolean is_mutex = MapUtil.getBool((Map<String, Object>) queueResult.data, "is_mutex", false);
                    // 如果存在互斥配置，直接返回，不再发送系统通知
                    if (is_mutex) return new SyncResult(0, "");
                }
            }

            // 通过 config_code 查询是否存在系统级别的通知配置
            List<NotifyConfigEntity> sysConfigList = NotifyConfigEntity.getInstance().getList(config_code, organize_code, notifyType);
            if (sysConfigList != null && !sysConfigList.isEmpty()) {
                // 遍历系统通知配置列表，并依次发送通知
                addSendQueue(unique_code, transData, sysConfigList, throttleKey);
            }
        } catch (Exception e) {
            // 捕获异常并记录日志
            LogsUtil.error(e, TAG, "发送通知发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 加入发送队列 - 一般用于发送多种通知方式的情况下进行批量通知的发送。
     *
     * @param unique_code 唯一标识符
     * @param transData   传输的数据，JSON 格式
     * @param list        通知配置列表
     * @param throttleKey (可选)自定义限制发送key，此key存在值将不以unique_code为缓存来限制发送频率
     * @return 通知发送结果，SyncResult 对象
     */
    public SyncResult addSendQueue(String unique_code, JSONObject transData, List<NotifyConfigEntity> list, String throttleKey) {
        Map<String, Object> cb = new LinkedHashMap<>();
        for (NotifyConfigEntity notifyConfig : list) {
            // 检查是否有互斥设置，如果有则标记为互斥,只要有一条互斥就不会往下执行
            if (notifyConfig.is_mutex == 1) cb.put("is_mutex", true);

            // 查询通知模板，如果模板不存在，记录日志并跳过
            if (!StringUtils.hasLength(notifyConfig.template_code)) {
                LogsUtil.warn(TAG, String.format("%s-%s 通知配置有误，缺少通知模板", notifyConfig.config_code, notifyConfig.method_code));
                continue;
            }
            NotifyTemplateEntity notifyTemplate = NotifyTemplateEntity.getInstance().getWithCode(notifyConfig.template_code);
            if (notifyTemplate == null || notifyTemplate.id == 0) {
                LogsUtil.warn(TAG, String.format("%s-%s 通知配置有误，无法找到[%s]通知模板", notifyConfig.config_code, notifyConfig.method_code, notifyConfig.template_code));
                continue;
            }

            if (StringUtil.isEmpty(throttleKey)) throttleKey = String.format("Notify:Throttle:%s:Send", unique_code);

            // 新增通知日志记录
            NotifyLogsEntity logsEntity = new NotifyLogsEntity();
            logsEntity.unique_code = unique_code;
            logsEntity.config_code = notifyConfig.config_code;
            logsEntity.method_code = notifyConfig.method_code;
            logsEntity.template_code = notifyConfig.template_code;
            logsEntity.status = 0; // 设置状态为待发送
            logsEntity.accept_list = notifyConfig.accept_list;
            logsEntity.retry_max_count = notifyConfig.retry_max_count;
            logsEntity.retry_timeout_ms = notifyConfig.retry_timeout_ms;
            logsEntity.throttling_timeout_ms = notifyConfig.throttling_timeout_ms;
            logsEntity.data = transData.toJSONString();
            logsEntity.platform_code = notifyConfig.platform_code;
            logsEntity.organize_code = notifyConfig.organize_code;
            logsEntity = logsEntity.add(); // 保存日志记录

            // 发送通知
            if (notifyConfig.throttling_timeout_ms < 0) push(logsEntity, notifyConfig, notifyTemplate);
            else {
                NotifyLogsEntity finalLogsEntity = logsEntity;
                ExecutionThrottle.getInstance().run(true
                        , json_data -> push(finalLogsEntity, notifyConfig, notifyTemplate)
                        , throttleKey
                        , notifyConfig.throttling_timeout_ms
                        , null);
            }
        }
        return new SyncResult(0, "", cb);
    }

    /**
     * 重发通知 - 根据日志中的消息 ID 重发已存在的通知。
     *
     * @param message_id 通知日志的唯一标识符
     * @return 通知发送结果，SyncResult 对象
     */
    public SyncResult re_push(String message_id) {
        // 如果消息 ID 为空，直接返回错误
        if (!StringUtils.hasLength(message_id)) return new SyncResult(2, "不存在消息记录");

        // 查找对应的通知日志
        NotifyLogsEntity logsEntity = NotifyLogsEntity.getInstance().where("message_id", message_id).findEntity();
        if (logsEntity == null || logsEntity.id == 0) return new SyncResult(2, "不存在消息记录");

        // 查找对应的通知配置和通知模板
        NotifyConfigEntity notifyConfig = NotifyConfigEntity.getInstance().where("config_code", logsEntity.config_code)
                .where("method_code", logsEntity.method_code)
                .where("template_code", logsEntity.template_code)
                .findEntity();
        NotifyTemplateEntity notifyTemplate = NotifyTemplateEntity.getInstance()
                .where("template_code", logsEntity.template_code)
                .findEntity();

        // 发送通知
        return push(logsEntity, notifyConfig, notifyTemplate);
    }

    /**
     * 发送通知 - 实际的通知发送逻辑，根据不同的通知方式（如微信、短信等）发送消息。
     *
     * @param logsEntity     通知日志记录
     * @param notifyConfig   通知配置
     * @param notifyTemplate 通知模板
     * @return 通知发送结果，SyncResult 对象
     */
    private SyncResult push(NotifyLogsEntity logsEntity
            , NotifyConfigEntity notifyConfig
            , NotifyTemplateEntity notifyTemplate) {
        // 如果日志记录为空，返回错误
        if (logsEntity == null || logsEntity.id == 0) return new SyncResult(2, "不存在通知日志");

        // 根据不同的通知方式发送消息
        SyncResult r = new SyncResult(1, "发送失败");
        switch (notifyConfig.method_code) {
            case "WECHATCORPBOT":
                r = WechatCorpBotNotify.getInstance().send(logsEntity, notifyConfig, notifyTemplate, JSONObject.parse(logsEntity.data));
                break;
            case "ALIYUN_SMS":
                r = AliyunSMSNotify.getInstance().send(logsEntity, notifyConfig, notifyTemplate, JSONObject.parse(logsEntity.data));
                break;
            case "ALIYUN_VOICE":
                r = AliyunVoiceNotify.getInstance().send(logsEntity, notifyConfig, notifyTemplate, JSONObject.parse(logsEntity.data));
                break;
        }

        // 更新通知日志状态
        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("update_time", TimeUtil.getTimestamp());

        // 发送请求成功
        if (r.code == 0) {
            set_data.put("status", 2); // 成功发送，状态设置为 2
            logsEntity.where("message_id", logsEntity.message_id).update(set_data);
        }
        // 发送请求失败，根据重试次数进行重试或标记为失败
        else {
            // 添加请求错误日志
            addNotifyErrorLogs(logsEntity.message_id, JSONObject.toJSONString(r));

            if (logsEntity.retry_count < logsEntity.retry_max_count && logsEntity.retry_max_count > 0) {
                set_data.put("status", 3); // 标记为重试
                set_data.put("retry_count", logsEntity.retry_count + 1);
                logsEntity.where("message_id", logsEntity.message_id).update(set_data);
                NotifyServiceDelayedTaskJob.getInstance().add(logsEntity.message_id, logsEntity.retry_timeout_ms); // 加入延迟任务重试
            } else {
                LogsUtil.warn(TAG, "通知消息[%s]已达最大重试次数（%s）", logsEntity.message_id, logsEntity.retry_max_count);
                set_data.put("status", 1); // 标记为失败
                set_data.put("retry_count", logsEntity.retry_count + 1);
                logsEntity.where("message_id", logsEntity.message_id).update(set_data);
            }
        }
        return r;
    }

    /**
     * 添加通知错误日志
     *
     * @param message_id
     * @param content
     */
    private void addNotifyErrorLogs(String message_id, String content) {
        NotifyErrorLogsEntity logsEntity = new NotifyErrorLogsEntity();
        logsEntity.message_id = message_id;
        logsEntity.content = content;
        logsEntity.insert();
    }
}