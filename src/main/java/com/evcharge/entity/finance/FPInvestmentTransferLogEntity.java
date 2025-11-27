package com.evcharge.entity.finance;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 金融产品 - 投资加盟 - 汇款记录;
 *
 * @author : JED
 * @date : 2022-11-22
 */
public class FPInvestmentTransferLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期时间戳，精确到月
     */
    public long date_month;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 状态：0=待转账，1=已转账，2=转账延迟，3=转账异常
     */
    public int status;
    /**
     * 当月所有订单总收益
     */
    public double totalIncomeAmount;
    /**
     * 转账金额
     */
    public double transferAmount;
    /**
     * 收款人姓名
     */
    public String payeeName;
    /**
     * 收款银行卡卡号
     */
    public String payeeBankCardNumber;
    /**
     * 收款关联的银行ID
     */
    public long payeeBankId;
    /**
     * 付款人姓名
     */
    public String payerName;
    /**
     * 付款付款银行卡卡号
     */
    public String payerBankCardNumber;
    /**
     * 付款关联的银行ID
     */
    public long payerBankId;
    /**
     * 管理员ID
     */
    public long adminId;
    /**
     * 转账时间戳
     */
    public long transferTime;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static FPInvestmentTransferLogEntity getInstance() {
        return new FPInvestmentTransferLogEntity();
    }
}