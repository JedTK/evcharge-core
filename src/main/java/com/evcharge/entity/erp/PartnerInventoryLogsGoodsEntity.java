package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合伙人：库存日志商品;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerInventoryLogsGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日志ID
     */
    public long logs_id;
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
    public static PartnerInventoryLogsGoodsEntity getInstance() {
        return new PartnerInventoryLogsGoodsEntity();
    }
}
