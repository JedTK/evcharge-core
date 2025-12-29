package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.libsdk.aliyun.AliPaymentSDK;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.respon.PaymentRefundResponse;
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
    public SyncResult refund(ConsumeOrdersEntity consumeOrderRefundsEntity,String refundOrderSn, BigDecimal refundAmount, String reason) {
        AliPaymentSDK aliPaymentSDK = new AliPaymentSDK();
        SyncResult r = aliPaymentSDK.refund(consumeOrderRefundsEntity.order_sn, consumeOrderRefundsEntity.pay_order_sn, refundAmount.doubleValue(), reason);
        if (r.code != 0) {
            return r;
        }
        JSONObject jsonObject = (JSONObject) r.data;
        PaymentRefundResponse response = new PaymentRefundResponse();
        response.refund_bank_order_no = "";
        response.refund_bank_trx_no = "";
        return new SyncResult(0,"success",response);
    }
}
