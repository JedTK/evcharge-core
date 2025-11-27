package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 属性值1-n（表示一个规格拥有对应的值）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zAttrValueEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 属性ID
     */
    public long attr_id;
    /**
     * 属性值
     */
    public String attrValue;
    /**
     * 排序索引
     */
    public int sort_index;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zAttrValueEntity getInstance() {
        return new zAttrValueEntity();
    }
}