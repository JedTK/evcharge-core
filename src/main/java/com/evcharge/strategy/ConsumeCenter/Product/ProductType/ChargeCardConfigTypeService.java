package com.evcharge.strategy.ConsumeCenter.Product.ProductType;


import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.recharge.RechargeConfigEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.SyncResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChargeCardConfigTypeService implements ProductTypeStrategyService {

    @Override
    public String getProductType() {
        return "charge_card";
    }

    @Override
    public List<Map<String, Object>> getConfigList() {
        return ChargeCardConfigEntity.getInstance()
                .cache(String.format("Consume:Product:Type:%s:Config:List", getProductType()), ECacheTime.DAY)
                .field("id,product_id,cardName as title")
                .select();
    }


    @Override
    public SyncResult updateProductIdForConfig(long productId, long configId) {
        ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance()
                .where("id", configId)
                .findEntity();

        if (chargeCardConfigEntity == null) return new SyncResult(1, "配置文件不存在");

        Map<String, Object> info = new HashMap<>();
        info.put("product_id", productId);
        long result = ChargeCardConfigEntity.getInstance().where("id", chargeCardConfigEntity.id).update(info);
        if (result == 0) return new SyncResult(1, "更新失败");

        return new SyncResult(0, "更新成功");

    }
}
