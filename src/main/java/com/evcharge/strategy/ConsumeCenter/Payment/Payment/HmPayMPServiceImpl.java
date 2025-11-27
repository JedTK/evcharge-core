package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.enumdata.EUserRegType;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.respon.PaymentRefundResponse;
import com.evcharge.strategy.ConsumeCenter.Payment.config.PaymentCallbackConfig;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class HmPayMPServiceImpl implements PaymentService {
    @Override
    public String getPaymentMethodCode() {
        return "HmPay_MP"; // 返回我们在数据库中定义的唯一编码
    }


    @Override
    public SyncResult createPaymentRequest(ConsumeOrdersEntity order, String description) {
        HmPaymentSDK paymentSDK = new HmPaymentSDK();
        double totalPrice=order.order_price.doubleValue();
        return paymentSDK.create(
                EUserRegType.wechatId
                , order.uid
                ,order.order_sn
                ,description
                ,totalPrice
                , PaymentCallbackConfig.HMPayMP_Callback
        );
    }

    @Override
    public SyncResult refund(ConsumeOrdersEntity consumeOrderRefundsEntity,String refundOrderSn, BigDecimal refundAmount, String reason) {
        HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();

        SyncResult r= hmPaymentSDK.refund(
                consumeOrderRefundsEntity.order_sn
                ,consumeOrderRefundsEntity.pay_order_sn
                ,refundAmount.doubleValue()
                ,reason
        );
        if (r.code != 0) {
            return r;
        }
        JSONObject jsonObject = (JSONObject) r.data;
        PaymentRefundResponse response = new PaymentRefundResponse();

        response.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
        response.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");
        /**
         * 应该如何处理比较合适
         */
        return new SyncResult(0,"success",response);
    }

}
