package com.evcharge.entity.spusku;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 类目属性n-n 通过关联数据生成属性JSON数据;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zCategoryAttrEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 分类ID
     */
    public long category_id;
    /**
     * 规格ID
     */
    public long attr_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zCategoryAttrEntity getInstance() {
        return new zCategoryAttrEntity();
    }
}
