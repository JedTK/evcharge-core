package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目财务信息;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WFChargeStationProjectFinancialsEntity extends BaseEntity implements Serializable {
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
     * 成本标题
     */
    public String title;
    /**
     * 成本
     */
    public double cost;
    /**
     * 付款状态：0-未付款，1-已付款
     */
    public int payStatus;
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
    public static WFChargeStationProjectFinancialsEntity getInstance() {
        return new WFChargeStationProjectFinancialsEntity();
    }
}