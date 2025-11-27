package com.evcharge.strategy.ConsumeCenter.Product.ProductType;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductTypeStrategyFactory {


    private final Map<String, ProductTypeStrategyService> serviceMap = new HashMap<>();


    public ProductTypeStrategyFactory(List<ProductTypeStrategyService> services) {

        for (ProductTypeStrategyService service : services) {
            serviceMap.put(service.getProductType(), service);
        }
    }

    public ProductTypeStrategyService getService(String productType) {
        ProductTypeStrategyService service = serviceMap.get(productType);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported product type for ProductTypeServe: " + productType);
        }
        return service;
    }


}
