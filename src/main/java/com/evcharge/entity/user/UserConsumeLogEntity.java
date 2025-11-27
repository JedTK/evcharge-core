package com.evcharge.entity.user;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 消费记录表;
 * @author : Jay
 * @date : 2022-10-11
 */
public class UserConsumeLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户id
     */
    public long uid ;
    /**
     * 回调地址
     */
    public String notify_url ;
    /**
     * 商户订单号
     */
    public String pay_order_code ;
    /**
     * 交易单号
     */
    public String bank_serial ;
    /**
     * 订单编号
     */
    public String ordersn ;
    /**
     * 支付信息
     */
    public String content ;
    /**
     * ip地址
     */
    public String ip ;
    /**
     * 支付类型
     */
    public String paytype_id ;
    /**
     * 支付时间
     */
    public long pay_time ;
    /**
     * 支付金额
     */
    public BigDecimal pay_price ;
    /**
     * 订单类型
     */
    public String order_type ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 状态
     */
    public int status;
    /**
     * 备注
     */
    public String memo;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static UserConsumeLogEntity getInstance() {
        return new UserConsumeLogEntity();
    }
}
