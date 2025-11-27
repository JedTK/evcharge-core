package com.evcharge.service.Summary.Dashboard.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.dto.summary.RegionRequest;
import org.springframework.stereotype.Component;

@Component
public interface DashboardInspectV2Service {

    JSONObject getFireSupplies(RegionRequest request);

    /**
     * 获取品牌列表
     * @return JSONObject
     */
    JSONObject getOrganizeList();

    /**
     * 获取巡检统计
     *
     * @return JSONObject
     */
    JSONObject getInspectSummary(RegionRequest request);

    /**
     * 获取巡检站点,地图使用
     *
     * @return JSONObject
     */
    JSONObject getInspectStationForMap(RegionRequest request);

    /**
     * 获取站点详情
     * @return
     */
    JSONObject getInspectStationInfo(String uuid);

    /**
     * 获取巡检站点,列表使用
     *
     * @return JSONObject
     */
    JSONObject getInspectStationForPage(RegionPageRequest request,String keyword,String types);

    /**
     * 获取巡检日志
     *
     * @return JSONObject
     */
    JSONObject getInspectLogList(RegionPageRequest request);

    /**
     * 获取系统巡检日志
     * @param request RegionPageRequest
     * @return
     */
    JSONObject getSystemInspectLogList(RegionPageRequest request);
}
