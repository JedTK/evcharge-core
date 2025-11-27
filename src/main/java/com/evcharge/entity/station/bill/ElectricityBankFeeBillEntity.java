package com.evcharge.entity.station.bill;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * null;
 * @author : Jay
 * @date : 2024-2-20
 */
public class ElectricityBankFeeBillEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 电子回单号码
     */
    public String e_receipt_number ;
    /**
     * 付方账号
     */
    public String payer_account ;
    /**
     * 付方账户名称
     */
    public String payer_account_name ;
    /**
     * 付方开户银行名称
     */
    public String payer_bank_name ;
    /**
     * 收方账号
     */
    public String payee_account ;
    /**
     * 收方账户名称
     */
    public String payee_account_name ;
    /**
     * 收方开户银行名称
     */
    public String payee_bank_name ;
    /**
     * 收款金额
     */
    public double payment_amount ;
    /**
     * 币种
     */
    public String currency ;
    /**
     * 摘要
     */
    public String summary ;
    /**
     * 用途
     */
    public String purpose ;
    /**
     * 交易流水号
     */
    public String transaction_number ;
    /**
     * 创建时间
     */
    public long creation_time ;
    /**
     * 备注
     */
    public String remarks ;
    /**
     * 验证码
     */
    public String verification_code ;
    /**
     * 记账网点
     */
    public String accounting_branch ;
    /**
     * 记账柜员
     */
    public String accounting_clerk ;
    /**
     * 记账日期
     */
    public long accounting_date ;
    /**
     * 补打次数
     */
    public int reprint_times ;
    /**
     * 业务（产品）种类
     */
    public String business_type ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ElectricityBankFeeBillEntity getInstance() {
        return new ElectricityBankFeeBillEntity();
    }
}