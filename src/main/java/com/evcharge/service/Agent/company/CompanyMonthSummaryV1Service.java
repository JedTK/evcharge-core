package com.evcharge.service.Agent.company;


import com.evcharge.entity.agent.agent.AgentToOrganizeEntity;
import com.evcharge.entity.agent.company.CompanyGroupEntity;
import com.evcharge.entity.agent.company.CompanyInvoiceRecordEntity;
import com.evcharge.entity.agent.company.CompanyMonthSummaryV1Entity;
import com.xyzs.utils.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyMonthSummaryV1Service {

    @Autowired
    private CompanyGroupService companyGroupService;

    /**
     * 检查公司表是否有足够的待开票金额
     *
     * @param withdrawalAmount 提现金额
     * @return Boolean
     */
    public Boolean checkCompanyGroupForWithdraw(BigDecimal withdrawalAmount) {
        long monthTime = TimeUtil.getMonthBegin();
        BigDecimal amountPending = CompanyMonthSummaryV1Entity.getInstance()
                .where("date_time", monthTime)
                .where("tax_subject", 2)
                .where("amount_pending", ">", 0)
                .sumGetBigDecimal("amount_pending");

        return amountPending.compareTo(withdrawalAmount) >= 0;

    }


    /**
     * 将提现金额分配到多个公司
     *
     * @param orderSn          订单编号
     * @param agentOrgCode     组织代码
     * @param withdrawalAmount 提现金额
     */
    public void allocateWithdrawalToCompanies(String orderSn, String agentOrgCode, BigDecimal withdrawalAmount) {
        AgentToOrganizeEntity agentToOrganizeEntity = AgentToOrganizeEntity.getInstance()
                .where("organize_code", agentOrgCode)
                .findEntity();

        if (agentToOrganizeEntity.tax_subject == 1) {
            String generalTaxpayerCompanyCode = "shuhuan";
            Map<String, Object> record = new HashMap<>();
            record.put("withdraw_order_no", orderSn);
            record.put("company_code", generalTaxpayerCompanyCode);
            record.put("agent_org_code", agentOrgCode);
            record.put("invoice_amount", withdrawalAmount);
            record.put("create_time", TimeUtil.getTimestamp());
            CompanyInvoiceRecordEntity.getInstance().insert(record);

            return;
        }


        long monthTime = TimeUtil.getMonthBegin();
        List<Map<String, Object>> compnaylist = new ArrayList<>();
        /**
         * 1、查找所有公司
         * 2、遍历公司
         */
        List<CompanyMonthSummaryV1Entity> list = CompanyMonthSummaryV1Entity.getInstance()
                .where("date_time", monthTime)
                .where("amount_pending", ">", 0)
                .selectList();

        if (list.isEmpty()) return;

        /**
         * amount_invoiced 已开票金额
         * amount_invoicing 正在开票金额
         * amount_pending 待开票金额
         */

        //剩余提现金额
        BigDecimal remainingWithdrawalAmount = withdrawalAmount;
        for (CompanyMonthSummaryV1Entity summary : list) {
            CompanyGroupEntity company = companyGroupService.getCompanyInfoByCode(summary.company_code);
            if (company == null || company.allow_invoice == 0) continue;

            //本次分摊金额 = min(提现剩余金额, 公司待开票金额)
            BigDecimal invoiceAmount = remainingWithdrawalAmount.min(summary.amount_pending);

            //插入记录
            Map<String, Object> record = new HashMap<>();
            record.put("withdraw_order_no", orderSn);
            record.put("company_code", summary.company_code);
            record.put("agent_org_code", agentOrgCode);
            record.put("invoice_amount", invoiceAmount);
            record.put("create_time", TimeUtil.getTimestamp());

            CompanyInvoiceRecordEntity.getInstance().insert(record);

            //更新 company_month_summary
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("amount_invoicing", summary.amount_invoicing.add(invoiceAmount));
            updateData.put("amount_pending", summary.amount_pending.subtract(invoiceAmount));

            CompanyMonthSummaryV1Entity.getInstance()
                    .where("id", summary.id)
                    .update(updateData);

            //减少剩余提现金额
            remainingWithdrawalAmount = remainingWithdrawalAmount.subtract(invoiceAmount);

            //结束条件
            if (remainingWithdrawalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

    }
}
