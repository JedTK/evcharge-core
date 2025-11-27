package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库出入库记录商品;
 *
 * @author : JED
 * @date : 2023-1-10
 */
public class eStorehouseLogsGoodsEntity extends BaseEntity implements Serializable {
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
     * (可选)单价，有就填
     */
    public double price;
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
    public static eStorehouseLogsGoodsEntity getInstance() {
        return new eStorehouseLogsGoodsEntity();
    }
}
