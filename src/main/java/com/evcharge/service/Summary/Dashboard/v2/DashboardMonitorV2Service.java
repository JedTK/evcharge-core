package com.evcharge.service.Summary.Dashboard.v2;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionPageRequest;
import com.evcharge.dto.summary.RegionRequest;
import org.springframework.stereotype.Component;

@Component
public interface DashboardMonitorV2Service {

    JSONObject getMonitorList(RegionPageRequest request,String type);


}
