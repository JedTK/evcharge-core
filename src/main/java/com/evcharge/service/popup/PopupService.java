package com.evcharge.service.popup;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.popup.PopupConfigEntity;
import com.evcharge.entity.popup.PopupMessageEntity;
import com.evcharge.entity.popup.PopupTemplateEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.text.template.MissingPolicy;
import com.xyzs.text.template.StringTemplateEngine;
import com.xyzs.utils.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 弹窗中台（Popup Center）核心服务
 * <p>
 * 目标一句话：
 * 前端只负责问“现在应该弹什么”，后端统一返回“弹或不弹 + 弹哪些 + 内容模板 + 展示策略”。
 * <p>
 * 一、模块职责边界
 * 1) 统一弹窗入口（check）
 * - 聚合“用户专属消息 + 全局消息”
 * - 做场景/客户端匹配
 * - 做过期处理
 * - 做优先级排序与冲突决策
 * - 下发时写事件（DELIVER）并将消息状态从待处理改为已下发
 * <p>
 * 2) 推送消息入口（push）
 * - 业务方只需要提供 popup_code + uid + var + biz_key
 * - 后端落库 PopupMessage（消息队列化）
 * - 使用 biz_key 做幂等去重（避免重复生成同一业务消息）
 * - 写事件（CREATE_MESSAGE）用于追踪来源（可选）
 * <p>
 * 3) 消费回执入口（event_click）
 * - 用户点击“主按钮”（确认、去参与、去更新等）
 * - 将消息状态置为已消费（status=2）
 * - 写事件（CLICK_CONFIRM）用于闭环统计
 * <p>
 * 二、数据对象约定（强烈建议保持一致）
 * 1) PopupConfig（配置）
 * - 决定：是否启用、有效期、优先级、模板、场景、客户端、是否强制、是否允许串行等
 * <p>
 * 2) PopupMessage（消息）
 * - 决定：某个用户此刻是否“待弹/已下发/已完成/已取消/已过期”
 * - vars_json：本次弹窗的动态变量（如中奖金额、订单号、特定跳转参数）
 * - biz_key：业务主键，用于幂等排重（同一业务不重复推送）
 * <p>
 * 3) PopupEventLog（事件日志，由 PopupEventService 负责写）
 * - 用于统计曝光、点击、关闭、确认等行为
 * - 用于后续做频控（PopupFrequencyStrategy）与运营分析
 * <p>
 * 三、消息状态（PopupMessage.status）约定
 * 0 = 待处理（尚未下发给客户端）
 * 1 = 已下发（check 已经返回给客户端，避免重复下发）
 * 2 = 已消费完成（用户点了确认或完成该弹窗流程）
 * 3 = 已取消（运营/系统取消，或被策略淘汰）
 * 4 = 已过期（超过 expire_time 或配置时间窗导致不再可弹）
 * <p>
 * 四、全局消息（uid=0）的处理方式
 * - 全局消息本质是“面向所有用户的消息模板”
 * - check 时对每个用户进行“物化”（复制成用户专属消息）
 * - 物化时必须做幂等：同一个 global message 不应给同一用户生成多次
 * <p>
 * 五、频控与人群控制（本文件未完全实现，但建议的落点）
 * - 频控策略建议放在 check 阶段：
 * 1) 对 candidates 做频控校验（今日最多弹几次、活动期最多弹几次、看过不再弹、点击不再弹）
 * 2) 频控数据来源：PopupEventLog（DELIVER/EXPOSE/CLICK/CLOSE 等）
 * - 人群控制建议在“候选筛选阶段”完成：
 * 1) 白名单/黑名单 uid
 * 2) 分组人群（用户标签、渠道、地区、版本等）
 * <p>
 * 六、关键索引建议（非常重要，避免并发重复与性能问题）
 * 1) PopupMessage 唯一约束（建议二选一或组合）
 * - UNIQUE(popup_code, uid, biz_key)
 * - 或 UNIQUE(uid, biz_key) （取决于 biz_key 的全局唯一性定义）
 * <p>
 * 2) PopupMessage 查询索引（check 强依赖）
 * - INDEX(uid, status)
 * - INDEX(uid, status, expire_time)
 * - INDEX(uid, popup_code)
 * <p>
 * 3) PopupEventLog 查询索引（频控/统计强依赖）
 * - INDEX(uid, popup_code, event_code, create_time)
 * - INDEX(message_id, event_code)
 * <p>
 * 七、一些实现取舍说明（你现在这份实现的逻辑取向）
 * - check 只取 status=0 的消息，避免重复下发
 * - 下发后立即改 status=1，保证幂等
 * - 全局消息在 check 时才扩散到用户（避免 push 时就爆炸式写入）
 * <p>
 * 八、注意事项（后续优化方向）
 * - 当前按优先级排序会产生 N+1 查询 PopupConfig（后续应做批量查询或缓存）
 * - 当前没有在 check 阶段写 EXPOSE/CLOSE 等事件，需要前端补回调接口或在渲染时上报
 * - matchList 目前只支持“逗号列表 + 末尾* 前缀通配”，复杂规则可以后续扩展
 */
public class PopupService {

    private final static String TAG = "Popup模块";

    /**
     * 单例实例
     * - 使用双重检查锁（DCL）保证并发安全与初始化性能
     */
    private volatile static PopupService instance;

    public static PopupService getInstance() {
        if (instance == null) {
            synchronized (PopupService.class) {
                if (instance == null) {
                    instance = new PopupService();
                }
            }
        }
        return instance;
    }

    /**
     * 推送弹窗消息（写入 PopupMessage）
     * <p>
     * 典型使用场景：
     * - 中奖提醒：popup_code=PRIZE_xxx，biz_key=中奖记录ID
     * - 订单完成：popup_code=ORDER_FINISH_xxx，biz_key=订单号
     * - 公告推送：popup_code=NOTICE_xxx，uid=0（全局），biz_key=公告ID
     * <p>
     * 幂等说明：
     * - 当 biz_key 非空时，会先检查是否存在相同 (popup_code + uid + biz_key) 的消息
     * - 已存在则认为重复推送，直接返回
     *
     * @param popup_code 弹窗编码（必须存在于 PopupConfig）
     * @param uid        用户 id；uid=0 表示全局消息（对所有用户生效）
     * @param var        动态变量（用于模板渲染/前端展示/跳转参数）
     * @param biz_key    业务主键（用于幂等/排重），允许为空
     */
    public ISyncResult push(String popup_code, long uid, JSONObject var, String biz_key) {
        if (popup_code == null || popup_code.isEmpty()) return new SyncResult(0, "popup_code不能为空");
        if (uid < 0) return new SyncResult(0, "uid不合法");

        // 读取配置：用于写入 scene/client/expire_time 等字段
        PopupConfigEntity cfg = PopupConfigService.getInstance().getByCode(popup_code);
        if (cfg == null) {
            LogsUtil.warn(TAG, String.format("无法查询%s弹窗配置", popup_code));
            return new SyncResult(3, String.format("无法查询%s弹窗配置", popup_code));
        }

        long nowTime = TimeUtil.getTimestamp();

        // 幂等：biz_key 非空时先查重
        // 说明：这个去重会依赖你的数据层是否对 exist() 做了高效索引支持
        if (StringUtil.isNotEmpty(biz_key)) {
            boolean exists = PopupMessageEntity.getInstance()
                    .where("popup_code", popup_code)
                    .where("uid", uid)
                    .where("biz_key", biz_key)
                    .exist();
            if (exists) return new SyncResult(10, "重复推送消息");
        }

        // 构建消息实体
        PopupMessageEntity msg = new PopupMessageEntity();
        msg.message_id = common.md5(common.getUUID() + nowTime); // 消息唯一标识（建议数据库加 UNIQUE(message_id)）
        msg.popup_code = popup_code;
        msg.uid = uid;
        msg.scene_code = cfg.scene_code;   // 将配置里的触发场景固化到消息，避免配置变更影响历史消息
        msg.client_code = cfg.client_code; // 同上
        msg.status = 0;                    // 0=待处理，等待 check 下发
        msg.biz_key = biz_key == null ? "" : biz_key;
        msg.vars_json = var == null ? "{}" : var.toJSONString();

        // 过期时间：这里采用 cfg.end_time 作为消息过期时间
        // - end_time<=0 表示不过期
        // - 如果你希望“消息独立过期”，可以改成 min(cfg.end_time, now+ttl) 或者完全独立配置
        msg.expire_time = (cfg.end_time > 0 ? cfg.end_time : 0);
        msg.create_time = nowTime;
        msg.update_time = nowTime;

        // 入库
        msg.insert();

        // 写事件：CREATE_MESSAGE
        // - 用于追踪消息生成来源（业务推送）
        // - 如果你担心事件量太大，也可以只在 DELIVER/CLICK 等阶段记
        PopupEventService.getInstance().add(uid
                , msg.message_id
                , popup_code
                , "CREATE_MESSAGE"
                , biz_key
                , cfg.scene_code
                , cfg.client_code
                , var
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message_id", msg.message_id);
        return new SyncResult(0, "", data);
    }

    /**
     * 前端统一入口：检查“现在应该弹什么”
     * <p>
     * 返回结构建议：
     * - popups: []  按优先级排序
     * - 每个 item 内包含：message_id、popup_code、template_code、vars、is_force、allow_multi_chain 等
     * <p>
     * 核心流程：
     * 1) 查询用户专属待处理消息（uid=当前用户，status=0）
     * 2) 查询全局待处理消息（uid=0，status=0）
     * 3) 将符合当前 scene/client 的全局消息物化为用户消息（幂等）
     * 4) 合并后再做过期、scene/client 二次过滤
     * 5) 按配置 priority 排序
     * 6) 逐条组装返回，并将消息置为已下发（status=1），写事件 DELIVER
     * 7) 按 allow_multi_chain 控制是否继续返回后续弹窗
     * <p>
     * 备注：
     * - 这里暂时没有频控策略判断（PopupFrequencyStrategy），后续建议插入在 candidates 生成之后
     * - 下发后改 status=1，可避免客户端反复调用导致重复下发
     *
     * @param uid         用户 id（必须登录）
     * @param scene_code  场景编码（如 app_launch/home_enter/order_finish）
     * @param client_code 客户端编码（如 MINI_PROGRAM/IOS/ANDROID）
     */
    public ISyncResult check(long uid, String scene_code, String client_code) {
        if (uid <= 0) return new SyncResult(99, "请先登录");
        long now = TimeUtil.getTimestamp();

        if (scene_code == null) scene_code = "";
        if (client_code == null) client_code = "";

        // 1) 查用户专属待处理消息
        // 说明：先只取 status=0，避免重复下发；若要支持“下发失败重试”，可引入 retry_count 或回滚机制
        List<PopupMessageEntity> userPending = PopupMessageEntity.getInstance()
                .where("uid", uid)
                .whereIn("status", "0,1")
                .selectList();

        // 2) 查全局待处理消息（uid=0）
        // 说明：全局消息本质是模板，check 时才扩散到用户，避免 push 时写爆库
        List<PopupMessageEntity> globalPending = PopupMessageEntity.getInstance()
                .where("uid", 0)
                .where("status", 0)
                .selectList();

        // 3) 全局消息物化为当前用户专属消息（幂等）
        // 注意：这里会根据 scene/client 做过滤，避免把不适用的全局消息复制给用户
        for (PopupMessageEntity g : globalPending) {

            // 3.1) 过期判断：全局消息过期后可以直接标记过期，避免后续每次 check 反复判断
            if (isExpired(g, now)) {
                markExpired(g, now);
                continue;
            }

            // 3.2) 场景/客户端匹配（为空视为不限制）
            if (!matchList(g.scene_code, scene_code)) continue;
            if (!matchList(g.client_code, client_code)) continue;

            // 3.3) 物化为用户消息（并发下可能重复插入，需依赖唯一索引兜底）
            PopupMessageEntity uMsg = materializeGlobalMessageToUser(g, uid, now);
            if (uMsg != null) userPending.add(uMsg);
        }

        // 4) 过滤过期 + 再次过滤 scene/client（用户消息也需要过滤）
        // 说明：用户消息理论上在 push 时已固化 scene/client，但仍建议在下发前再校验一遍
        List<PopupMessageEntity> candidates = new java.util.ArrayList<>();
        for (PopupMessageEntity m : userPending) {
            if (isExpired(m, now)) {
                markExpired(m, now);
                continue;
            }
            if (!matchList(m.scene_code, scene_code)) continue;
            if (!matchList(m.client_code, client_code)) continue;
            candidates.add(m);
        }

        // 没有候选，返回空数组
        if (candidates.isEmpty()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("popups", java.util.Collections.emptyList());
            return new SyncResult(0, "", data);
        }

        // 5) 按 PopupConfig.priority 排序（小的优先）
        // 说明：
        // - 这里会产生 N+1 查询（每条消息查一次 config）
        // - 后续建议把 popup_code 批量查出 config map，或做本地缓存
        candidates.sort((a, b) -> {
            PopupConfigEntity ca = PopupConfigService.getInstance().getByCode(a.popup_code);
            PopupConfigEntity cb = PopupConfigService.getInstance().getByCode(b.popup_code);
            int pa = (ca == null ? 999999 : ca.priority);
            int pb = (cb == null ? 999999 : cb.priority);
            if (pa != pb) return Integer.compare(pa, pb);
            // 同优先级按创建时间早的先（更“公平”）
            return Long.compare(a.create_time, b.create_time);
        });

        // 6) 根据 allow_multi_chain 决定返回哪些
        // - allow_multi_chain=0：只返回第一条（强互斥）
        // - allow_multi_chain=1：允许串行返回多个（如先弹公告再弹活动）
        java.util.List<Map<String, Object>> popups = new java.util.ArrayList<>();
        int maxReturn = 5; // 保护上限，避免一次返回过多导致前端弹窗风暴

        for (PopupMessageEntity m : candidates) {
            PopupConfigEntity cfg = PopupConfigService.getInstance().getByCode(m.popup_code);
            // 配置不存在或未启用，跳过
            if (cfg == null || cfg.status != 1) continue;

            PopupTemplateEntity templateEntity = PopupTemplateService.getInstance().getByCode(m.popup_code);
            if (templateEntity == null || templateEntity.status != 1) continue;

            // 配置有效期校验：配置被下线/过期就不弹
            if (!isConfigActive(cfg, now)) continue;

            if (StringUtil.isEmpty(m.vars_json)) m.vars_json = "{}";
            JSONObject vars = JSONObject.parseObject(m.vars_json);
            if (vars == null) vars = new JSONObject();

            // 组装返回给前端的 item
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("message_id", m.message_id);
            item.put("title", StringTemplateEngine.render(templateEntity.title, vars, MissingPolicy.EMPTY_STRING));
            item.put("sub_title", StringTemplateEngine.render(templateEntity.sub_title, vars, MissingPolicy.EMPTY_STRING));
            item.put("content", StringTemplateEngine.render(templateEntity.content, vars, MissingPolicy.EMPTY_STRING));
            item.put("image_url", StringTemplateEngine.render(templateEntity.image_url, vars, MissingPolicy.EMPTY_STRING));

            // 组建按钮数据
            String button_confirm_json_str = StringTemplateEngine.render(templateEntity.button_confirm_json, vars, MissingPolicy.EMPTY_STRING);
            JSONObject button_confirm_json = JsonUtil.toJSON(button_confirm_json_str);
            String button_close_json_str = StringTemplateEngine.render(templateEntity.button_close_json, vars, MissingPolicy.EMPTY_STRING);
            JSONObject button_close_json = JsonUtil.toJSON(button_close_json_str);

            item.put("button_confirm_json", button_confirm_json);
            item.put("button_close_json", button_close_json);
            popups.add(item);

            // 7) 更新消息状态为已下发 + 记录 DELIVER 事件
            // 说明：
            // - status=1 表示已经对客户端“下发过”了
            // - DELIVER 是“后端决定下发”的事件，不等于用户真实曝光（EXPOSE 需要前端渲染后回调）
            try {
                if (m.status != 1) {
                    m.update(m.id, new LinkedHashMap<>() {{
                        put("status", 1);
                        put("update_time", now);
                    }});
                }

                PopupEventService.getInstance().add(
                        uid,
                        m.message_id,
                        m.popup_code,
                        "DELIVER",
                        m.biz_key,
                        scene_code,
                        client_code,
                        null
                );
            } catch (Exception e) {
                // 下发事件失败不应阻断主流程，但需要记录日志
                LogsUtil.error(TAG, "下发弹窗消息失败: " + e.getMessage());
            }

            // 冲突处理：不允许串行则只返回第一条
            if (cfg.allow_multi_chain == 0) break;

            // 返回数量保护
            if (popups.size() >= maxReturn) break;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("popups", popups);
        return new SyncResult(0, "", data);
    }

    /**
     * 物化全局消息为用户专属消息
     * <p>
     * 目的：
     * - 全局消息（uid=0）不直接给每个用户写一条，避免爆库
     * - 由 check 在用户访问时“按需复制”成用户专属消息
     * <p>
     * 幂等策略：
     * - derivedBizKey：如果 globalMsg.biz_key 为空，则使用 "GLOBAL:" + globalMsg.message_id
     * - 以 (popup_code + uid + derivedBizKey) 做查重，避免一个用户被复制多次
     * <p>
     * 并发说明：
     * - 多线程/多实例同时 check 同一个用户时，可能同时走到 insert
     * - 推荐数据库加唯一索引兜底，insert 重复键时在 catch 中吞掉即可
     *
     * @param globalMsg 全局消息实体
     * @param uid       用户 id
     * @param nowMs     当前时间戳
     * @return 新生成的用户消息；若已存在或插入失败返回 null
     */
    private PopupMessageEntity materializeGlobalMessageToUser(PopupMessageEntity globalMsg, long uid, long nowMs) {
        if (globalMsg == null) return null;

        // 兜底 biz_key，保证可幂等
        String derivedBizKey = globalMsg.biz_key;
        if (StringUtil.isEmpty(derivedBizKey)) derivedBizKey = "GLOBAL:" + globalMsg.message_id;

        // 幂等检查：用户是否已经有这条消息
        boolean exists = PopupMessageEntity.getInstance()
                .where("popup_code", globalMsg.popup_code)
                .where("uid", uid)
                .where("biz_key", derivedBizKey)
                .exist();
        if (exists) return null;

        PopupMessageEntity msg = new PopupMessageEntity();
        msg.message_id = common.md5(common.getUUID() + nowMs);
        msg.popup_code = globalMsg.popup_code;
        msg.uid = uid;
        msg.scene_code = globalMsg.scene_code;
        msg.client_code = globalMsg.client_code;
        msg.status = 0;
        msg.biz_key = derivedBizKey;
        msg.vars_json = StringUtil.isEmpty(globalMsg.vars_json) ? "{}" : globalMsg.vars_json;
        msg.expire_time = globalMsg.expire_time;
        msg.create_time = nowMs;
        msg.update_time = nowMs;

        try {
            msg.insert();

            // 事件是否记录？
            // - 不建议记录 CREATE_MESSAGE（因为全局消息会给很多用户生成，事件量会膨胀）
            // - 建议只在真正下发时写 DELIVER 即可

            return msg;
        } catch (Exception e) {
            // 并发插入时如果你加了唯一索引，这里可能抛重复键；吞掉即可
            LogsUtil.warn(TAG, "物化全局消息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 事件：点击主按钮（确认按钮）
     * <p>
     * 语义：
     * - 用户点击弹窗的主按钮（确认/去参与/去更新等）
     * - 将消息状态置为已消费（status=2）
     * - 写 CLICK_CONFIRM 事件
     * <p>
     * 安全性：
     * - 校验 message_id 归属，避免用户点击他人的 message_id
     * <p>
     * 幂等性：
     * - 已消费（status=2）再次点击，直接返回成功
     *
     * @param uid        用户 id
     * @param message_id 消息 id
     */
    public ISyncResult event_click(long uid, String message_id, String button_code) {
        if (uid <= 0) return new SyncResult(99, "请先登录");
        if (StringUtil.isEmpty(message_id)) return new SyncResult(0, "message_id不能为空");
        if (StringUtil.isEmpty(button_code)) return new SyncResult(0, "button_code不能为空");

        long now = TimeUtil.getTimestamp();

        List<PopupMessageEntity> list = PopupMessageEntity.getInstance()
                .where("message_id", message_id)
                .selectList();
        if (list == null || list.isEmpty()) return new SyncResult(3, "弹窗消息不存在");

        PopupMessageEntity msg = list.get(0);
        if (msg.uid != uid) return new SyncResult(98, "无权限操作该弹窗消息");

        if (isExpired(msg, now)) {
            markExpired(msg, now);
            return new SyncResult(4, "弹窗消息已过期");
        }

        // 反查按钮定义（推荐走快照；这里演示走当前模板）
        PopupConfigEntity cfg = PopupConfigService.getInstance().getByCode(msg.popup_code);
        if (cfg == null) return new SyncResult(3, "弹窗配置不存在");

        // 你这里目前用 getByCode(m.popup_code) 拿模板，其实更合理的是 cfg.template_code
        PopupTemplateEntity tpl = PopupTemplateService.getInstance().getByCode(cfg.template_code);
        if (tpl == null) return new SyncResult(3, "弹窗模板不存在");

        JSONObject vars = JSONObject.parseObject(StringUtil.isEmpty(msg.vars_json) ? "{}" : msg.vars_json);
        if (vars == null) vars = new JSONObject();

        JSONObject btnConfirm = JsonUtil.toJSON(StringTemplateEngine.render(tpl.button_confirm_json, vars, MissingPolicy.EMPTY_STRING));
        JSONObject btnClose = JsonUtil.toJSON(StringTemplateEngine.render(tpl.button_close_json, vars, MissingPolicy.EMPTY_STRING));

        JSONObject btn = null;
        if (btnConfirm != null && button_code.equals(btnConfirm.getString("button_code"))) btn = btnConfirm;
        if (btnClose != null && button_code.equals(btnClose.getString("button_code"))) btn = btnClose;

        if (btn == null) {
            // 防御：前端传了未知按钮编码
            PopupEventService.getInstance().add(uid, msg.message_id, msg.popup_code,
                    "CLICK_UNKNOWN", msg.biz_key, msg.scene_code, msg.client_code,
                    new JSONObject().fluentPut("button_code", button_code));
            return new SyncResult(6, "未知按钮");
        }

        String action = btn.getString("action");
        String consumePolicy = btn.getString("consume_policy"); // CONSUME / KEEP / CANCEL

        // 根据 consume_policy 决定消息状态
        if ("CONSUME".equalsIgnoreCase(consumePolicy)) {
            if (msg.status != 2) {
                msg.where("message_id", message_id).update(new LinkedHashMap<>() {{
                    put("status", 2);
                    put("update_time", now);
                }});
            }
        } else if ("CANCEL".equalsIgnoreCase(consumePolicy)) {
            msg.where("message_id", message_id).update(new LinkedHashMap<>() {{
                put("status", 3);
                put("update_time", now);
            }});
        } else {
            // KEEP：不改状态（或者只更新时间）
            msg.where("message_id", message_id).update(new LinkedHashMap<>() {{
                put("update_time", now);
            }});
        }

        // 写事件：带上 button_code/action（后续分析就有了）
        JSONObject ext = new JSONObject();
        ext.put("button_code", button_code);
        ext.put("action", action);
        ext.put("consume_policy", consumePolicy);

        PopupEventService.getInstance().add(uid, msg.message_id, msg.popup_code,
                "CLICK", msg.biz_key, msg.scene_code, msg.client_code, ext);

        return new SyncResult(0, "");
    }

    // region remark - 辅助函数

    /**
     * 配置有效期判断
     * <p>
     * 判定规则：
     * - status 必须启用
     * - start_time > 0 且 now < start_time：未开始
     * - end_time > 0 且 now > end_time：已结束
     *
     * @param cfg   弹窗配置
     * @param nowMs 当前时间戳
     * @return 是否在有效期内且启用
     */
    private boolean isConfigActive(PopupConfigEntity cfg, long nowMs) {
        if (cfg == null) return false;
        if (cfg.status != 1) return false;
        if (cfg.start_time > 0 && nowMs < cfg.start_time) return false;
        return cfg.end_time <= 0 || nowMs <= cfg.end_time;
    }

    /**
     * 消息过期判断
     * <p>
     * 规则：
     * - expire_time <= 0：不过期
     * - now > expire_time：过期
     *
     * @param msg   弹窗消息
     * @param nowMs 当前时间戳
     * @return 是否过期
     */
    private boolean isExpired(PopupMessageEntity msg, long nowMs) {
        if (msg == null) return true;
        if (msg.expire_time <= 0) return false;
        return nowMs > msg.expire_time;
    }

    /**
     * 标记消息为过期
     * <p>
     * 注意：
     * - 这里按 message_id 更新，依赖 message_id 的唯一性
     * - 如果 msg 为空应避免调用（当前调用点都已保证非空）
     *
     * @param msg   弹窗消息
     * @param nowMs 当前时间戳
     */
    private void markExpired(PopupMessageEntity msg, long nowMs) {
        msg.where("message_id", msg.message_id).update(new LinkedHashMap<>() {{
            put("status", 4);
            put("update_time", nowMs);
        }});
    }

    /**
     * JSON 列表匹配工具（支持通配与可选排除）
     *
     * 规则来源：数据库配置字段（必须是 JSON 数组字符串）
     * 示例：
     * 1) 不限制（放行）：null / "" / "[]" / ["*"] / ["ALL"]
     * 2) 精确匹配：["MINI_PROGRAM","IOS"]
     * 3) 前缀通配：["/pages/*","app_*"]   （仅支持末尾 '*' 的前缀通配）
     * 4) 排除规则（可选）：["/pages/*","!/pages/debug/*"]
     *
     * 匹配语义：
     * - 默认是“白名单匹配”：命中任意正向 token 即通过
     * - 若存在排除 token（以 '!' 开头），且 value 命中排除规则，则直接拒绝（优先级最高）
     *
     * 注意事项：
     * - 空数组/空字符串：视为不限制（返回 true）
     * - "*" / "ALL"：视为全放行（返回 true）
     * - 解析失败：视为不命中（返回 false），并记录 warn 日志，便于排查脏配置
     * - 是否大小写敏感：由 ignoreCase 控制；路径通常建议敏感，client_code 可不敏感
     *
     * @param rawJsonArray 配置规则（JSON 数组字符串）
     * @param value        待匹配值（scene_code/client_code）
     * @return true=命中/放行；false=不命中/拦截
     */
    private boolean matchList(String rawJsonArray, String value) {
        return matchList(rawJsonArray, value, false);
    }

    /**
     * @param ignoreCase 是否忽略大小写（true 时用 equalsIgnoreCase/startsWithIgnoreCase）
     */
    private boolean matchList(String rawJsonArray, String value, boolean ignoreCase) {
        // value 为空：按空字符串处理，避免 NPE
        value = (value == null) ? "" : value;

        // 规则为空：不限制 -> 放行
        if (StringUtil.isEmpty(rawJsonArray)) return true;

        String s = rawJsonArray.trim();
        if (s.isEmpty()) return true;

        // 兼容极端情况：有人直接写 "*" 或 "ALL"（虽然你说全 JSON，但留着更稳）
        if ("*".equals(s) || "ALL".equalsIgnoreCase(s)) return true;

        final JSONArray arr;
        try {
            arr = JSONArray.parseArray(s);
        } catch (Exception e) {
            LogsUtil.warn(TAG, "matchList：规则不是合法 JSON 数组，raw=" + s);
            return false;
        }

        // 空数组：不限制 -> 放行
        if (arr == null || arr.isEmpty()) return true;

        // 先处理排除规则：任何一个排除命中 -> 直接拒绝
        for (int i = 0; i < arr.size(); i++) {
            String token = StringUtil.trimToEmpty(arr.getString(i));
            if (token.isEmpty()) continue;

            if (token.charAt(0) == '!') {
                String deny = token.substring(1).trim();
                if (deny.isEmpty()) continue;
                if (matchToken(deny, value, ignoreCase)) return false;
            }
        }

        // 再处理放行规则：任一命中 -> 放行
        for (int i = 0; i < arr.size(); i++) {
            String token = StringUtil.trimToEmpty(arr.getString(i));
            if (token.isEmpty()) continue;

            // 跳过排除 token
            if (token.charAt(0) == '!') continue;

            // 全放行 token
            if ("*".equals(token) || "ALL".equalsIgnoreCase(token)) return true;

            if (matchToken(token, value, ignoreCase)) return true;
        }

        return false;
    }

    /**
     * 单个 token 匹配：
     * - 精确匹配：token == value
     * - 前缀通配：token 以 '*' 结尾，则比较 value 是否以 token 去掉 '*' 的前缀开头
     */
    private boolean matchToken(String token, String value, boolean ignoreCase) {
        if (!ignoreCase) {
            if (token.equals(value)) return true;
            if (token.endsWith("*")) {
                String prefix = token.substring(0, token.length() - 1);
                return !prefix.isEmpty() && value.startsWith(prefix);
            }
            return false;
        }

        // ignoreCase = true
        if (token.equalsIgnoreCase(value)) return true;
        if (token.endsWith("*")) {
            String prefix = token.substring(0, token.length() - 1);
            return !prefix.isEmpty() && startsWithIgnoreCase(value, prefix);
        }
        return false;
    }

    private boolean startsWithIgnoreCase(String s, String prefix) {
        if (s == null || prefix == null) return false;
        if (prefix.length() > s.length()) return false;
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    // endregion
}
