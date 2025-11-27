package com.evcharge.entity.agent.company;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公司开票记录;null
 *
 * @author : Jay
 * @date : 2025-11-17
 */
@TargetDB("evcharge_agent")
public class CompanyInvoiceRecordEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 提现订单号,;
     */
    public String withdraw_order_no;
    /**
     * 公司代码,;
     */
    public String company_code;
    /**
     * 代理组织代码,;
     */
    public String agent_org_code;
    /**
     * 发票编号,;
     */
    public String invoice_no;
    /**
     * 发票图片,;
     */
    public String invoice_image;
    /**
     * 发票税点,;
     */
    public BigDecimal invoice_tax_rate;
    /**
     * 开票金额,;
     */
    public BigDecimal invoice_amount;
    /**
     * 开票时间,;
     */
    public long invoice_time;
    /**
     * 开票日期,;
     */
    public String invoice_date;
    /**
     * 开票月份 (如 2025-11),;
     */
    public String invoice_month;
    /**
     * 备注,;
     */
    public String remark;
    /**
     * 状态 -1=驳回 1=待审核 2=审核中 3=审核通过
     */
    public int status;
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
    public static CompanyInvoiceRecordEntity getInstance() {
        return new CompanyInvoiceRecordEntity();
    }
}