package com.evcharge.entity.rbac;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 权限模版;
 * @author : Jay
 * @date : 2024-6-4
 */
@TargetDB("evcharge_rbac")
public class RBPermissionTempEntity extends BaseEntity implements Serializable{
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
     * 权限id
     */
    public String permission_ids ;
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
    public static RBPermissionTempEntity getInstance() {
        return new RBPermissionTempEntity();
    }
}