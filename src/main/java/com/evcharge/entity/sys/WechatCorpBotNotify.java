package com.evcharge.entity.sys;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBotPool;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.ConfigManager;
import com.xyzs.utils.LogsUtil;
import org.springframework.util.StringUtils;

/**
 * WechatCorpBotNotify 类
 * <p>
 * 该类用于封装通过企业微信机器人发送通知的逻辑，提供了一套简洁的接口，用于设置组织代码、通知类型和消息内容，
 * 并通过企业微信机器人发送这些消息。通过此类，开发者可以简化与企业微信机器人的交互。
 *
 * <p>
 * 主要功能：
 * <ul>
 *   <li>封装消息的发送逻辑</li>
 *   <li>处理发送失败的情况（例如消息发送频率超限）</li>
 *   <li>支持消息的延迟发送</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用方法：
 * <pre>
 * WechatCorpBotNotify notify = WechatCorpBotNotify.getInstance()
 *     .setOrganize_code("orgCode")
 *     .setNotify_type("type")
 *     .setMessage(new JSONObject());
 *
 * SyncResult result = notify.send();
 * if (result.code == 0) {
 *     // 发送成功
 * } else {
 *     // 处理失败情况
 * }
 * </pre>
 * </p>
 *
 * <p>
 * 返回代码说明：
 * <ul>
 *   <li>0: 成功发送（或消息已加入延迟发送队列）</li>
 *   <li>1: 发送失败，无法获取通知配置或延迟发送任务添加失败</li>
 *   <li>2: 参数缺失（组织代码、通知类型或消息内容）</li>
 * </ul>
 * </p>
 *
 * @see WechatCorpBotPool
 * @see SysNotifySettingsEntity
 * @see WechatCorpBotNotifyDelayedMessageTaskJob
 */
@Deprecated
public class WechatCorpBotNotify {

    //region 属性
    /**
     * 组织代码，用于标识哪个组织发送消息。
     * 该字段必须设置，否则无法发送消息。
     */
    public String organize_code;

    /**
     * 通知类型，用于标识消息的类别。
     * 该字段必须设置，否则无法发送消息。
     */
    public String notify_type;

    /**
     * 通知基础类型，当没有找到通知配置的时候，调用此通知类型
     */
    public String notify_base_type = "System";

    /**
     * 消息内容，使用 JSONObject 格式封装。
     * 该字段必须设置，否则无法发送消息。
     */
    public JSONObject message;

    /**
     * 设置组织代码。
     *
     * @param organize_code 组织代码
     * @return 当前 WechatCorpBotNotify 对象，支持链式调用
     */
    public WechatCorpBotNotify setOrganize_code(String organize_code) {
        this.organize_code = organize_code;
        return this;
    }

    /**
     * 设置通知类型。
     *
     * @param notify_type 通知类型
     * @return 当前 WechatCorpBotNotify 对象，支持链式调用
     */
    public WechatCorpBotNotify setNotify_type(String notify_type) {
        this.notify_type = notify_type;
        return this;
    }

    /**
     * 设置通知基础类型，当没有找到通知配置的时候，调用此通知类型
     *
     * @param notify_base_type
     * @return
     */
    public WechatCorpBotNotify setNotify_base_type(String notify_base_type) {
        this.notify_base_type = notify_base_type;
        return this;
    }

    /**
     * 设置消息内容。
     *
     * @param message 消息内容
     * @return 当前 WechatCorpBotNotify 对象，支持链式调用
     */
    public WechatCorpBotNotify setMessage(JSONObject message) {
        this.message = message;
        return this;
    }
    //endregion

    /**
     * 创建 WechatCorpBotNotify 的实例。
     *
     * @return WechatCorpBotNotify 的新实例
     */
    public static WechatCorpBotNotify getInstance() {
        return new WechatCorpBotNotify();
    }

    /**
     * send 方法
     * <p>
     * 此方法负责将消息通过企业微信机器人发送出去，并根据发送结果返回不同的状态码。
     *
     * @return SyncResult 返回发送结果的同步结果对象，包含操作结果的代码和信息。
     * <p>
     * 在消息发送过程中，该方法执行以下步骤：
     * <ol>
     *   <li>检查组织代码是否为空。如果为空，返回错误代码 2。</li>
     *   <li>检查通知类型是否为空。如果为空，返回错误代码 2。</li>
     *   <li>检查消息内容是否为空。如果为空，返回错误代码 2。</li>
     *   <li>根据组织代码和通知类型获取相应的企业微信机器人池。如果获取失败，返回错误代码 1。</li>
     *   <li>通过机器人池发送消息。如果发送成功，返回代码 0；如果发送失败且错误代码为 45009（表示消息发送频率超限），尝试将消息添加到延迟发送任务队列。</li>
     *   <li>如果延迟发送任务添加失败，返回错误代码 1；否则，返回代码 0，表示消息延迟发送成功。</li>
     * </ol>
     */
    public SyncResult send() {
        if (!StringUtils.hasLength(this.organize_code)) {
            organize_code = SysGlobalConfigEntity.getString("System:Organize:Code", ConfigManager.getString("System:Organize:Code"));
        }
        if (!StringUtils.hasLength(this.notify_type)) notify_type = this.notify_base_type;
        if (message == null) return new SyncResult(2, "缺少消息");

        // 获取指定组织代码和通知类型对应的企业微信机器人池
        WechatCorpBotPool botPool = SysNotifySettingsEntity.getInstance().getWechatCorpBotPool(organize_code, notify_type);
        if (botPool == null && !notify_type.equalsIgnoreCase(this.notify_base_type)) {
            botPool = SysNotifySettingsEntity.getInstance().getWechatCorpBotPool(organize_code, this.notify_base_type);
        }
        if (botPool == null) return new SyncResult(1, "无法获取通知配置");
        SyncResult r = botPool.send(this.message);
        // 如果发送失败，且错误代码为45009（表示消息发送频率超限）
        if (r.code == 45009) {
            // 尝试将消息添加到延迟发送任务队列
            if (WechatCorpBotNotifyDelayedMessageTaskJob.getInstance().add(this).code != 0) {
                return new SyncResult(1, "发送失败");
            }
            return new SyncResult(0, "消息延迟发送");
        }

        LogsUtil.info("企业微信通知", "==> %s %s %s", organize_code, notify_type, message.toJSONString());
        return new SyncResult(0, "");
    }
}