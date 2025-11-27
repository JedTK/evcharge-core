package com.evcharge.DBSyncer.core;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;

/**
 * 数据库同步任务接口，用于定义不同同步方式的统一规范。
 * 支持单次同步、分页同步以及基于MQ的异步同步调度。
 * 实现类可根据实际业务逻辑选择不同的数据来源和调度方式。
 */
public interface IDBSyncTask {

    /**
     * 分页同步任务执行（适用于单机分页任务，也可由MQ消费者分页调度调用）。
     *
     * <p>一般用于控制端通过分页参数执行多个批次的同步。</p>
     *
     * @param iDataSourceFetcher 数据源获取器接口（用于主表数据拉取）
     * @param params             参数对象，通常包含分页信息（如 page、rows）、时间区间、过滤条件等
     * @return 同步结果对象，封装状态码与提示信息
     */
    SyncResult executePagedSync(IDataSourceFetcher iDataSourceFetcher, JSONObject params, IDataSyncListener iDataSyncListener);

    /**
     * 单次同步任务执行（执行一次分页数据的同步）。
     *
     * <p>该方法主要在分页任务的具体某一页或MQ消费者中被调用，用于执行一次完整的子任务。</p>
     *
     * @param iDataSourceFetcher 数据源获取器接口
     * @param params             参数对象，包含一次分页执行所需条件
     * @return 同步结果
     */
    SyncResult executeSyncOnce(IDataSourceFetcher iDataSourceFetcher, JSONObject params, IDataSyncListener iDataSyncListener);

    /**
     * 通过消息队列（MQ）进行同步任务分发。
     *
     * <p>本方法负责将分页任务或指定数据推送到MQ队列中，由MQ消费者进行异步拉取并调用 {@link #executeSyncOnce} 执行。</p>
     *
     * <p>注意：MQ消费者收到消息后应解析参数并调用 executeSyncOnce 或 executePagedSync 执行实际任务。</p>
     *
     * @param iDataSourceFetcher 数据源获取器接口
     * @param params             参数对象，用于控制同步的数据范围、分页等信息
     * @param topic              RocketMQ 主题
     * @param tag                RocketMQ 标签，用于消息分类
     * @return 推送结果对象
     */
    SyncResult executeMQSync(IDataSourceFetcher iDataSourceFetcher, JSONObject params, String topic, String tag);
}
