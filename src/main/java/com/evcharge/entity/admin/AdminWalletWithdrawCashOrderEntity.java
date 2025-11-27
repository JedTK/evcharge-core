package com.evcharge.entity.admin;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理员钱包提现订单;
 *
 * @author : JED
 * @date : 2023-1-17
 */
public class AdminWalletWithdrawCashOrderEntity extends BaseEntity implements Serializable {
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
     * 组织ID
     */
    public long organize_id;
    /**
     * 申请人id
     */
    public long apply_admin_id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 提现的金额
     */
    public BigDecimal amount;
    /**
     * 手续费
     */
    public BigDecimal fee;
    /**
     * 实际到账金额
     */
    public BigDecimal receiveAmount;
    /**
     * 提现模式id
     */
    public long wm_config_id;
    /**
     * 状态：-1-删除，0-取消，1-待审核，2-审核通过，3-已完成
     */
    public int status;
    /**
     * 转账状态：0-待转账，1-已转账
     */
    public int transfer_status;
    /**
     * 发票状态：0-待开票，1-待确认，2-已开票，3-开票异常
     */
    public int invoice_status;
    /**
     * (可选)银行流水订单号
     */
    public String bankOrderSN;
    /**
     * 付款人姓名
     */
    public String payerName;
    /**
     * 付款人银行卡卡号
     */
    public String payerBankCardNumber;
    /**
     * 付款人关联的银行ID
     */
    public long payer_bank_id;
    /**
     * 付款人关联的银行支行ID
     */
    public long payer_bank_branch_id;
    /**
     * 收款人姓名
     */
    public String payeeName;
    /**
     * 收款人银行卡卡号
     */
    public String payeeBankCardNumber;
    /**
     * 收款人关联的银行ID
     */
    public long payee_bank_id;
    /**
     * 收款人关联的银行支行ID
     */
    public long payee_bank_branch_id;
    /**
     * 描述
     */
    public String desc;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 转账时间戳
     */
    public long transfer_time;
    /**
     * 开票时间戳
     */
    public long invoice_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminWalletWithdrawCashOrderEntity getInstance() {
        return new AdminWalletWithdrawCashOrderEntity();
    }
}
