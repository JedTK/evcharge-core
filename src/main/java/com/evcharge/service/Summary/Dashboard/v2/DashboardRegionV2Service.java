package com.evcharge.service.Summary.Dashboard.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.service.Summary.Dashboard.v2.impl.DashboardRegionV2ServiceImpl;
import org.springframework.stereotype.Component;

@Component
public interface DashboardRegionV2Service {

    /**
     * 获取用户区域权限
     * @param adminId 管理员id
     * @param level 等级id 1=省份 2=市 3=区 4=街道
     * @param parentCode 父级code
     * @return JSONObject
     */
    JSONObject getUserRegion(long adminId,int level,String parentCode);

    JSONObject getUserRegionOptimized(long adminId, int level, String parentCode);

    JSONObject getUserRegionTree(long adminId) ;

    JSONObject getUserRegionPermission(long adminId);

}
