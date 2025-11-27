package com.evcharge.strategy.ConsumeCenter.Payment.Refund;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.SyncResult;

import java.math.BigDecimal;

public interface OrderRefundService {
    /**
     * 获取此服务处理的产品类型
     */
    String getProductType();

    /**
     * 校验该订单是否可退款
     * @param order 待退款的订单
     * @return SyncResult
     */
    SyncResult isRefundable(ConsumeOrdersEntity order, BigDecimal refundAmount);

    /**
     * 处理退款业务
     * @param order 订单实体类
     * @param refundAmount 退款金额
     * @param refundReason 退款理由
     * @return SyncResult
     */
    SyncResult dealRefund(ConsumeOrdersEntity order, BigDecimal refundAmount,String refundReason);


}
