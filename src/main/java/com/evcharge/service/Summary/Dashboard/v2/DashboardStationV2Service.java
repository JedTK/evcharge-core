package com.evcharge.service.Summary.Dashboard.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionRequest;
import org.springframework.stereotype.Component;

@Component
public interface DashboardStationV2Service {

    /**
     * 获取站点统计数据
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getStationSummary(RegionRequest request);

    /**
     * 获取站点统计数据明细
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getStationSummaryDetail(RegionRequest request);

    /**
     * 获取站点使用率
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getStationUseRate(RegionRequest request);

    /**
     * 获取端口使用情况
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getSocketUsage(RegionRequest request);


    /**
     * 获取充电数据
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getChargeChartByMonth(RegionRequest request, int month);

    /**
     * 获取用户注册数
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getUserCountChartByMonth(RegionRequest request, int month);



    /**
     * 获取充电数据
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getChargeChartByDay(RegionRequest request, int days);

    /**
     * 获取用户注册数
     *
     * @param request RegionRequest
     * @return JSONObject
     */
    JSONObject getUserCountChartByDay(RegionRequest request, int days);

}
