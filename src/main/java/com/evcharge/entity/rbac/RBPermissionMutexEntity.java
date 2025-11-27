package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * RBAC权限互斥表;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBPermissionMutexEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 权限1
     */
    public long permission_id1;
    /**
     * 权限2
     */
    public long permission_id2;

    //endregion
}
