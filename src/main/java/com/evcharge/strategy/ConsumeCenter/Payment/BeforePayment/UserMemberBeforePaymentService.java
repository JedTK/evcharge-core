package com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

@Service
public class UserMemberBeforePaymentService implements BeforePaymentService {
    @Override
    public String getProductType() {
        return "user_member";
    }

    @Override
    public SyncResult beforePaymentCheck(ConsumeOrdersEntity consumeOrdersEntity,long productId) {
        return new SyncResult(0, "");
    }
}
