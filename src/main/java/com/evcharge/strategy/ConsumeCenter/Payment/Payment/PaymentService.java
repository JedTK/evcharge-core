package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.SyncResult;

import java.math.BigDecimal;

public interface PaymentService {
    /**
     * 获取支付方式编码，例如 "ALIPAY_MP", "WECHAT_MP"
     */
    String getPaymentMethodCode();

    /**
     * 根据订单信息生成支付请求参数
     */
    SyncResult createPaymentRequest(ConsumeOrdersEntity order, String description);


    SyncResult refund(ConsumeOrdersEntity order,String refundOrderSn, BigDecimal refundAmount, String reason);

}
