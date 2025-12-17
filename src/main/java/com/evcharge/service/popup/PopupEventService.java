package com.evcharge.service.popup;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.popup.PopupConfigEntity;
import com.evcharge.entity.popup.PopupEventLogEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

/**
 * 弹窗日志;(PopupEventLog)表服务接口
 *
 * @author : JED
 * @date : 2025-12-12
 */
public class PopupEventService {

    private final static String TAG = "Popup事件日志";

    private volatile static PopupEventService instance;

    public static PopupEventService getInstance() {
        if (instance == null) {
            synchronized (PopupEventService.class) {
                if (instance == null) {
                    instance = new PopupEventService();
                }
            }
        }
        return instance;
    }

    /**
     * 新增弹窗日志
     *
     * @param uid        用户id
     * @param messageId  弹窗消息id
     * @param popup_code 弹窗编码
     * @param event_type 事件类型
     * @param biz_key    业务幂等主键（中奖记录ID/订单号等）
     * @param ext_data   额外JSON数据
     */
    public void add(long uid
            , String messageId
            , String popup_code
            , String event_type
            , String biz_key
            , String scene_code
            , String client_code
            , JSONObject ext_data) {
        if (StringUtil.isEmpty(scene_code)) {
            PopupConfigEntity cfg = PopupConfigService.getInstance().getByCode(popup_code);
            if (cfg == null) {
                LogsUtil.warn(TAG, String.format("无法查询%s弹窗配置", popup_code));
                return;
            }
            scene_code = cfg.scene_code;
            if (StringUtil.isEmpty(client_code)) client_code = cfg.client_code;
        }

        PopupEventLogEntity entity = new PopupEventLogEntity();
        entity.event_id = common.md5(common.getUUID() + TimeUtil.getTimestamp());
        entity.popup_code = popup_code;
        entity.message_id = messageId;
        entity.biz_key = biz_key;
        entity.uid = uid;
        entity.scene_code = scene_code;
        entity.client_code = client_code;
        entity.event_type = event_type;
        entity.event_day = TimeUtil.getTimeString("yyyy-MM-dd");
        entity.ext_json = ext_data == null ? "{}" : ext_data.toJSONString();
        entity.create_time = TimeUtil.getTimestamp();
        entity.insert();
    }
}
