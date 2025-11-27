package com.evcharge.service.Agent.company;

import com.evcharge.entity.agent.company.CompanyInvoiceRecordEntity;
import com.evcharge.entity.agent.company.CompanyMonthSummaryV1Entity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.checkerframework.checker.units.qual.Time;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyInvoiceRecordService {

    public CompanyInvoiceRecordEntity getRecordById(Long recordId) {
        return CompanyInvoiceRecordEntity.getInstance().where("id", recordId).findEntity();
    }

    /**
     * 检查发票号是否存在
     *
     * @param invoiceCode String
     * @return boolean
     */
    public boolean checkInvoiceCodeExist(String invoiceCode) {
        int count = CompanyInvoiceRecordEntity.getInstance()
                .where("invoice_no", invoiceCode)
                .whereIn("status", "1,2,3") //状态 -1=驳回 1=待审核 2=审核中 3=审核通过
                .count();
        return count > 0;
    }


    public BigDecimal getOrderInvoiceAmount(String orderSn) {
        return CompanyInvoiceRecordEntity.getInstance().where("withdraw_order_no", orderSn)
                .where("status", 1)
                .sumGetBigDecimal("invoice_amount");
    }


    /**
     * 批量更新开票状态
     *
     * @param orderSn
     * @param status
     * @return
     */
    public SyncResult updateOrderInvoiceStatus(String orderSn, int status) {
        return DataService.getDB("evcharge_agent").beginTransaction(connection -> updateOrderInvoiceStatus(connection, orderSn, status));
    }


    public SyncResult updateOrderInvoiceStatus(Connection connection, String orderSn, int status) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("status", status);
            map.put("update_time", TimeUtil.getTimestamp());


            long res = CompanyInvoiceRecordEntity.getInstance().where("withdraw_order_no", orderSn)
                    .updateTransaction(connection, map);

            if (res == 0) return new SyncResult(1, "数据更新失败");

            return new SyncResult(0, "数据更新成功");
        } catch (Exception e) {
            return new SyncResult(1, "数据更新异常，异常原因：" + e.getMessage());
        }
    }

    public SyncResult updateOrderInvoiceAmount(String orderSn) {
        return DataService.getDB("evcharge_agent").beginTransaction(connection -> updateOrderInvoiceAmount(connection, orderSn));
    }

    public SyncResult updateOrderInvoiceAmount(Connection connection, String orderSn) {


        try {
            List<CompanyInvoiceRecordEntity> list = CompanyInvoiceRecordEntity.getInstance()
                    .where("withdraw_order_no", orderSn)
                    .selectList();

            if (list.isEmpty()) return new SyncResult(1, "数据不存在");


            for (CompanyInvoiceRecordEntity companyInvoiceRecordEntity : list) {
                String monthDate = TimeUtil.toTimeString(companyInvoiceRecordEntity.create_time, "yyyy-MM");
                CompanyMonthSummaryV1Entity companyMonthSummaryV1Entity = CompanyMonthSummaryV1Entity.getInstance()
                        .where("date", monthDate)
                        .where("company_code", companyInvoiceRecordEntity.company_code)
                        .findEntity();

                if (companyMonthSummaryV1Entity == null) continue;
                Map<String, Object> map = new HashMap<>();

                BigDecimal amountInvoicing = companyMonthSummaryV1Entity.amount_invoicing.subtract(companyInvoiceRecordEntity.invoice_amount);
                BigDecimal amountPending = companyMonthSummaryV1Entity.amount_pending.add(companyInvoiceRecordEntity.invoice_amount);

                map.put("amount_invoicing", amountInvoicing);
                map.put("amount_pending", amountPending);

                long res = CompanyMonthSummaryV1Entity.getInstance()
                        .where("id", companyMonthSummaryV1Entity.id)
                        .updateTransaction(connection, map);

                if (res == 0) return new SyncResult(1, "更新失败");
//                return new SyncResult(0, "更新成功");
            }

            return new SyncResult(0, "数据更新成功");
        } catch (Exception e) {
            return new SyncResult(1, "数据更新异常，异常原因：" + e.getMessage());
        }

    }


}
