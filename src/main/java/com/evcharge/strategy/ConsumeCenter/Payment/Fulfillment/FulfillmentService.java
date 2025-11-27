package com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment;

import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;

public interface FulfillmentService {
    /**
     * 获取此服务处理的产品类型
     */
    String getProductType();

    /**
     * 执行订单的履行逻辑
     * @param order 已经支付成功的订单对象
     */
    void processFulfillment(ConsumeOrdersEntity order);

    
    void rollbackFulfillment(ConsumeOrdersEntity order); // 新增的回滚方法
}
