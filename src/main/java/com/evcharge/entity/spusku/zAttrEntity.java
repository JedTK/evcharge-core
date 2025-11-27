package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 属性;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zAttrEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 属性名
     */
    public String attrName;
    /**
     * 父级ID
     */
    public long parent_id;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 类型，1=单选，2=文本输入
     */
    public int typeId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zAttrEntity getInstance() {
        return new zAttrEntity();
    }
}
