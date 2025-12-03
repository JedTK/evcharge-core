package com.evcharge.strategy.ConsumeCenter.Payment.PaymentCallback;

import com.xyzs.entity.SyncResult;

import javax.servlet.http.HttpServletRequest;

public interface PaymentCallbackService {

    /**
     * 获取当前服务所处理的支付渠道编码
     */
    String getChannelCode();



    String process(HttpServletRequest request);


    SyncResult confirmOrder(String orderSn);


}
