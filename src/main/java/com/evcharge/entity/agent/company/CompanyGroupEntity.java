package com.evcharge.entity.agent.company;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公司管理;null
 *
 * @author : Jay
 * @date : 2025-11-17
 */
@TargetDB("evcharge_agent")
public class CompanyGroupEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 公司名称,;
     */
    public String company_name;
    /**
     * 公司代码,;
     */
    public String company_code;
    /**
     * 公司地址,;
     */
    public String company_address;
    /**
     * 公司法人,;
     */
    public String legal_person;
    /**
     * 税务主体 1=一般纳税人 2=小规模纳税人,;
     */
    public int tax_subject;
    /**
     * 公司联系方式,;
     */
    public String contact_phone;
    /**
     * 公司开票信息,;
     */
    public String invoice_info;
    /**
     * 公司银行账户信息,;
     */
    public String bank_account_info;
    /**
     * 开票金额警戒线,;
     */
    public BigDecimal invoice_warning_amount;
    /**
     * 是否允许开票 1=允许 0=不允许,;
     */
    public int allow_invoice;
    /**
     * 状态 1=启用 0=禁用,;
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
    public static CompanyGroupEntity getInstance() {
        return new CompanyGroupEntity();
    }
}