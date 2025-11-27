package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：采购订单商品;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class ePurchaseOrderGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 采购订单ID
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
     * 仓库id
     */
    public long storehouse_id;
    /**
     * 数量
     */
    public int count;
    /**
     * 采购单价
     */
    public double purchasePrice;
    /**
     * 总价
     */
    public double totalAmount;
    /**
     * 物流公司，可选
     */
    public String deliveryCompany;
    /**
     * 物流运单号，可选
     */
    public String deliveryOrderSN;
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
    public static ePurchaseOrderGoodsEntity getInstance() {
        return new ePurchaseOrderGoodsEntity();
    }
}
