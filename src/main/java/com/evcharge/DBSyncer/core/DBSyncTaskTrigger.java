package com.evcharge.DBSyncer.core;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.ThreadUtil;


public abstract class DBSyncTaskTrigger implements IDBTaskTrigger {

    public String TAG = "数据同步触发器";

    /**
     * 内部封装的同步任务对象
     */
    protected DBSyncTask task;
    /**
     * where查询表达式模板，支持占位符替换，如：{start_time}、{end_time}
     */
    protected String WHERE_EXPRESS = "create_time >= {start_time} AND create_time <= {end_time}";
    /**
     * MQ主题
     */
    protected String MQTopic = "";
    /**
     * MQ标签
     */
    protected String MQTag = "";

    private ISyncResult initParams(JSONObject params) {
        if (params == null) params = new JSONObject();
        int page = JsonUtil.getInt(params, "page", 1);
        int rows = JsonUtil.getInt(params, "rows", 100);

        params.put("page", page);
        params.put("rows", rows);

        if (task == null) {
            LogsUtil.warn(TAG, "数据同步任务还没初始化");
            return new SyncResult(2, "数据同步任务还没初始化");
        }
        task.WhereExpress = WHERE_EXPRESS;
        return new SyncResult(0, "");
    }

    @Override
    public ISyncResult batch(JSONObject params, boolean useMQ) {
        return batch(params, useMQ, null);
    }

    /**
     * 同步触发函数
     *
     * @param params 参数
     * @param useMQ  是否使用 MQ 进行异步任务分发（true=RocketMQ；false=线程池）
     */
    @Override
    public ISyncResult batch(JSONObject params, boolean useMQ, IDataSyncListener iDataSyncListener) {
        ISyncResult r = initParams(params);
        if (!r.isSuccess()) return r;
        if (useMQ) {
            // 使用 RocketMQ 分页异步投递
            task.executeMQSync(null, params, MQTopic, MQTag);
        } else {
            // 使用线程池立即分页执行
            ThreadUtil.getInstance().execute(TAG, () -> task.executePagedSync(null, params, iDataSyncListener));
        }
        return new SyncResult(0, "任务已触发");
    }

    @Override
    public ISyncResult once(JSONObject params) {
        return once(params, null);
    }

    @Override
    public ISyncResult once(JSONObject params, IDataSyncListener iDataSyncListener) {
        ISyncResult r = initParams(params);
        if (!r.isSuccess()) return r;
        task.executeSyncOnce(null, params, iDataSyncListener);
        return new SyncResult(0, "任务已触发");
    }
}
