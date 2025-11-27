package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.ConsumeOrderDeliveryEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConsumeOrderDeliveryService {


    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    /**
     * 根据订单id获取物流信息
     *
     * @param orderId
     * @return
     */
    public ConsumeOrderDeliveryEntity getConsumeOrderDeliveryByOrderId(long orderId) {

        return ConsumeOrderDeliveryEntity.getInstance()
                .where("order_id", orderId)
                .order("create_time desc")
                .limit(1)
                .findEntity();
    }

    /**
     * 根据订单id获取物流信息
     *
     * @param orderSn
     * @return
     */
    public ConsumeOrderDeliveryEntity getConsumeOrderDeliveryByOrderSn(String orderSn) {

        return ConsumeOrderDeliveryEntity.getInstance()
                .where("order_sn", orderSn)
                .order("create_time desc")
                .limit(1)
                .findEntity();
    }


    /**
     * 发货
     *
     * @param orderSn    订单编号
     * @param carrier    物流公司
     * @param trackingNo 快递单号
     * @return
     */
    public SyncResult shipping(String orderSn, String carrier, String trackingNo) {
        ConsumeOrdersEntity consumeOrdersEntity = consumeOrdersService.findByOrderSn(orderSn);
        if (consumeOrdersEntity == null) return new SyncResult(1, "订单信息不存在");
        ConsumeOrderDeliveryEntity consumeOrderDeliveryEntity = this.getConsumeOrderDeliveryByOrderSn(orderSn);
        if (consumeOrderDeliveryEntity == null) return new SyncResult(1, "物流信息不存在");

        //判断是否已经发货
        if (consumeOrderDeliveryEntity.status.equals("SHIPPED")) return new SyncResult(1, "该订单已发货");
        Map<String, Object> deliveryMap = new HashMap<String, Object>();
        deliveryMap.put("carrier", carrier);
        deliveryMap.put("tracking_no", trackingNo);
        deliveryMap.put("status", "SHIPPED");
        deliveryMap.put("shipment_time", TimeUtil.getTimestamp());
        deliveryMap.put("update_time", TimeUtil.getTimestamp());


        long update = ConsumeOrderDeliveryEntity.getInstance().where("id", consumeOrderDeliveryEntity.id).update(deliveryMap);
        if (update == 0) return new SyncResult(1, "1.更新物流信息失败");
        //更新订单履行状态
        long orderUpdate=ConsumeOrdersEntity.getInstance().where("order_sn",orderSn).update(new HashMap<>(){{
            put("update_time", TimeUtil.getTimestamp());
            put("fulfillment_status", "FULFILLED");
        }});
        if (orderUpdate == 0) return new SyncResult(1, "2.更新物流信息失败");

        return new SyncResult(0, "success", "发货成功");
    }


}
