package com.evcharge.service.Active;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.ACTConfigEntity;
import com.evcharge.entity.active.ACTLogsEntity;
import com.evcharge.entity.active.ACTSceneEntity;
import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动业务逻辑
 */
public class ACTService {

    private final static String TAG = "活动业务";

    /**
     * 活动异步触发器
     *
     * @param scene_code     场景编码
     * @param uid            用户id
     * @param biz_key        幂等业务键(订单号/充值单号/自定义)
     * @param params         参数值
     * @param iAsyncListener 异步监听
     */
    public void triggerAsync(String scene_code, long uid, String biz_key, JSONObject params, IAsyncListener iAsyncListener) {
        ThreadUtil.getInstance().execute(TAG, new Runnable() {
            @Override
            public void run() {
                ISyncResult r = trigger(scene_code, uid, biz_key, params);
                if (iAsyncListener != null) iAsyncListener.onResult(0, r);
            }
        });
    }


    /**
     * 活动同步触发器
     *
     * @param scene_code 场景编码
     * @param uid        用户id
     * @param biz_key    幂等业务键(订单号/充值单号/自定义)
     * @param params     参数值
     */
    public ISyncResult trigger(@NonNull String scene_code, long uid, @NonNull String biz_key, JSONObject params) {
        if (StringUtil.isEmpty(scene_code)) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少场景编码");
            return new SyncResult(2, "缺少场景编码");
        }
        if (uid <= 0) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少用户id");
            return new SyncResult(2, "缺少用户id");
        }
        try {

            List<Map<String, Object>> list = ACTSceneService.getInstance().getList(scene_code);
            if (list == null || list.isEmpty()) return new SyncResult(3, "当前场景无配置任何活动");

            int success = 0;
            int error = 0;
            for (Map<String, Object> data : list) {
                String activity_code = MapUtil.getString(data, "activity_code");
                ISyncResult r = triggerByActivateCode(activity_code, scene_code, uid, biz_key, params);
                if (r.isSuccess()) success++;
                else error++;
            }
            Map<String, Object> cb_data = new LinkedHashMap<>();
            cb_data.put("success", success);
            cb_data.put("error", error);
            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "调用触发器过程中发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 活动触发器
     *
     * @param activity_code 活动编码
     * @param uid           用户id
     * @param biz_key       幂等业务键(订单号/充值单号/自定义)
     * @param params        参数值
     */
    private ISyncResult triggerByActivateCode(@NonNull String activity_code, @NonNull String scene_code, long uid, @NonNull String biz_key, JSONObject params) {
        if (StringUtil.isEmpty(activity_code)) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少活动编码");
            return new SyncResult(2, "缺少活动编码");
        }
        ACTConfigEntity cfg = ACTConfigService.getInstance().getConfig(activity_code);
        if (cfg == null) {
            LogsUtil.warn(TAG, "调用触发器错误：无 %s 活动配置", activity_code);
            return new SyncResult(11, "缺少活动配置");
        }

        if (cfg.status != 1) {
            LogsUtil.warn(TAG, "[%s] - 活动停止使用", activity_code);
            return new SyncResult(11, String.format("[%s] - 活动停止使用", activity_code));
        }

        // 表示有活动时间限制
        long now_time = TimeUtil.getTimestamp();
        if (cfg.start_time != 0 && cfg.end_time != 0) {
            if (now_time < cfg.start_time) {
                return new SyncResult(12, String.format("[%s] - 活动还没开始", activity_code));
            }
            if (now_time > cfg.end_time) {
                return new SyncResult(12, String.format("[%s] - 活动已结束", activity_code));
            }
        }

        // 检查幂等：同一场景，同一活动，同一用户，同一biz_key只能存在一个
        if (ACTLogsService.getInstance().exist(activity_code, scene_code, uid, biz_key)) {
            return new SyncResult(10, "已执行过");
        }

        IACTStrategy strategy = ACTStrategyFactory.getStrategy(cfg.strategy_code);
        if (strategy == null) {
            LogsUtil.warn(TAG, "[%s] - 没有被注册，无法使用", activity_code);
            return new SyncResult(11, String.format("[%s] - 没有被注册，无法使用", activity_code));
        }

        ACTContext ctx = new ACTContext();
        ctx.activity_code = activity_code;
        ctx.scene_code = scene_code;
        ctx.uid = uid;
        ctx.biz_key = biz_key;
        ctx.now_time = now_time;
        ctx.config = cfg;
        ctx.params = params;
        try {
            ISyncResult r = strategy.execute(ctx);
            // 当出现-1时不进行日志记录
            if (r.getCode() == -1) return r;

            ACTLogsEntity logsEntity = new ACTLogsEntity();
            logsEntity.activity_code = activity_code;
            logsEntity.scene_code = scene_code;
            logsEntity.uid = uid;
            logsEntity.biz_key = biz_key;
            logsEntity.result_code = r.getCode();
            logsEntity.result_msg = r.getMsg();
            logsEntity.extra_json = JSONObject.toJSONString(r.getData());
            ACTLogsService.getInstance().add(logsEntity);

            return r;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "调用触发器过程中发生错误");
        }
        return new SyncResult(1, "");
    }
}
