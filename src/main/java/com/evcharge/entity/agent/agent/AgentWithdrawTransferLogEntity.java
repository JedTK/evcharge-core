package com.evcharge.entity.agent.agent;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 提现转账流水表（支持多卡多银行拆分转账）;null
 *
 * @author : Jay
 * @date : 2025-11-18
 */
public class AgentWithdrawTransferLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键ID,;
     */
    public long id;
    /**
     * 提现订单编号（外键关联 withdraw_order.order_no）,;
     */
    public String order_sn;
    /**
     * 付款银行名称（平台用于转账的银行）,;
     */
    public String payer_bank_name;
    /**
     * 付款银行代码,;
     */
    public String payer_bank_code;
    /**
     * 付款账户名称（公司户名）,;
     */
    public String payer_account_name;
    /**
     * 付款银行账号（平台银行卡，脱敏/加密存储）,;
     */
    public String payer_account_no;
    /**
     * 收款人姓名,;
     */
    public String receiver_account_name;
    /**
     * 收款人银行卡号,;
     */
    public String receiver_account_no;
    /**
     * 收款银行名称,;
     */
    public String receiver_bank_name;
    /**
     * 收款银行代码,;
     */
    public String receiver_bank_code;
    /**
     * 银行转账流水号,;
     */
    public String transfer_no;
    /**
     * 本笔转账金额,;
     */
    public BigDecimal amount;
    /**
     * 实际转账时间,;
     */
    public long transfer_time;
    /**
     * 转账凭证（回单图片路径）,;
     */
    public String attachment;
    /**
     * 录入人,;
     */
    public long operator;
    /**
     * 备注,;
     */
    public String remark;
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
    public static AgentWithdrawTransferLogEntity getInstance() {
        return new AgentWithdrawTransferLogEntity();
    }
}