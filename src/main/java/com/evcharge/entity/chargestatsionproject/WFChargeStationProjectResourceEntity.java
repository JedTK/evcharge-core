package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目资源管理;
 *
 * @author : JED
 * @date : 2023-10-20
 */
public class WFChargeStationProjectResourceEntity extends BaseEntity implements Serializable {
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
     * 类型：场景-Scenes，效果图-Effect，公示-announcement，合同-contract，其他-other
     */
    public String type;
    /**
     * 文件名
     */
    public String fileName;
    /**
     * 文件链接
     */
    public String url;
    /**
     * 状态：0-删除，1-正常
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
    public static WFChargeStationProjectResourceEntity getInstance() {
        return new WFChargeStationProjectResourceEntity();
    }
}
