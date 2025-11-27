package com.evcharge.entity.active.abcbank;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * 农行充值活动订单;
 * @author : Jay
 * @date : 2025-3-21
 */
public class ABCBankActiveOrderEntity extends BaseEntity implements Serializable{
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
     * 用户手机号码 非授权登录时获取
     */
    public String phone ;
    /**
     * 活动配置id 关联 ABCBankActiveConfig 表
     */
    public long config_id ;
    /**
     * 订单编号
     */
    public String ordersn ;
    /**
     * tokenid
     */
    public String token_id ;
    /**
     * 金额
     */
    public BigDecimal price ;
    /**
     * 数量
     */
    public int amount ;
    /**
     * 支付金额
     */
    public BigDecimal pay_price ;
    /**
     * 回调报文
     */
    public String callback_content ;
    /**
     * 回调时间
     */
    public long callback_time ;
    /**
     * 平台代码
     */
    public String platform_code ;
    /**
     * 退款订单号
     */
    public String refund_order_sn ;
    /**
     * 退款流水号
     */
    public String refund_bank_order_no ;
    /**
     * 退款时间
     */
    public long refund_time ;
    /**
     * 退款报文
     */
    public String refund_content ;
    /**
     * 状态 1=待支付 2=已支付 -1=已取消
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
    public static ABCBankActiveOrderEntity getInstance() {
        return new ABCBankActiveOrderEntity();
    }





}