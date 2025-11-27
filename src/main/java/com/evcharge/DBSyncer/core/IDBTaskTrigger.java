package com.evcharge.DBSyncer.core;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.ISyncResult;

/**
 * 数据库任务触发接口，用于统一定义数据同步任务的触发方式。
 * 支持通过 MQ（消息队列）异步触发或直接触发执行一次任务。
 * <p>
 * 实现类可用于调度中心、消息总线、后台管理系统等模块调用。
 */
public interface IDBTaskTrigger {

    /**
     * 批量触发任务，可选择是否通过 MQ 推送异步执行。
     * <p>
     * 一般用于定时任务调度、管理员手动触发或消息队列分发任务。
     *
     * @param params 参数对象，包含必要的同步条件、时间范围、分页信息等
     * @param useMQ  是否通过 MQ 推送任务执行：
     *               - true：推送到消息队列，由消费者异步执行；
     *               - false：在当前线程直接执行。
     * @return 同步结果对象，封装了任务状态码、描述信息等
     */
    ISyncResult batch(JSONObject params, boolean useMQ);

    ISyncResult batch(JSONObject params, boolean useMQ, IDataSyncListener iDataSyncListener);

    /**
     * 单次触发任务（不使用 MQ 推送），适合调试或小批量任务场景，或者是MQ消费者收到消息处理时使用
     *
     * @param params 参数对象，可包含筛选条件、同步目标、数据源配置等
     * @return 同步结果对象，封装了任务状态码、描述信息等
     */
    ISyncResult once(JSONObject params);

    ISyncResult once(JSONObject params, IDataSyncListener iDataSyncListener);
}
