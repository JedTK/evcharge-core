package com.evcharge.entity.admin;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 管理员对应区域的权限;
 * @author : Jay
 * @date : 2025-8-12
 */
@TargetDB("evcharge_rbac")
public class AdminToRegionLevelEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long admin_id ;
    /**
     * 省份权限
     */
    public int province_permission ;
    /**
     * 城市权限
     */
    public int city_permission ;
    /**
     * 地区权限
     */
    public int district_permission ;
    /**
     * 街道权限
     */
    public int street_permission ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static AdminToRegionLevelEntity getInstance() {
        return new AdminToRegionLevelEntity();
    }
}