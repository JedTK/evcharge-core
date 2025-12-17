package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.ConsumeOrderItemsEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsumeOrderItemsService {


    /**
     * 根据
     * @param orderId  订单id
     * @return
     */
    public List<ConsumeOrderItemsService> getItemsListByOrderId(long orderId){
        return ConsumeOrderItemsEntity.getInstance()
                .where("order_id",orderId)
                .selectList();
    }

    /**
     * 根据
     * @param orderSn  订单id
     * @return
     */
    public List<ConsumeOrderItemsService> getItemsListByOrderSn(String orderSn){
        return ConsumeOrderItemsEntity.getInstance()
                .where("order_sn",orderSn)
                .selectList();
    }

    /**
     * 根据
     * @param orderId 订单id
     * @return
     */
    public ConsumeOrderItemsEntity getItemsByOrderId(long orderId){
        return ConsumeOrderItemsEntity.getInstance()
                .where("order_id",orderId)
                .order("create_time desc")
                .limit(1)
                .findEntity();
    }


    /**
     * 根据
     * @param orderSn 订单id
     * @return
     */
    public ConsumeOrderItemsEntity getItemsByOrderSn(String orderSn){
        return ConsumeOrderItemsEntity.getInstance()
                .where("order_sn",orderSn)
                .order("create_time desc")
                .limit(1)
                .findEntity();
    }



}
