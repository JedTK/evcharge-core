package com.evcharge.service.ConsumeCenter;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.enumdata.EConsumeOrderStatus;
import com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment.BeforePaymentFactory;
import com.evcharge.strategy.ConsumeCenter.Payment.BeforePayment.BeforePaymentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentServiceFactory;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsumeOrdersService {

    @Autowired
    private BeforePaymentFactory beforePaymentFactory;

    @Autowired
    private final PaymentServiceFactory paymentServiceFactory;

    public ConsumeOrdersService(PaymentServiceFactory paymentServiceFactory) {
        this.paymentServiceFactory = paymentServiceFactory;
    }

    /**
     * 统计符合条件的产品数量
     */
    public int getCount(Map<String, Object> param) {
        ConsumeOrdersEntity query = buildQuery(param);
        return query.count();
    }

    /**
     * 获取分页产品列表（默认按 id desc 排序）
     */
    public List<ConsumeOrdersEntity> getList(Map<String, Object> param, int offset, int limit) {
        return getList(param, offset, limit, "sort asc");
    }

    /**
     * 获取分页产品列表（支持自定义排序）
     */
    public List<ConsumeOrdersEntity> getList(Map<String, Object> param, int offset, int limit, String orderBy) {
        ConsumeOrdersEntity consumeProductsEntity = buildQuery(param);
        return consumeProductsEntity
                .order(orderBy)
                .page(offset, limit).selectList();
    }


    private ConsumeOrdersEntity buildQuery(Map<String, Object> param) {
        ConsumeOrdersEntity entity = new ConsumeOrdersEntity();

        if (param == null || param.isEmpty()) {
            return entity;
        }

        for (Map.Entry<String, Object> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (Objects.isNull(value)) continue;
            entity.where(key, value);
        }

        return entity;

    }


    /**
     * 创建订单号
     *
     * @param type String
     * @return
     */
    public String createOrderSn(String type) {
        String OrderSN;
        switch (type) {
            case "recharge":
                OrderSN = String.format("RE%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            case "refund":
                OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            default:
                OrderSN = String.format("OR%s%sSN", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
        }
        OrderSN = OrderSN.toUpperCase();
        return OrderSN;
    }

    /**
     * 创建订单
     *
     * @param consumeOrdersEntity 订单实体类
     * @return SyncResult
     */
    public SyncResult createOrder(ConsumeOrdersEntity consumeOrdersEntity,long productId) {
        // 1. 创建订单前检查用户是否有资格购买产品 比如半年卡 每人每个站点只限购一张这类限制
        String productType = consumeOrdersEntity.product_type;

        BeforePaymentService beforePaymentService = beforePaymentFactory.getBeforePaymentService(productType);

        SyncResult result = beforePaymentService.beforePaymentCheck(consumeOrdersEntity,productId);

        if (result.code != 0) return result;

        long orderId = consumeOrdersEntity.insertGetId();
        if (orderId == 0) return new SyncResult(1, "1.创建订单失败，请稍后再试");
        return new SyncResult(0, "success", orderId);
    }

    /**
     * 发起支付
     *
     * @param orderSn     订单编号
     * @param description 订单描述
     * @return SyncResult
     */
    public SyncResult initiatePayment(String orderSn, String description) {
        // 1. 从数据库查询订单
        ConsumeOrdersEntity order = this.findByOrderSn(orderSn);

        // 2. 根据订单的支付方式获取对应的服务
        String methodCode = order.payment_type;
        PaymentService service = paymentServiceFactory.getPaymentService(methodCode);

        // 3. 调用统一的接口发起支付请求
        return service.createPaymentRequest(order, description);
    }

    /**
     * 根据订单编号查找订单信息
     *
     * @param orderSn
     * @return
     */
    public ConsumeOrdersEntity findByOrderSn(String orderSn) {
        return ConsumeOrdersEntity.getInstance()
                .where("order_sn", orderSn)
                .findEntity();
    }

    /**
     * 支付成功
     *
     * @param orderSn      订单编号
     * @param payOrderSn   银行流水号
     * @param payPrice     支付金额
     * @param settleAmount 结算金额
     * @return
     */
    public SyncResult paySuccess(String orderSn, String payOrderSn, BigDecimal payPrice, BigDecimal settleAmount) {
        ConsumeOrdersEntity order = this.findByOrderSn(orderSn);
        if (order == null) return new SyncResult(1, "can not find order");
        if (order.payment_status == EConsumeOrderStatus.PAID.getCode()) return new SyncResult(1, "该订单已支付");

        Map<String, Object> data = new HashMap<>();


        data.put("payment_status", EConsumeOrderStatus.PAID.getCode()); //支付成功
        data.put("pay_order_sn", payOrderSn); //更新订单
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("pay_time", TimeUtil.getTimestamp());
        data.put("pay_price", payPrice);
        data.put("settle_amount", settleAmount);

        order.where("id", order.id).update(data);
        return new SyncResult(0, "success");
    }


    /**
     * 取消订单
     *
     * @param orderSn 订单编号
     * @return SyncResult
     */
    public SyncResult cancelOrder(String orderSn) {
        ConsumeOrdersEntity orderInfo = this.findByOrderSn(orderSn);

        if (orderInfo == null) {
            return new SyncResult(2, "订单不存在");
        }

        if (orderInfo.payment_status == EConsumeOrderStatus.CANCELED.getCode()) {
            return new SyncResult(3, "订单已取消");
        }

        if (orderInfo.payment_status == EConsumeOrderStatus.PAID.getCode()) {
            return new SyncResult(3, "订单已完成支付");
        }

        JSONObject data = new JSONObject();

        data.put("payment_status", EConsumeOrderStatus.CANCELED.getCode());
        data.put("update_time", TimeUtil.getTimestamp());
        int r = ConsumeOrdersEntity.getInstance().where("id", orderInfo.id).update(data);
        if (r != 0) {
            //删除缓存
            DataService.getMainCache().del(String.format("ConsumeOrder:%s", orderSn));
            return new SyncResult(0, "订单取消成功");
        } else {
            return new SyncResult(1, "订单取消失败");
        }
    }


}
