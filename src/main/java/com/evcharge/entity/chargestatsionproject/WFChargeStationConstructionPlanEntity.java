package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目施工计划;
 *
 * @author : JED
 * @date : 2023-10-25
 */
public class WFChargeStationConstructionPlanEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 开始时间
     */
    public long start_time;
    /**
     * 结束时间
     */
    public long end_time;
    /**
     * 内容
     */
    public String content;
    /**
     * 状态：0-未完成，1-完成
     */
    public int status;
    /**
     * 创建者id
     */
    public long creater_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WFChargeStationConstructionPlanEntity getInstance() {
        return new WFChargeStationConstructionPlanEntity();
    }
}
