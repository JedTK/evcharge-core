package com.evcharge.strategy.ConsumeCenter.Payment.Refund;

import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.service.User.UserSummaryService;
import com.xyzs.entity.SyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RechargeOrderRefundService implements OrderRefundService {

    @Autowired
    private UserSummaryService userSummaryService;

    @Override
    public String getProductType() {
        return "recharge";
    }

    @Override
    public SyncResult isRefundable(ConsumeOrdersEntity ordersEntity, BigDecimal refundAmount) {
        // 伪代码：从用户服务中获取充值后的消费情况
        // 校验充值金额是否已被使用
        // ...
        refundAmount = refundAmount.abs().multiply(new BigDecimal(-1)); // 最后乘以-1 将退款变为负数
        //状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
        BigDecimal totalRefundAmount = ConsumeOrderRefundsEntity.getInstance()
                .where("order_sn", ordersEntity.order_sn)
                .where("status", "SUCCESS")
                .sumGetBigDecimal("refund_amount");

        if (refundAmount.add(totalRefundAmount).abs().compareTo(ordersEntity.pay_price) > 0) {
            return new SyncResult(8, "退款金额不能大于原支付金额");
        }

        return new SyncResult(0, ""); // 如果已被使用则返回 false
    }

    public SyncResult dealRefund(ConsumeOrdersEntity order, BigDecimal refundAmount, String refundReason) {
        refundAmount = refundAmount.abs().multiply(new BigDecimal(-1));
        return userSummaryService.updateBalance(
                order.uid
                , refundAmount.doubleValue()
                , "refund"
                ,"充值退款"
                , order.order_sn
        );
    }
}
