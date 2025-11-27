package com.evcharge.strategy.ConsumeCenter.Payment.Payment;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentServiceFactory {

    private final List<PaymentService> services;
    private final Map<String, PaymentService> serviceMap = new HashMap<>();

    public PaymentServiceFactory(List<PaymentService> services) {
        this.services = services;
    }
    /**
     * 项目启动后，初始化方法会自动将所有服务放入 Map
     */
    @PostConstruct
    public void init() {
        // 项目启动时，自动将所有PaymentService实现类放入Map中
        for (PaymentService service : services) {
            serviceMap.put(service.getPaymentMethodCode(), service);
        }
    }

    /**
     * 根据支付方式编码获取对应的支付服务
     */
    public PaymentService getPaymentService(String methodCode) {
        PaymentService service = serviceMap.get(methodCode);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + methodCode);
        }
        return service;
    }

}
