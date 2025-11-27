package com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment;

import com.evcharge.entity.consumecenter.order.ConsumeOrderItemsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.user.member.MemberConfigEntity;
import com.evcharge.service.ConsumeCenter.ConsumeOrderItemsService;
import com.evcharge.service.User.UserMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserMemberFulfillmentService implements FulfillmentService {
    @Autowired
    private ConsumeOrderItemsService consumeOrderItemsService;

    @Autowired
    private UserMemberService userMemberService;

    @Override
    public String getProductType() {
        return "user_member"; // 与产品表中的 type 对应
    }

    @Override
    public void processFulfillment(ConsumeOrdersEntity order) {
        ConsumeOrderItemsEntity consumeOrderItemsEntity = consumeOrderItemsService.getItemsByOrderId(order.id);
        if (consumeOrderItemsEntity == null) return;
        MemberConfigEntity memberConfigEntity = MemberConfigEntity.getInstance().getInfoByProductId(consumeOrderItemsEntity.product_id);

        if (memberConfigEntity == null) return;
        userMemberService.purchaseMembership(order.uid, memberConfigEntity.id, order.order_sn);
        return;
    }

    public void rollbackFulfillment(ConsumeOrdersEntity order) {

    }
}
