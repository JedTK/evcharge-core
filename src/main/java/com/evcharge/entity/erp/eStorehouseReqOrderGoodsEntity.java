package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库调拨订单商品;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseReqOrderGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 入库订单ID
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
     * 备注
     */
    public String remark;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static eStorehouseReqOrderGoodsEntity getInstance() {
        return new eStorehouseReqOrderGoodsEntity();
    }
}
