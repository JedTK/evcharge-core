package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 品牌-类目 n-n 可通过品牌查询有哪些类目，或者通过类目查询有;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuBrandToCategoryEntity extends BaseEntity implements Serializable {
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
     * 品牌ID
     */
    public long brand_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpuBrandToCategoryEntity getInstance() {
        return new zSpuBrandToCategoryEntity();
    }
}
