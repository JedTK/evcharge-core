package com.evcharge.entity.consumecenter.order;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细表;
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeOrderItemsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 订单明细ID,;
     */
    public long id;
    /**
     * 关联订单ID,;
     */
    public long order_id;
    /**
     * 订单编号,;
     */
    public String order_sn;
    /**
     * 关联产品ID,;
     */
    public long product_id;
    /**
     * 标题,;
     */
    public String title;
    /**
     * 主图,;
     */
    public String main_image;
    /**
     * 购买数量,;
     */
    public int quantity;
    /**
     * 商品单价,;
     */
    public BigDecimal price;
    /**
     * 存储业务特定数据，如赠送金额,;
     */
    public String details;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static ConsumeOrderItemsEntity getInstance() {
        return new ConsumeOrderItemsEntity();
    }
}