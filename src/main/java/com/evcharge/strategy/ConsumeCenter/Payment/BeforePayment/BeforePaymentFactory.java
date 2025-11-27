package com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BeforePaymentFactory {

    private final List<BeforePaymentService> services;

    private final Map<String, BeforePaymentService> serviceMap = new HashMap<>();

    public BeforePaymentFactory(List<BeforePaymentService> services) {
        this.services = services;
    }

    @PostConstruct
    public void init() {
        for (BeforePaymentService service : services) {
            serviceMap.put(service.getProductType(), service);
        }
    }
    /**
     * 根据产品编码获取对应的支付服务
     */
    public BeforePaymentService getBeforePaymentService(String productType) {
        BeforePaymentService service = serviceMap.get(productType);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported product type: " + productType);

        }
        return service;
    }

}
