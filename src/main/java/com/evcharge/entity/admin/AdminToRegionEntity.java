package com.evcharge.entity.admin;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 管理员对应省市区街道表;
 * @author : Jay
 * @date : 2025-7-23
 */
@TargetDB("evcharge_rbac")
public class AdminToRegionEntity extends BaseEntity implements Serializable{
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
     * 组织代码
     */
    public String organize_code ;
    /**
     * 省份
     */
    public String province ;
    /**
     * 城市
     */
    public String city ;
    /**
     * 地区
     */
    public String district ;
    /**
     * 街道
     */
    public String street ;
    /**
     * 省份代码
     */
    public String province_code ;
    /**
     * 城市代码
     */
    public String city_code ;
    /**
     * 地区代码
     */
    public String district_code ;
    /**
     * 街道代码
     */
    public String street_code ;
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
    public static AdminToRegionEntity getInstance() {
        return new AdminToRegionEntity();
    }


    







}