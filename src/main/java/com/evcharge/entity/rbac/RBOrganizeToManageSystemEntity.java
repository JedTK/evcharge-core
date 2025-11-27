package com.evcharge.entity.rbac;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 组织 - 系统;
 *
 * @author : JED
 * @date : 2024-5-23
 */
public class RBOrganizeToManageSystemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 系统代码
     */
    public String sysCode;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static RBOrganizeToManageSystemEntity getInstance() {
        return new RBOrganizeToManageSystemEntity();
    }
}
