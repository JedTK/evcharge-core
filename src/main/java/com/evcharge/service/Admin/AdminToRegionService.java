package com.evcharge.service.Admin;

import com.evcharge.entity.admin.AdminToRegionEntity;
import com.xyzs.entity.SyncResult;

import java.util.List;


public class AdminToRegionService {


    public static AdminToRegionService getInstance() {
        return new AdminToRegionService();
    }


    public SyncResult getRegion(long adminId) {
        List<AdminToRegionEntity> list = AdminToRegionEntity.getInstance().
                where("admin_id", adminId)
                .selectList();

        if (list.isEmpty()) {
            return new SyncResult(1, "没有权限");
        }

        List<RegionTreeBuilder.RegionNode> regionNodes = RegionTreeBuilder.buildRegionHierarchy(list);

        return new SyncResult(0, "success", regionNodes);
    }



}
