package com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment;

import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderItemsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.service.ConsumeCenter.ConsumeOrderItemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChargeCardFulfillmentService implements FulfillmentService {

    @Autowired
    private ConsumeOrderItemsService consumeOrderItemsService;


    @Override
    public String getProductType() {
        return "charge_card"; // 与产品表中的 type 对应
    }

    @Override
    public void processFulfillment(ConsumeOrdersEntity consumeOrdersEntity) {
        ConsumeOrderItemsEntity consumeOrderItemsEntity = consumeOrderItemsService.getItemsByOrderId(consumeOrdersEntity.id);
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(consumeOrdersEntity.CSId, false);
        if (chargeStationEntity == null) {
            return;
        }
        ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithProductId(consumeOrderItemsEntity.product_id);
        if (chargeCardConfigEntity == null || chargeCardConfigEntity.id == 0) {
            return;
        }

        UserChargeCardEntity.getInstance().purchaseCallback(
                consumeOrdersEntity.uid
                ,consumeOrdersEntity.CSId
                ,chargeCardConfigEntity.id
                ,consumeOrdersEntity.order_sn
                ,""
        );
    }

    public void rollbackFulfillment(ConsumeOrdersEntity order) {

    }
}
