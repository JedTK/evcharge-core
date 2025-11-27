package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合伙人：采购订单商品;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerPurchaseOrderGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单ID
     */
    public long order_id;
    /**
     * 标题，方便查询
     */
    public String title;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * sku编码
     */
    public String sku_code;
    /**
     * 数量
     */
    public int count;
    /**
     * 单价
     */
    public double price;
    /**
     * 总价
     */
    public double totalAmount;
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
    public static PartnerPurchaseOrderGoodsEntity getInstance() {
        return new PartnerPurchaseOrderGoodsEntity();
    }
}
