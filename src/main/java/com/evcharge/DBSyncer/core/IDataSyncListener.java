package com.evcharge.DBSyncer.core;

import java.util.Map;

/**
 * 数据同步生命周期监听器
 * 用于监听单条与整体同步过程的前后回调。
 */
public interface IDataSyncListener {

    /**
     * 单条数据同步前的回调（前置处理）
     *
     * @param mainRecord 主数据库记录的数据
     * @param syncRecord 需要同步的数据库的数据
     */
    void onBeforeSyncRecord(Map<String, Object> mainRecord, Map<String, Object> syncRecord);

    /**
     * 单条数据同步完成后的回调
     *
     * @param mainRecord 主数据库记录的数据
     * @param syncRecord 需要同步的数据库的数据
     */
    void onAfterSyncRecord(Map<String, Object> mainRecord, Map<String, Object> syncRecord);
}

