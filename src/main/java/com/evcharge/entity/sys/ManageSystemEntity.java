package com.evcharge.entity.sys;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 管理系统;
 *
 * @author : JED
 * @date : 2022-12-29
 */
public class ManageSystemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 系统名称
     */
    public String sysName;
    /**
     * 系统编码
     */
    public String sysCode;
    /**
     * 上级ID
     */
    public long parent_id;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ManageSystemEntity getInstance() {
        return new ManageSystemEntity();
    }
}
