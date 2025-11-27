package com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment;

import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.service.ConsumeCenter.ConsumeOrderItemsService;
import com.xyzs.entity.SyncResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChargeCardBeforePaymentService implements BeforePaymentService {

    @Autowired
    private ConsumeOrderItemsService consumeOrderItemsService;

    @Override
    public String getProductType() {
        return "charge_card";
    }


    @Override
    public SyncResult beforePaymentCheck(ConsumeOrdersEntity consumeOrdersEntity,long productId) {
        String CSId=consumeOrdersEntity.CSId;
//        ConsumeOrderItemsEntity consumeOrderItemsEntity = consumeOrderItemsService.getItemsByOrderId(consumeOrdersEntity.id);
//
//        if(consumeOrderItemsEntity==null){
//            return new SyncResult(1,"无效产品id");
//        }


        if (!StringUtils.hasLength(CSId) || "0".equals(CSId)) return new SyncResult(2, "无效充电桩");
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(CSId, false);
        if (chargeStationEntity == null) {
            return new SyncResult(2, "无效充电桩");
        }
        ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithProductId(productId);
        if (chargeCardConfigEntity == null || chargeCardConfigEntity.id == 0) {
            return new SyncResult(101, "充电卡配置错误");
        }
        //检查用户是否允许购买充电卡
        SyncResult checkResult = UserChargeCardEntity.getInstance().purchaseCheck(consumeOrdersEntity.uid, chargeStationEntity, chargeCardConfigEntity, false);
        if (checkResult.code != 0) return checkResult;

        return new SyncResult(0,"success");
    }
}
