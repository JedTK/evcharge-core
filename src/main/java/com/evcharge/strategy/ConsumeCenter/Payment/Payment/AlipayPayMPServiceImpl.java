package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.libsdk.aliyun.AliPaymentSDK;
import com.evcharge.strategy.ConsumeCenter.Payment.config.PaymentCallbackConfig;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AlipayPayMPServiceImpl implements PaymentService {

    @Override
    public String getPaymentMethodCode() {
        return "ALIPAY_MP"; // 返回我们在数据库中定义的唯一编码
    }

    @Override
    public SyncResult createPaymentRequest(ConsumeOrdersEntity order, String description) {
        AliPaymentSDK aliPaymentSDK = new AliPaymentSDK();

        double totalPrice=order.order_price.doubleValue();

        return aliPaymentSDK.create(
                order.uid
                ,order.order_sn
                ,description
                ,totalPrice
                , PaymentCallbackConfig.AliPayMP_Callback
                ,null
        );

    }

    @Override
    public SyncResult refund(ConsumeOrdersEntity consumeOrdersEntity,String refundOrderSn, BigDecimal refundAmount, String reason) {
        return null;
    }
}
