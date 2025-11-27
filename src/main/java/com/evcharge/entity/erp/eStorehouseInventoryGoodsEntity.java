package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库盘点商品;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseInventoryGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 盘点订单ID
     */
    public long order_id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * sku编码
     */
    public String sku_code;
    /**
     * 系统库存
     */
    public int sysStock;
    /**
     * 盘点库存
     */
    public int InventoryStock;
    /**
     * 备注
     */
    public String remark;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static eStorehouseInventoryGoodsEntity getInstance() {
        return new eStorehouseInventoryGoodsEntity();
    }
}
