package com.evcharge.service.Active;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;

import java.util.Map;

/**
 * 活动业务逻辑
 */
public class ACTService {

    /**
     * 活动同步触发器
     *
     * @param scene_code 场景编码
     * @param uid        用户id
     * @param biz_key    幂等业务键(订单号/充值单号/自定义)
     * @param params     参数值
     */
    public ISyncResult trigger(String scene_code, long uid, String biz_key, JSONObject params) {
        return new SyncResult(1, "");
    }

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

    }
}
