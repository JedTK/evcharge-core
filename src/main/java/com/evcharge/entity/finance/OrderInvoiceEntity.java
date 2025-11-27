package com.evcharge.entity.finance;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充值订单发票管理;
 * @author : Jay
 * @date : 2024-2-29
 */
public class OrderInvoiceEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 订单类型 1=充值订单 2=优惠卡订单
     */
    public int order_type ;
    /**
     * 发票抬头
     */
    public String invoice_header ;
    /**
     * 发票电子档
     */
    public String invoice_pic ;
    /**
     * 发票金额
     */
    public double invoice_price ;
    /**
     * 税号
     */
    public String tax ;
    /**
     * 手机号
     */
    public String phone ;
    /**
     * 邮箱
     */
    public String email ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static OrderInvoiceEntity getInstance() {
        return new OrderInvoiceEntity();
    }
}