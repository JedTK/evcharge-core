package com.evcharge.service.Agent.agent;

import com.evcharge.entity.agent.agent.AgentWithdrawOrderEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;


@Service
public class AgentWithdrawOrderService {


    public static AgentWithdrawOrderService getInstance() {
        return new AgentWithdrawOrderService();
    }


    public AgentWithdrawOrderEntity getOrderInfo(String organizeCode, String orderSn) {

        return AgentWithdrawOrderEntity.getInstance()
                .where("ordersn", orderSn)
                .where("organize_code", organizeCode)
                .findEntity();


    }


    /**
     * 检查是否有在途订单
     *
     * @param organizeCode String
     * @return boolean
     */
    public boolean checkOrderInTransit(String organizeCode) {
        int count = AgentWithdrawOrderEntity.getInstance().where("organize_code", organizeCode)
                .whereIn("status", "1,2,3") //状态：-1=已取消 1=已提交，待上传发票，2=已上传发票，审核中 3=已审核，转账中，5=已转账 已完成 -2=拒绝
                .count();
        return count > 0;
    }


    public SyncResult updateOrderStatus(String orderSn, int status) {
        return DataService.getDB("evcharge_agent").beginTransaction(connection -> updateOrderStatusTransaction(connection, orderSn, status));
    }


    public SyncResult updateOrderStatusTransaction(Connection connection, String orderSn, int status) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", status);
        try {
            long res = AgentWithdrawOrderEntity.getInstance()
                    .where("ordersn", orderSn).updateTransaction(connection, map);

            if (res == 0) return new SyncResult(1, "更新订单状态失败");
            return new SyncResult(0, "更新订单状态成功");
        } catch (Exception e) {
            return new SyncResult(1, "更新订单状态异常，异常原因：" + e.getMessage());
        }


    }


}
