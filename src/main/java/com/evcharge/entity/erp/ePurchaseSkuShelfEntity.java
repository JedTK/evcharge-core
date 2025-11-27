package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 采购SKU货架;
 *
 * @author : JED
 * @date : 2023-1-29
 */
public class ePurchaseSkuShelfEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * sku编码
     */
    public String sku_code;
    /**
     * 状态：0-下架，1-上架
     */
    public int status;
    /**
     * 库存数量
     */
    public int stock;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ePurchaseSkuShelfEntity getInstance() {
        return new ePurchaseSkuShelfEntity();
    }
}
