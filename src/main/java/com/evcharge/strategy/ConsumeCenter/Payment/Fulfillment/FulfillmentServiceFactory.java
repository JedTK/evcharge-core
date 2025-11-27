package com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FulfillmentServiceFactory {

    private final Map<String, FulfillmentService> serviceMap = new HashMap<>();

    public FulfillmentServiceFactory(List<FulfillmentService> services) {
        for (FulfillmentService service : services) {
            serviceMap.put(service.getProductType(), service);
        }
    }

    public FulfillmentService getService(String productType) {
        FulfillmentService service = serviceMap.get(productType);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported product type for fulfillment: " + productType);
        }
        return service;
    }



}
