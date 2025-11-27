package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电桩渠道介绍表;
 *
 * @author : Jay
 * @date : 2024-6-25
 */
public class ChargeStationManagerEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 项目id
     */
    public String project_id;
    /**
     * 充电桩id
     */
    public long cs_id;
    /**
     * 介绍人
     */
    public long introducer_id;
    /**
     * 用户ID
     */
    public long admin_id;
    /**
     * 状态
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationManagerEntity getInstance() {
        return new ChargeStationManagerEntity();
    }

    /**
     * 批量创建渠道管理员
     *
     * @param introducerIds 介绍人id
     * @param projectId     项目id
     * @param adminId       管理员id
     */
    public void multiCreate(String[] introducerIds, String projectId, long adminId) {
        if (this.where("project_id", projectId).count() > 0) {
            this.where("project_id", projectId).del();
        }
        for (String i : introducerIds) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("project_id", projectId);
            data.put("introducer_id", i);
            data.put("admin_id", adminId);
            data.put("create_time", TimeUtil.getTimestamp());
            this.insert(data);
        }
    }


}