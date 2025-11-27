package com.evcharge.strategy.ConsumeCenter.Payment.PaymentCallback;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentCallbackServiceFactory {


    // Spring 会自动将所有实现了 PaymentCallbackService 接口的 Bean 注入到这个列表中
    private final List<PaymentCallbackService> services;

    // 用于快速查找服务的 Map
    private final Map<String, PaymentCallbackService> serviceMap = new HashMap<>();

    // 通过构造函数注入依赖
    public PaymentCallbackServiceFactory(List<PaymentCallbackService> services) {
        this.services = services;
    }

    /**
     * 项目启动后，初始化方法会自动将所有服务放入 Map
     */
    @PostConstruct
    public void init() {
        for (PaymentCallbackService service : services) {
            serviceMap.put(service.getChannelCode(), service);
        }
    }

    /**
     * 根据支付渠道编码获取对应的服务实例
     * @param channelCode 支付渠道编码，如 "alipay", "wechat"
     * @return 对应的 PaymentCallbackService 实例
     */
    public PaymentCallbackService getService(String channelCode) {
        PaymentCallbackService service = serviceMap.get(channelCode);
        if (service == null) {
            throw new IllegalArgumentException("Unsupported payment channel: " + channelCode);
        }
        return service;
    }


}
