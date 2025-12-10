package com.evcharge.DBSyncer.core;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.rocketmq.XRocketMQ;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库同步任务抽象类，实现了通过分页或MQ方式同步主库与子库的数据。
 * 支持字段映射、增量对比同步、事务操作。
 */
public class DBSyncTask implements IDBSyncTask {
    private final static String TAG = "数据库同步";

    // region 属性
    /**
     * 主数据库名
     */
    public String mainDBName = "";

    /**
     * 子数据库名
     */
    public String slaveDBName;

    /**
     * 主表表名
     */
    public String mainTableName;

    /**
     * 子表表名
     */
    public String slaveTableName;

    /**
     * 主数据库：主键字段名，默认为 "Id"
     */
    public String MainPrimaryKey = "Id";
    /**
     * 从数据库：主键字段名，默认为 "Id"
     */
    public String SlavePrimaryKey = "Id";

    /**
     * 字段映射配置，用于主表字段与子表字段之间的对应
     */
    public List<DBSyncTableField> FieldMapping;

    /**
     * where查询表达式模板，支持占位符替换，如：{start_time}、{end_time}
     */
    public String WhereExpress;

    /**
     * 子库数据库连接对象，延迟初始化
     */
    private ISqlDBObject slaveDB;

    // 链式设置参数
    public DBSyncTask setMainDBName(String mainDBName) {
        this.mainDBName = mainDBName;
        return this;
    }

    public DBSyncTask setSlaveDBName(String slaveDBName) {
        this.slaveDBName = slaveDBName;
        return this;
    }

    public DBSyncTask setMainTableName(String mainTableName) {
        this.mainTableName = mainTableName;
        return this;
    }

    public DBSyncTask setSlaveTableName(String slaveTableName) {
        this.slaveTableName = slaveTableName;
        return this;
    }

    public DBSyncTask setMainPrimaryKey(String MainPrimaryKey) {
        MainPrimaryKey = MainPrimaryKey;
        return this;
    }

    public DBSyncTask setSlavePrimaryKey(String SlavePrimaryKey) {
        SlavePrimaryKey = SlavePrimaryKey;
        return this;
    }

    public DBSyncTask setFieldMapping(List<DBSyncTableField> fieldMapping) {
        FieldMapping = fieldMapping;
        return this;
    }

    public DBSyncTask setWhereExpress(String whereExpress) {
        this.WhereExpress = whereExpress;
        return this;
    }
    // endregion

    // region 字段生成

    /**
     * 获取主表查询字段字符串（带主键 + 所有映射字段）
     */
    public String getMainTableField() {
        return getTableFields("main", this.MainPrimaryKey);
    }

    /**
     * 获取子表查询字段字符串（带主键 + 所有映射字段）
     */
    public String getSlaveTableField() {
        return getTableFields("slave", this.SlavePrimaryKey);
    }

    /**
     * 内部通用方法，用于获取指定类型的字段列表（主表/子表）
     *
     * @param fieldType 字段类型："main" 或 "slave"
     */
    private String getTableFields(String fieldType, String PrimaryKey) {
        StringBuilder fieldStr = new StringBuilder();
        fieldStr.append(PrimaryKey).append(",");

        for (DBSyncTableField field : FieldMapping) {
            String value = "main".equals(fieldType) ? field.mainField : field.slaveField;
            if (StringUtil.isEmpty(value)) continue;
            fieldStr.append(value).append(",");
        }

        if (fieldStr.length() > 0) fieldStr.deleteCharAt(fieldStr.length() - 1); // 删除最后一个逗号
        return fieldStr.toString();
    }

    // endregion

    // region 同步接口实现

    /**
     * 分页同步（循环执行同步，直到返回空数据）
     */
    @Override
    public SyncResult executePagedSync(IDataSourceFetcher iDataSourceFetcher, JSONObject params, IDataSyncListener iDataSyncListener) {
        int page = JsonUtil.getInt(params, "page", 1);
        params.put("page", page);
        int rows = JsonUtil.getInt(params, "rows", 100);
        params.put("rows", rows);

        while (true) {
            SyncResult r = executeSyncOnce(iDataSourceFetcher, params, iDataSyncListener);
            if (!r.isSuccess()) return r;
            page++;
            params.put("page", page);
        }
    }

    /**
     * 执行单页同步（从主表读取一页数据，同步至子表）
     */
    @Override
    public SyncResult executeSyncOnce(IDataSourceFetcher iDataSourceFetcher, JSONObject params, IDataSyncListener iDataSyncListener) {
        if (iDataSourceFetcher == null) iDataSourceFetcher = DefaultDataSourceFetcher;
        if (slaveDB == null) slaveDB = DataService.getDB(slaveDBName).name(slaveTableName);

        LogsUtil.info(TAG, "%s.%s ====> %s.%s           分片同步中：%s", mainDBName, mainTableName, slaveDBName, slaveTableName, params.toString());
        List<Map<String, Object>> list = iDataSourceFetcher.fetchDataList(params);
        if (list == null || list.isEmpty()) {
            LogsUtil.warn(TAG, "%s.%s ====> %s.%s           同步完毕：%s", mainDBName, mainTableName, slaveDBName, slaveTableName, params.toString());
            return new SyncResult(100, "同步完毕");
        }
        return handleJob(list, iDataSyncListener);
    }

    /**
     * 使用 RocketMQ 异步分页分发任务（每页分发一条 MQ 消息）
     */
    @Override
    public SyncResult executeMQSync(IDataSourceFetcher iDataSourceFetcher,
                                    JSONObject params,
                                    String topic,
                                    String tag) {
        int page = JsonUtil.getInt(params, "page", 1);
        params.put("page", page);
        int rows = JsonUtil.getInt(params, "rows", 100);
        params.put("rows", rows);

        if (iDataSourceFetcher == null) iDataSourceFetcher = DefaultDataSourceFetcher;
        if (slaveDB == null) slaveDB = DataService.getDB(slaveDBName).name(slaveTableName);

        long count = iDataSourceFetcher.countData(params);
        if (count == 0) return new SyncResult(3, "无数据需要同步");
        long pages = Convert.toInt(Math.ceil((double) count / (double) rows));

        LogsUtil.info(TAG, "开始分发同步任务，共 %d 条数据，分 %d 页", count, pages);

        for (int i = 1; i <= pages; i++) {
            JSONObject clone = new JSONObject(params);  // 快速深拷贝
            clone.put("page", i);
            XRocketMQ.getGlobal().pushOneway(topic, tag, clone);
            LogsUtil.info(TAG, "已发送同步任务至MQ，第 %d 页", i);
        }

        return new SyncResult(0, String.format("同步任务已全部分发，共 %d 页", pages));
    }

    // endregion

    // region 默认主表数据源抓取器（用于主表分页查询）

    /**
     * 默认数据源提供器
     */
    private final IDataSourceFetcher DefaultDataSourceFetcher = new IDataSourceFetcher() {
        @Override
        public List<Map<String, Object>> fetchDataList(JSONObject params) {
            int page = JsonUtil.getInt(params, "page", 1);
            int rows = JsonUtil.getInt(params, "rows", 100);
            String whereExpression = buildWhereExpress(params);

            ISqlDBObject db = DataService.getMainDB()
                    .name(mainTableName)
                    .field(getMainTableField())
                    .page(page, rows)
                    .order("id"); // 默认按id排序分页
            if (StringUtil.isNotEmpty(whereExpression)) db.addWhere(whereExpression);
            return db.select();
        }

        @Override
        public long countData(JSONObject params) {
            String whereExpression = buildWhereExpress(params);
            ISqlDBObject db = DataService.getMainDB().name(mainTableName);
            if (StringUtil.isNotEmpty(whereExpression)) db.addWhere(whereExpression);
            return db.count();
        }
    };

    /**
     * 构建 where 表达式（将 WHERE_EXPRESS 中的 {key} 占位符替换为参数值）
     *
     * @param params 参数，如 { "start_time": 111, "end_time": 222 }
     * @return 替换后的表达式，例如 "create_time >= 111 AND create_time <= 222"
     */
    private String buildWhereExpress(JSONObject params) {
        if (params == null) params = new JSONObject();
        if (StringUtil.isEmpty(this.WhereExpress)) return "";

        String result = this.WhereExpress;
        for (String key : params.keySet()) {
            String placeholder = String.format("\\{%s\\}", key);
            String value = JsonUtil.getString(params, key);

            if (value == null) value = ""; // 避免 null 替换异常
            result = result.replaceAll(placeholder, value);
        }
        return result;
    }

    // endregion

    // region 数据同步逻辑处理

    /**
     * 执行同步逻辑：对比主表与子表的字段，执行新增或更新
     */
    private SyncResult handleJob(@NonNull List<Map<String, Object>> list, IDataSyncListener iDataSyncListener) {
        // 提取主键列表
        String[] pkArray = list.stream()
                .map(data -> MapUtil.getString(data, MainPrimaryKey))
                .filter(s -> s != null && !s.trim().isEmpty())
                .toArray(String[]::new);

        // 获取子库中已有的对应数据
        List<Map<String, Object>> slaveList = slaveDB
                .field(getSlaveTableField())
                .whereIn(SlavePrimaryKey, pkArray)
                .select();
        Map<String, Map<String, Object>> slaveMap = slaveList.stream()
                .collect(Collectors.toMap(
                        row -> MapUtil.getString(row, SlavePrimaryKey),
                        row -> row
                ));

        // 遍历主表数据进行比对与同步
        for (Map<String, Object> mainRecord : list) {
            String pk = MapUtil.getString(mainRecord, MainPrimaryKey);
            if (StringUtil.isEmpty(pk)) continue;

            Map<String, Object> syncRecord = slaveMap.getOrDefault(pk, new HashMap<>());
            boolean isAdd = syncRecord.isEmpty();
            if (isAdd) syncRecord.put(SlavePrimaryKey, pk);

            // 1) 默认字段映射：无论是否有监听器，都先把主表字段复制过去
            for (DBSyncTableField field : FieldMapping) {
                if (StringUtil.isEmpty(field.mainField) || StringUtil.isEmpty(field.slaveField)) continue;
                if (field.slaveField.equalsIgnoreCase(SlavePrimaryKey)) continue;

                Object mainValue = mainRecord.get(field.mainField);
                Object slaveValue = syncRecord.get(field.slaveField);

                // 值不一致才更新
                if (!MapUtil.looselyEquals(mainValue, slaveValue)) {
                    syncRecord.put(field.slaveField, mainValue);
                } else {
                    syncRecord.remove(field.slaveField); // 保持精简
                }
            }

            // 2) 监听器最后一刀：在默认映射的基础上进行加工/覆盖/删除
            if (iDataSyncListener != null) iDataSyncListener.onBeforeSyncRecord(mainRecord, syncRecord);

            // 3) 执行新增或更新
            if (isAdd) {
                slaveDB.insert(syncRecord);
                LogsUtil.info(TAG, "%s.%s ====> %s.%s           新增 - %s=%s", mainDBName, mainTableName, slaveDBName, slaveTableName, SlavePrimaryKey, pk);
            } else {
                // 防止主键重复更新
                if (!"id".equalsIgnoreCase(SlavePrimaryKey)) syncRecord.remove("id");
                syncRecord.remove(SlavePrimaryKey);

                if (!syncRecord.isEmpty()) {
                    slaveDB.where(SlavePrimaryKey, pk).update(syncRecord);
                    LogsUtil.info(TAG, "%s.%s ====> %s.%s           更新 - %s=%s", mainDBName, mainTableName, slaveDBName, slaveTableName, SlavePrimaryKey, pk);
                }
            }

            if (iDataSyncListener != null) iDataSyncListener.onAfterSyncRecord(mainRecord, syncRecord);
        }
        return new SyncResult(0, "");
    }

    // endregion
}

