package com.evcharge.entity.admin;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理员钱包余额日志;
 *
 * @author : JED
 * @date : 2022-12-22
 */
@Deprecated
public class AdminWalletLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long admin_id;
    /**
     * 订单号
     */
    public String OrderSN1;
    /**
     * 标题
     */
    public String title;
    /**
     * 类型：1=充值、2=提现、3=转账、4=支付、5=收益
     */
    public int typeId;
    /**
     * 变动的金额
     */
    public BigDecimal amount;
    /**
     * 描述
     */
    public String desc;
    /**
     * (可选)额外订单号
     */
    public String extraOrderSN;
    /**
     * (可选)扩展数据
     */
    public String extraData;
    /**
     * (可选)关联的类名
     */
    public String className;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminWalletLogEntity getInstance() {
        return new AdminWalletLogEntity();
    }
}