package com.evcharge.DBSyncer.core;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * 数据源获取器
 */
public interface IDataSourceFetcher {
    /**
     * 获取数据列表
     *
     * @param params 查询参数
     * @return 数据列表
     */
    List<Map<String, Object>> fetchDataList(JSONObject params);

    /**
     * 获取总数据量
     *
     * @param params 查询参数
     * @return 总数据量
     */
    long countData(JSONObject params);
}
