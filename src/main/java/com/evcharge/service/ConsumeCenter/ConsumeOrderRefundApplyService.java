package com.evcharge.service.ConsumeCenter;


import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundApplyDetailEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.service.User.UserService;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsumeOrderRefundApplyService {


    private UserService userService;

    /**
     * 创建订单号
     *
     * @return
     */
    public String createOrderSn() {

        return String.format("APPLY%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                , common.randomStr(4));
    }


    public void applyRefundForRecharge(long uid, BigDecimal refundAmount) {
        /**
         * 获取用户余额
         * 判断退款余额是否大于用户余额
         * 获取用户最近充值订单，需要遍历循环
         * 情况一：用户余额20元，第一笔订单金额是20，调用接口退款，清空用户余额
         * 情况二：用户余额30元，第一笔订单金额是20元，第二笔订单是20元，则第一笔订单退款20元，第二笔订单退款10元
         * 情况三：
         *
         */
        BigDecimal userBalance = UserSummaryEntity.getInstance().getBalanceWithUid(uid);
        if (userBalance.compareTo(refundAmount) != 0) {
            return;
        }
        boolean result = true;
        List<ConsumeOrderRefundApplyDetailEntity> orderList = new ArrayList<>();
        BigDecimal lastRefundAmount = refundAmount;
        String applySn = createOrderSn();
        int page = 1;
        while (result) {
            ConsumeOrderRefundApplyDetailEntity consumeOrderRefundApplyDetailEntity = new ConsumeOrderRefundApplyDetailEntity();
            consumeOrderRefundApplyDetailEntity.uid = uid;
            consumeOrderRefundApplyDetailEntity.apply_sn = applySn;
            consumeOrderRefundApplyDetailEntity.created_time = TimeUtil.getTimestamp();
            //获取没有退款的充值订单
            ConsumeOrdersEntity consumeOrdersEntity = ConsumeOrdersEntity.getInstance()
                    .where("product_type", "recharge")
                    .where("uid", uid)
                    .order("pay_time desc")
                    .page(page)
                    .findEntity();


            if (consumeOrdersEntity == null) return;
            //需要检查该订单是否申请退款，已经申请过退款的无法再继续申请
            //还有活动的不能退款

            //如果退款金额大于订单金额，需要减去订单金额，再继续查找下一个订单
            if (consumeOrdersEntity.pay_price.compareTo(lastRefundAmount) < 0) {
                lastRefundAmount = lastRefundAmount.subtract(consumeOrdersEntity.pay_price);

                consumeOrderRefundApplyDetailEntity.consume_order_sn = consumeOrdersEntity.order_sn;
                consumeOrderRefundApplyDetailEntity.refund_amount = consumeOrdersEntity.pay_price;
                orderList.add(consumeOrderRefundApplyDetailEntity);
                page=page+1;
            }
            //如果退款金额小于订单金额，则记录退款金额，无需再查找下一个订单
            if (consumeOrdersEntity.pay_price.compareTo(lastRefundAmount) >= 0) {
                consumeOrderRefundApplyDetailEntity.consume_order_sn = consumeOrdersEntity.order_sn;
                consumeOrderRefundApplyDetailEntity.refund_amount = lastRefundAmount;

                orderList.add(consumeOrderRefundApplyDetailEntity);
                page=page+1;
                result = false;
            }

        }
        if(orderList.isEmpty()) return;
        Map<String,Object> applyData=new HashMap<>();
        applyData.put("uid",uid);
        applyData.put("apply_sn",applySn);
        applyData.put("product_type","recharge");
        applyData.put("refund_amount",refundAmount);
        applyData.put("apply_refund_amount",refundAmount);
        applyData.put("refund_reason","");
        applyData.put("refund_description","");
        applyData.put("refund_status",1);
        applyData.put("created_time",TimeUtil.getTimestamp());



    }


}
