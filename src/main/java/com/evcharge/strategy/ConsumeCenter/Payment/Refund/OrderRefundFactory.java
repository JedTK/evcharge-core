package com.evcharge.strategy.ConsumeCenter.Payment.Refund;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderRefundFactory {

    // Spring 会自动将所有实现了 PaymentCallbackService 接口的 Bean 注入到这个列表中
    private final List<OrderRefundService> services;
    // 用于快速查找服务的 Map
    private final Map<String, OrderRefundService> serviceMap = new HashMap<>();

    // 通过构造函数注入依赖
    public OrderRefundFactory(List<OrderRefundService> services) {
        this.services = services;
    }

    /**
     * 项目启动后，初始化方法会自动将所有服务放入 Map
     */
    @PostConstruct
    public void init(){
        for(OrderRefundService service : services){
            serviceMap.put(service.getProductType(), service);
        }
    }

    /**
     * 根据支付渠道编码获取对应的服务实例
     * @param productType 支付渠道编码，如 "alipay", "wechat"
     * @return 对应的 PaymentCallbackService 实例
     */
    public OrderRefundService getService(String productType){
        OrderRefundService service = serviceMap.get(productType);
        if(service == null){
            throw new RuntimeException("No service found for product type: " + productType);
        }
        return service;
    }



}
