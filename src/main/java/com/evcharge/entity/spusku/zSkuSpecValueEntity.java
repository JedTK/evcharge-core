package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * SKU规格值 n-n;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSkuSpecValueEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * sku ID
     */
    public long sku_id;
    /**
     * 规格值ID
     */
    public long spec_value_id;
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
    public static zSkuSpecValueEntity getInstance() {
        return new zSkuSpecValueEntity();
    }
}
