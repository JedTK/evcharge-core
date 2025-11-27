package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.enumdata.EUserRegType;
import com.evcharge.libsdk.wechat.WechatPaySDK;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.respon.PaymentRefundResponse;
import com.evcharge.strategy.ConsumeCenter.Payment.config.PaymentCallbackConfig;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WechatPayMPServiceImpl implements PaymentService {

    @Override
    public String getPaymentMethodCode() {
        return "WechatPay_MP"; // 返回我们在数据库中定义的唯一编码
    }


    @Override
    public SyncResult createPaymentRequest(ConsumeOrdersEntity order, String description) {
        WechatPaySDK wechatPaySDK = new WechatPaySDK();
        double totalPrice=order.order_price.doubleValue();

        return wechatPaySDK.create(
                EUserRegType.wechatId
                ,order.uid
                ,order.order_sn
                ,description
                ,totalPrice
                , PaymentCallbackConfig.WechatPayMP_Callback
        );
    }

    @Override
    public SyncResult refund(ConsumeOrdersEntity consumeOrderRefundsEntity, String refundOrderSn,BigDecimal refundAmount, String reason) {
        WechatPaySDK wechatPaySDK = new WechatPaySDK();


       SyncResult r = wechatPaySDK.refund(consumeOrderRefundsEntity.order_sn
               , refundOrderSn
               , refundAmount.doubleValue(), reason);

        if (r.code != 0) {
            return r;
        }

        JSONObject jsonObject = (JSONObject) r.data;
        PaymentRefundResponse response = new PaymentRefundResponse();

        /**
         * TODO 需要确认微信支付回调是不是也是这两个字段
         */
        response.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
        response.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");

        return new SyncResult(0,"success",response);
    }

}
