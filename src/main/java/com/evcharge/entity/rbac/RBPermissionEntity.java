package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * RBAC权限基础;权限基础单位，与角色、菜单、操作、页面关联
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBPermissionEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 权限名称
     */
    public String name;
    /**
     * 权限编码
     */
    public String code;
    /**
     * 权限类型：M=MENU,O=OPERATE,P=PAGE
     */
    public int type_code;
    /**
     * 上级ID（树形结构时使用，父级实际只是用来显示，真正赋予权限是子级）
     */
    public long parent_id;
    /**
     * 排序
     */
    public long sort_index;
    /**
     * 备注
     */
    public String remark;

    //endregion

    public static RBPermissionEntity getInstance(){
        return new RBPermissionEntity();
    }
}
