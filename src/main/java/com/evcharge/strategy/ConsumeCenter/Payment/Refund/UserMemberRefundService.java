package com.evcharge.strategy.ConsumeCenter.Payment.Refund;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.service.User.UserMemberService;
import com.xyzs.entity.SyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserMemberRefundService  implements OrderRefundService{


    @Autowired
    private UserMemberService userMemberService;

    @Override
    public String getProductType() {
        return "user_member";
    }

    @Override
    public SyncResult isRefundable(ConsumeOrdersEntity ordersEntity, BigDecimal refundAmount) {
        return new SyncResult(0,"");
    }

    @Override
    public SyncResult dealRefund(ConsumeOrdersEntity order, BigDecimal refundAmount, String refundReason) {
         userMemberService.cancelMembership(order.uid);
         return new SyncResult(0,"");
    }
}
