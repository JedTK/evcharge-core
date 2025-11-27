package com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Component;

@Component
public interface BeforePaymentService {

    /**
     * 获取此服务处理的产品类型
     */
    String getProductType();

    SyncResult beforePaymentCheck(ConsumeOrdersEntity consumeOrdersEntity,long productId);
}
