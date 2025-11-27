package com.evcharge.service.Agent.agent;

import com.evcharge.entity.agent.summary.AgentDailyIncomeV1Entity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentIncomeService {
    public static AgentIncomeService getInstance() {
        return new AgentIncomeService();
    }
    public BigDecimal getWithdrawIncome(String organizeCode) {
        long currentTime = TimeUtil.getTime00(-1);
        Map<String, Object> totalConsumeData = AgentDailyIncomeV1Entity.getInstance()
                .field("IFNULL(SUM(split_amount),0) AS split_amount")
                .where("organize_code", organizeCode)
                .where("date_time", "<=", currentTime)
                .where("status", 0)
                .find();

        return MapUtil.getBigDecimal(totalConsumeData, "split_amount", 4, RoundingMode.HALF_UP);
    }
    /**
     * 获取提现明细表
     *
     * @param organizeCode String
     * @return
     */
    public List<AgentDailyIncomeV1Entity> getWithdrawIncomeDetail(String organizeCode) {
        long currentTime = TimeUtil.getTime00(-1);
        return AgentDailyIncomeV1Entity.getInstance()
                .field("*")
                .where("organize_code", organizeCode)
                .where("date_time", "<=", currentTime)
                .where("status", 0)
                .selectList();

    }
    /**
     * 获取提现明细表
     *
     * @param organizeCode String
     * @return
     */
    public List<AgentDailyIncomeV1Entity> getWithdrawIncomeDetailByOrderSn(String organizeCode, String orderSn) {

        return AgentDailyIncomeV1Entity.getInstance()
                .field("*")
                .where("organize_code", organizeCode)
                .where("withdraw_order_sn", orderSn)
                .selectList();

    }
    /**
     * 批量冻结收益
     *
     * @param organizeCode String
     * @param orderSn      String
     */
    public void batchFreezeIncome(String organizeCode, String orderSn) {

        List<AgentDailyIncomeV1Entity> list = getWithdrawIncomeDetail(organizeCode);

        if (list.isEmpty()) return;

        for (AgentDailyIncomeV1Entity nd : list) {
            Map<String, Object> data = new LinkedHashMap<>();

            data.put("status", 1);
            data.put("withdraw_order_sn", orderSn);
            data.put("update_time", TimeUtil.getTimestamp());

            AgentDailyIncomeV1Entity.getInstance()
                    .where("id", nd.id)
                    .update(data);

        }

    }
    /**
     * 批量处理收益
     *
     * @param organizeCode String
     * @param orderSn      String
     * @param status       int 状态 0=未提现 1=冻结中 2=已提现
     */
    public void batchHandleIncome(String organizeCode, String orderSn, int status) {

        List<AgentDailyIncomeV1Entity> list = getWithdrawIncomeDetailByOrderSn(organizeCode, orderSn);

        if (list.isEmpty()) return;

        for (AgentDailyIncomeV1Entity nd : list) {
            Map<String, Object> data = new LinkedHashMap<>();

            data.put("status", status);
            data.put("update_time", TimeUtil.getTimestamp());
            if (status == 0) {
                data.put("withdraw_order_sn", "");
            }
            AgentDailyIncomeV1Entity.getInstance()
                    .where("id", nd.id)
                    .update(data);

        }

    }


}
