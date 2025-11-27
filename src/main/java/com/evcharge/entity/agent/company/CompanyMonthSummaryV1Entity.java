package com.evcharge.entity.agent.company;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公司月汇总表;null
 *
 * @author : Jay
 * @date : 2025-11-17
 */
@TargetDB("evcharge_agent")
public class CompanyMonthSummaryV1Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 公司id,;
     */
    public long company_id;
    /**
     * 公司代码,;
     */
    public String company_code;
    /**
     * 税务主体 1=一般纳税人 2=小规模纳税人,;
     */
    public int tax_subject;
    /**
     * 日期（当月任意日期或统一为1号）,;
     */
    public String date;
    /**
     * 日期毫秒级时间戳,;
     */
    public long date_time;
    /**
     * 月度可开票金额,;
     */
    public BigDecimal amount_available;
    /**
     * 已开票金额,;
     */
    public BigDecimal amount_invoiced;
    /**
     * 正在开票金额,;
     */
    public BigDecimal amount_invoicing;
    /**
     * 待开票金额,;
     */
    public BigDecimal amount_pending;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 结束时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static CompanyMonthSummaryV1Entity getInstance() {
        return new CompanyMonthSummaryV1Entity();
    }
}