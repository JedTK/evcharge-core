package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * RBAC角色;直接与权限关联，例如：超级管理员，管理员
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBRoleEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 名称
     */
    public String name;
    /**
     * 编码
     */
    public String code;
    /**
     * 上级ID
     */
    public long parent_id;
    /**
     * 排序索引
     */
    public long sort_index;
    /**
     * 备注
     */
    public String remark;

    //endregion

    public static RBRoleEntity getInstance() {
        return new RBRoleEntity();
    }

    /**
     * 通过角色编码获取角色详细信息
     *
     * @param code
     * @return
     */
    public RBRoleEntity getByCode(String code) {
        return this.cache(String.format("BaseData:RBRole:%s", code))
                .where("code", code)
                .findEntity();
    }

    /**
     * 通过角色编码获取角色名字
     *
     * @param code         角色编码
     * @param defaultValue 默认值
     * @return
     */
    public String getNameByCode(String code, String defaultValue) {
        RBRoleEntity entity = getByCode(code);
        if (entity == null) return defaultValue;
        return entity.name;
    }
}
