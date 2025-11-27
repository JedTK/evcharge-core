package com.evcharge.strategy.ConsumeCenter.Product.ProductType;


import com.evcharge.entity.recharge.RechargeConfigEntity;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RechargeConfigTypeService implements ProductTypeStrategyService {

    @Override
    public String getProductType() {
        return "recharge";
    }

    @Override
    public List<Map<String, Object>> getConfigList() {

        return RechargeConfigEntity.getInstance()
                .cache(String.format("Consume:Product:Type:%s:Config:List", getProductType()), ECacheTime.DAY)
                .field("id,product_id,title")
                .select();


    }

    @Override
    public SyncResult updateProductIdForConfig(long productId, long configId) {
        RechargeConfigEntity rechargeConfigEntity = RechargeConfigEntity.getInstance()
                .where("id", configId)
                .findEntity();

        if (rechargeConfigEntity == null) return new SyncResult(1, "配置文件不存在");

        Map<String, Object> info = new HashMap<>();
        info.put("product_id", productId);
        long result = RechargeConfigEntity.getInstance().where("id", rechargeConfigEntity.id).update(info);


        if (result == 0) return new SyncResult(1, "更新失败");

        return new SyncResult(0, "更新成功");
    }
}
