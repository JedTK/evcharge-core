package com.evcharge.service.Summary.Dashboard.v2.impl;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.dto.summary.RegionRequest;
import com.evcharge.service.Summary.Dashboard.v2.DashboardUserV2Service;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardUserV2ServiceImpl implements DashboardUserV2Service {


    @Override
    public JSONObject getUserSummary() {

        String cacheKey = "Dashboard:V2:User:UserSummary";

        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);

        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }
        Map<String, Object> data = new HashMap<>();
        ISqlDBObject db = DataService.getDB().name("User");
        db.where("status", 0);
        db.where("is_robot",0);
        long userCount = db.count();
        data.put("user_count", userCount);

        DataService.getMainCache().setMap(cacheKey, data);

        return common.apicb(0, "success", data);
    }

    public JSONObject getUserRegisterChartByMonth(int month) {
        String cacheKey = String.format("Dashboard:V2:User:RegisterChart:%s", month);
        List<Map<String, Object>> cache = DataService.getMainCache().getList(cacheKey);

        if (!cache.isEmpty()) {
            return common.apicb(0, "success", cache);
        }

        long monthTime = TimeUtil.getMonthBegin(-month);

        ISqlDBObject db = DataService.getDB().name("PlatformMonthSummaryV2");
        db.where("organize_code", "genkigo");
        db.field("id,date as month,total_registered_users");
        db.where("date_time", ">=", monthTime);
        db.order("date_time asc");

        List<Map<String, Object>> list = db.select();

        if (list.isEmpty()) return common.apicb(1, "No data available.");
        int totalRegisteredUsers=0;
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            if(i==0){
                totalRegisteredUsers= MapUtil.getInt(map,"total_registered_users");
                continue;
            }
            totalRegisteredUsers=totalRegisteredUsers+MapUtil.getInt(map,"total_registered_users");
            map.put("total_registered_users",totalRegisteredUsers);
        }
        DataService.getMainCache().setList(cacheKey, list, ECacheTime.DAY);
        return common.apicb(0, "success", list);
    }
}
