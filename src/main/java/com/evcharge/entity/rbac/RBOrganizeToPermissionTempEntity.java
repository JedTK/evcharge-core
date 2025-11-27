package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * RBAC组织基础信息;组织权限一对一;
 * @author : Jay
 * @date : 2024-6-4
 */
@TargetDB("evcharge_rbac")
public class RBOrganizeToPermissionTempEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 组织id
     */
    public long organize_id ;
    /**
     * 组织代码
     */
    public String organize_code ;
    /**
     * 菜单模版id
     */
    public long permission_temp_id ;
    /**
     * 菜单模版标题
     */
    public String permission_temp_title ;
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
    public static RBOrganizeToPermissionTempEntity getInstance() {
        return new RBOrganizeToPermissionTempEntity();
    }
}