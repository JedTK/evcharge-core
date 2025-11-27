package com.evcharge.service.Agent.company;

import com.evcharge.entity.agent.company.CompanyGroupEntity;
import com.evcharge.entity.agent.company.CompanyMonthSummaryV1Entity;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyGroupService {


    public CompanyGroupEntity getCompanyInfoByCode(String code) {
        return CompanyGroupEntity.getInstance()
                .cache(String.format("Agent:CompanyGroup:%s", code))
                .where("company_code", code).findEntity();
    }


    //任务触发公司汇总表
    public void createMonthSummaryTask() {
        List<CompanyGroupEntity> list = CompanyGroupEntity.getInstance()
                .order("id asc")
                .selectList();
        if (list.isEmpty()) {
            return;
        }
        for (CompanyGroupEntity companyGroupEntity : list) {
            createMonthSummaryByManual(companyGroupEntity.company_code);
        }
        return;
    }

    //手动出发公司汇总表
    public void createMonthSummaryByManual(String code) {
        CompanyGroupEntity companyGroupEntity = getCompanyInfoByCode(code);
        if (companyGroupEntity == null) return;

        Map<String, Object> data = new HashMap<String, Object>();
        long monthTime = TimeUtil.getMonthBegin();
        //判断是否存在这个日期的
        int count = CompanyMonthSummaryV1Entity.getInstance()
                .where("company_code", companyGroupEntity.company_code)
                .where("date_time", monthTime)
                .count();

        if (count > 0) {
            return;
        }

        data.put("company_id", companyGroupEntity.id);
        data.put("company_code", companyGroupEntity.company_code);
        data.put("tax_subject", companyGroupEntity.tax_subject);
        data.put("amount_available", companyGroupEntity.invoice_warning_amount);
        data.put("amount_pending", companyGroupEntity.invoice_warning_amount);
        data.put("date_time", monthTime);
        data.put("date", TimeUtil.toTimeString(monthTime, "yyyy-MM"));
        data.put("create_time", TimeUtil.getTimestamp());

        CompanyMonthSummaryV1Entity.getInstance().insert(data);


    }


}
