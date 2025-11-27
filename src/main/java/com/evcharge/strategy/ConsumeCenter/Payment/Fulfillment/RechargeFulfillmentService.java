package com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment;

import com.evcharge.entity.consumecenter.order.ConsumeOrderItemsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.recharge.RechargeConfigEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.service.ConsumeCenter.ConsumeOrderItemsService;
import com.evcharge.service.ConsumeCenter.ConsumeProductsService;
import com.evcharge.service.User.UserService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RechargeFulfillmentService implements FulfillmentService {

    // 注入用户服务
    @Autowired
    private UserService userService;

    @Autowired
    private ConsumeProductsService consumeProductsService;

    @Autowired
    private ConsumeOrderItemsService consumeOrderItemsService;

    @Override
    public String getProductType() {
        return "recharge"; // 与产品表中的 type 对应
    }

    @Override
    public void processFulfillment(ConsumeOrdersEntity order) {
        // 从 order_items 的 details 字段获取充值和赠送金额
        // 假设已经解析出充值金额为 rechargeAmount，赠送金额为 bonusAmount
//        BigDecimal totalBalance = order.getPayPrice(); // 实际支付金额
//        // 更新用户余额
//        userService.addBalance(order.getUid(), totalBalance);
        // 如果有赠送金额，也加到余额中
        // userService.addBonus(order.getUid(), bonusAmount);
        //product_id

        ConsumeOrderItemsEntity consumeOrderItemsEntity = consumeOrderItemsService.getItemsByOrderId(order.id);

        if (consumeOrderItemsEntity == null) return;

//        ConsumeProductsEntity consumeProductsEntity=consumeProductsService.getProductById(consumeOrderItemsEntity.product_id);

        RechargeConfigEntity rechargeConfigEntity = RechargeConfigEntity.getInstance().getInfoByProductId(consumeOrderItemsEntity.product_id);


        if (rechargeConfigEntity == null) return;

        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            UserSummaryEntity.getInstance().updateBalanceTransaction(connection, order.uid, rechargeConfigEntity.balance, EBalanceUpdateType.recharge, "余额充值", order.order_sn);
            //如果有充值奖励
            if (rechargeConfigEntity.reward_balance > 0) {
                UserSummaryEntity.getInstance().updateBalanceTransaction(connection, order.uid, rechargeConfigEntity.reward_balance, rechargeConfigEntity.reward_sub_title, rechargeConfigEntity.reward_title, order.order_sn);
            }
            return new SyncResult(0, "success");
        });
        if (r.code > 0) {
            return;
        }
        DataService.getMainCache().del(String.format("RechargeOrder:%s", order.order_sn));

        System.out.println("订单 " + order.order_sn + " 充值成功，用户余额已更新。");
    }



    public void rollbackFulfillment(ConsumeOrdersEntity order) {

    }

}
