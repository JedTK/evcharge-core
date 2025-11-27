package com.evcharge.strategy.ConsumeCenter.Payment.Refund;

import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Service
public class ChargeCardRefundService implements OrderRefundService {

    @Override
    public String getProductType() {
        return "charge_card";
    }

    @Override
    public SyncResult isRefundable(ConsumeOrdersEntity ordersEntity, BigDecimal refundAmount) {

        UserChargeCardEntity chargeCardEntity = UserChargeCardEntity.getInstance()
                .where("OrderSN", ordersEntity.order_sn)
                .findEntity();

        if (chargeCardEntity == null) return new SyncResult(1, "该订单没有优惠卡信息");

        return new SyncResult(0, "");
    }

    @Override
    public SyncResult dealRefund(ConsumeOrdersEntity ordersEntity, BigDecimal refundAmount, String refundReason) {

        UserChargeCardEntity chargeCardEntity = UserChargeCardEntity.getInstance()
                .where("OrderSN", ordersEntity.order_sn)
                .findEntity();

        int noQuery = chargeCardEntity.where("cardNumber", chargeCardEntity.cardNumber).update(new LinkedHashMap<>() {{
            put("end_time", chargeCardEntity.start_time);
            put("status", 0);
        }});
        DataService.getMainCache().del(String.format("User:%s:ChargeCard:Valid", ordersEntity.uid));

        return new SyncResult(0, "");
    }
}
