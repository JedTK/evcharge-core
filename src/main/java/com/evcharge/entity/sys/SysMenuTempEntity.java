package com.evcharge.entity.sys;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 菜单模版;
 * @author : Jay
 * @date : 2024-5-28
 */
@TargetDB("evcharge_rbac")
public class SysMenuTempEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 菜单id
     */
    public String menu_ids ;
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
    public static SysMenuTempEntity getInstance() {
        return new SysMenuTempEntity();
    }
}