package com.evcharge.strategy.ConsumeCenter.Product.ProductType;


import com.xyzs.entity.SyncResult;

import java.util.List;
import java.util.Map;

public interface ProductTypeStrategyService {


    /**
     * 获取此服务处理的产品类型
     */
    String getProductType();

    List<Map<String,Object>> getConfigList();


    SyncResult updateProductIdForConfig(long productId,long configId);


}
