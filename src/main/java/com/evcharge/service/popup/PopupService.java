package com.evcharge.service.popup;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.popup.PopupConfigEntity;
import com.evcharge.entity.popup.PopupMessageEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 弹窗中台（Popup Center）
 * <p>
 * ## 建设目标：通过建设「弹窗中台」及统一判断 API，实现：
 * 1. **前端只调用一个接口**，由后端统一判断：
 * * 现在要不要弹？
 * * 弹哪个弹窗？
 * * 弹几个？以什么顺序？
 * 2. **配置化弹窗内容和策略**，无需频繁改代码上线：
 * * 弹窗文案、图片、按钮、跳转链接统一配置。
 * * 适配不同用户、不同平台、不同版本的展示规则。
 * 3. **统一的频次控制与人群控制**：
 * * 限制弹出次数、防骚扰。
 * * 区分“全体用户”和“指定用户 / 用户群体”。
 * 4. **统一的优先级与冲突处理**：
 * * 当一个时间点有多个弹窗满足条件，按照优先级 / 规则进行决策。
 * 5. **埋点与数据闭环**：
 * * 能统计某弹窗的曝光、点击、关闭情况，为后续运营优化提供数据支撑。
 * 一句话：**前端只负责问：现在应该弹什么？后端给出“弹或不弹 + 具体内容 + 展示策略”**。
 */
public class PopupService {

    private final static String TAG = "Popup模块";

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
     * 推送弹窗消息
     *
     * @param popup_code 弹窗代码
     * @param uid        用户id
     * @param var        参数
     * @param biz_key    业务主键，如中奖记录ID、订单号，用于幂等/排重
     */
    public ISyncResult push(String popup_code, long uid, JSONObject var, String biz_key) {
        if (popup_code == null || popup_code.isEmpty()) return new SyncResult(0, "popup_code不能为空");
        if (uid <= 0) return new SyncResult(0, "uid不合法");

        PopupConfigEntity cfg = PopupConfigService.getInstance().getByCode(popup_code);
        if (cfg == null) {
            LogsUtil.warn(TAG, String.format("无法查询%s弹窗配置", popup_code));
            return new SyncResult(3, String.format("无法查询%s弹窗配置", popup_code));
        }

        long nowTime = TimeUtil.getTimestamp();

        // 幂等：biz_key 非空时先查
        if (StringUtil.isNotEmpty(biz_key)) {
            boolean exists = PopupMessageEntity.getInstance()
                    .where("popup_code", popup_code)
                    .where("uid", uid)
                    .where("biz_key", biz_key)
                    .exist();
            if (exists) return new SyncResult(10, "重复推送消息");
        }

        PopupMessageEntity msg = new PopupMessageEntity();
        msg.message_id = common.md5(common.getUUID() + nowTime);
        msg.popup_code = popup_code;
        msg.uid = uid;
        msg.scene_code = cfg.scene_code;
        msg.client_code = cfg.client_code;
        msg.status = 0; // 任务状态：0=待处理，1=已下发(已返回给客户端)，2=已消费完成，3=已取消，4=已过期,;
        msg.biz_key = biz_key == null ? "" : biz_key;
        msg.vars_json = var == null ? "{}" : var.toJSONString();
        msg.expire_time = (cfg.end_time > 0 ? cfg.end_time : 0);
        msg.create_time = nowTime;
        msg.update_time = nowTime;
        msg.insert();

        // 事件：CREATE_MESSAGE（可选）/ 或 DELIVER 在 check 时写
        eventBus.publish(PopupEvent.create(uid, msg.messageId, popup_code, "CREATE_MESSAGE")
                .withScene(msg.sceneCode)
                .withClient(msg.clientCode)
                .withBizKey(msg.bizKey));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message_id", msg.message_id);
        return new SyncResult(0, "", data);
    }

    /**
     * 检查是否有弹窗消息
     *
     * @param uid         用户id
     * @param scene_code  触发场景编码，如 app_launch/home_enter/order_finish 或者是 页面路径
     * @param client_code 客户端编码
     */
    public ISyncResult check(long uid, String scene_code, String client_code) {

        return new SyncResult(1, "");
    }

    /**
     * 事件点击
     *
     * @param uid
     * @param message_id
     * @return
     */
    public ISyncResult event_click(long uid, String message_id) {
        return new SyncResult(1, "");
    }
}
