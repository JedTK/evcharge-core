package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentServiceFactory;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.respon.PaymentRefundResponse;
import com.evcharge.strategy.ConsumeCenter.Payment.Refund.OrderRefundFactory;
import com.evcharge.strategy.ConsumeCenter.Payment.Refund.OrderRefundService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConsumeOrdersRefundService {


    // 注入退款资格校验工厂
    @Autowired
    private OrderRefundFactory orderRefundFactory;

    @Autowired
    private PaymentServiceFactory paymentServiceFactory;

    /**
     * 创建订单号
     *
     * @param type String
     * @return
     */
    public String createOrderSn(String type) {
        String OrderSN;
        OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomStr(4));
        OrderSN = OrderSN.toUpperCase();
        return OrderSN;
    }

    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    public SyncResult initiateRefund(String orderSn, BigDecimal refundAmount, String reason) {

        ConsumeOrdersEntity ordersEntity = consumeOrdersService.findByOrderSn(orderSn);

        if (ordersEntity == null) return new SyncResult(1, "订单信息不存在");
        switch (ordersEntity.payment_status) {
            case -1:
                return new SyncResult(4, "订单已经取消");
            case 1:
                return new SyncResult(5, "订单还没支付");
            case 3://全额退款
                return new SyncResult(6, "订单已全部退款");
        }
        // 1. 业务有效性校验
        OrderRefundService orderRefundService = orderRefundFactory.getService(ordersEntity.product_type);
//        if (!eligibilityService.isRefundable(ordersEntity)) {
//            return new SyncResult(1,"该订单不能退款");
//        }
        SyncResult checkResult = orderRefundService.isRefundable(ordersEntity, refundAmount);

        if (checkResult.code != 0) return checkResult;

        PaymentService paymentService = paymentServiceFactory.getPaymentService(ordersEntity.payment_type);

        //创建退款订单
        ConsumeOrderRefundsEntity orderRefundsEntity = new ConsumeOrderRefundsEntity();
        orderRefundsEntity.order_id = ordersEntity.id;
        orderRefundsEntity.uid = ordersEntity.uid;
        orderRefundsEntity.order_sn = orderSn;
        orderRefundsEntity.refund_amount = refundAmount;
        orderRefundsEntity.refund_order_sn = this.createOrderSn(orderSn);
        orderRefundsEntity.status = "PENDING";
        orderRefundsEntity.create_time = TimeUtil.getTimestamp();

        SyncResult paymentRefundResult = paymentService.refund(ordersEntity, orderRefundsEntity.refund_order_sn, refundAmount, reason);

        if (paymentRefundResult.code != 0) return paymentRefundResult;

        PaymentRefundResponse refundResponse = (PaymentRefundResponse) paymentRefundResult.data;

        orderRefundsEntity.refund_bank_order_no = refundResponse.refund_bank_order_no;
        orderRefundsEntity.refund_bank_trx_no = refundResponse.refund_bank_trx_no;
        orderRefundsEntity.status = "SUCCESS";

        orderRefundsEntity.insertGetId();
        //如果刚好加起来是全额退款，是否需要更新订单状态？
//        ConsumeOrdersEntity consumeOrdersEntity = new ConsumeOrdersEntity();
        Map<String, Object> consumeOrdersUpdate = new LinkedHashMap<>();
        consumeOrdersUpdate.put("refund_status", 1);
        consumeOrdersUpdate.put("update_time", TimeUtil.getTimestamp());
//        consumeOrdersEntity.payment_status= EConsumeOrderStatus.PART_REFUND.getCode();
//        consumeOrdersEntity.refund_status= 1;
//        consumeOrdersEntity.update_time = TimeUtil.getTimestamp();

        ConsumeOrdersEntity.getInstance().where("id", ordersEntity.id).update(consumeOrdersUpdate);

        return orderRefundService.dealRefund(ordersEntity, refundAmount, reason);
    }

}
