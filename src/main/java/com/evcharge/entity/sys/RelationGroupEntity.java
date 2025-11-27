package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 关系分组;
 *
 * @author : JED
 * @date : 2022-11-21
 */
public class RelationGroupEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 代码
     */
    public String code;
    /**
     * 关系名
     */
    public String relationName;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static RelationGroupEntity getInstance() {
        return new RelationGroupEntity();
    }
}
