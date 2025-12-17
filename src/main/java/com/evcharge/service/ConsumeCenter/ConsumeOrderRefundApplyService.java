package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.*;
import com.evcharge.entity.recharge.RechargeConfigEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.service.User.UserSummaryService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.PaymentServiceFactory;
import com.evcharge.strategy.ConsumeCenter.Payment.Payment.respon.PaymentRefundResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ConsumeOrderRefundApplyService {

    @Autowired
    private ConsumeOrderRefundApplyDetailService consumeOrderRefundApplyDetailService;

    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    @Autowired
    private PaymentServiceFactory paymentServiceFactory;

    @Autowired
    private ConsumeOrdersRefundService consumeOrdersRefundService;

    @Autowired
    private ConsumeOrderItemsService consumeOrderItemsService;

    @Autowired
    private UserSummaryService userSummaryService;

    public ConsumeOrderRefundApplyEntity getApplyInfoBySn(String applySn) {
        return ConsumeOrderRefundApplyEntity.getInstance()
                .where("apply_sn", applySn)
                .findEntity();
    }

    /**
     * 获取用户可退款金额（现金优先原则）
     * 逻辑：如果余额 < 实付金额，则全退余额；如果余额 > 实付金额，则只退实付金额。
     */
    public BigDecimal getRefundAmountByUid(long uid) {
        // 1. 获取用户当前账户总余额
        BigDecimal currentBalance = UserSummaryEntity.getInstance().getBalanceWithUid(uid);
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRefundableCash = BigDecimal.ZERO;
        BigDecimal remainingBalanceToTrace = currentBalance; // 待追溯的余额水位

        int page = 1;
        long lastYearTimeStamp = TimeUtil.getAddDayTimestamp(-365);

        // 只要还有剩余余额没找到来源，且还有订单，就继续追溯
        while (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) > 0) {
            ConsumeOrdersEntity order = ConsumeOrdersEntity.getInstance()
                    .where("product_type", "recharge")
                    .where("uid", uid)
                    .where("refund_status", 1)
                    .where("payment_status", 2)
                    .order("pay_time desc")
                    .page(page)
                    .findEntity();

            if (order == null) break;
            page++;

            // 订单超过一年，虽然它贡献了余额，但不可退，直接扣减水位并跳过
            BigDecimal orderTotalContribution = order.pay_price.add(order.discount_price);
            if (order.pay_time < lastYearTimeStamp) {
                remainingBalanceToTrace = remainingBalanceToTrace.subtract(orderTotalContribution);
                if (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) < 0) remainingBalanceToTrace = BigDecimal.ZERO;
                continue;
            }

            // 获取产品配置，校验是否包含赠送金额（reward_balance）
            ConsumeOrderItemsEntity item = consumeOrderItemsService.getItemsByOrderSn(order.order_sn);
            if (item == null) continue;

            RechargeConfigEntity config = RechargeConfigEntity.getInstance().getInfoByProductId(item.product_id);
            // 根据你的要求：含有赠送金额的订单（reward_balance > 0）整笔不接受退款
            if (config == null || BigDecimal.valueOf(config.reward_balance).compareTo(BigDecimal.ZERO) > 0) {
                remainingBalanceToTrace = remainingBalanceToTrace.subtract(orderTotalContribution);
                if (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) < 0) remainingBalanceToTrace = BigDecimal.ZERO;
                continue;
            }

            // --- 核心退款算法（现金优先） ---

            // 1. 确定这笔订单在当前剩余余额水位中占了多少
            // 比如余额剩 15，这笔订单贡献了 20，那么它在余额里的有效部分就是 15
            BigDecimal effectiveAmountInBalance = remainingBalanceToTrace.min(orderTotalContribution);

            // 2. 确定这笔订单能退的现金上限（实付金额）
            BigDecimal maxCashCanRefund = order.pay_price;

            // 3. 取两者的极小值
            // 例子1：余额剩 15，实付 18 -> 退 15
            // 例子2：余额剩 19，实付 18 -> 退 18
            BigDecimal actualRefundFromThisOrder = effectiveAmountInBalance.min(maxCashCanRefund);

            totalRefundableCash = totalRefundableCash.add(actualRefundFromThisOrder);

            // 4. 水位下移，继续查下一笔
            remainingBalanceToTrace = remainingBalanceToTrace.subtract(orderTotalContribution);
            if (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalanceToTrace = BigDecimal.ZERO;
            }
        }

        // 最终返回结果，保留两位小数并向下取整（防止微小精度差异）
        return totalRefundableCash.setScale(2, RoundingMode.DOWN);
    }

    public String createOrderSn() {
        return String.format("APPLY%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"),
                common.randomStr(4));
    }

    /**
     * 驳回申请
     * @param applySn 订单编号
     * @param type 操作类型 用户自行取消还是系统后台取消 1=用户自行取消 2=系统取消
     * @return SyncResult
     */
    public SyncResult cancelRefundApply(String applySn,int type) {
        // 1. 获取并校验主申请单
        ConsumeOrderRefundApplyEntity apply = getApplyInfoBySn(applySn);
        if (apply == null) return new SyncResult(1, "退款申请不存在");
        if (apply.refund_status == 2) return new SyncResult(0, "该申请已处理完成");
        if (apply.refund_status != 1) return new SyncResult(1, "申请单状态不支持处理");
        Map<String,Object> data=new HashMap<>();
        data.put("refund_status",type==1?3:4);
        data.put("updated_time",TimeUtil.getTimestamp());
        long res =ConsumeOrderRefundApplyEntity.getInstance().where("id", apply.id).update(data);
        if(res==0) return new SyncResult(1,"取消失败");


        userSummaryService.updateBalance(
                apply.uid,
                apply.refund_amount.negate().doubleValue(),
                "apply_refund",
                "申请退款取消: " + applySn,
                applySn
        );


        return new SyncResult(0,"success");
    }


    /**
     * 申请退款：支持多笔订单自动拆分退款
     */
    public SyncResult applyRefundForRecharge(long uid, BigDecimal refundAmount,String reason) {

        int chargeCount= ChargeOrderEntity.getInstance().where("uid", uid)
                .where("status", 1)
                .count();
        if(chargeCount > 0) return new SyncResult(1,"还有充电中的订单没有结束，请结束充电订单后在申请退款");
        // 1. 金额合法性校验
        BigDecimal currentBalance = UserSummaryEntity.getInstance().getBalanceWithUid(uid);
        if (currentBalance.compareTo(refundAmount) < 0) {
            return new SyncResult(1, "余额不足，无法申请退款");
        }

        // 预校验：调用刚才写的逻辑，判断这笔钱是否属于“可自助退款”的现金范畴
        BigDecimal maxCanRefund = getRefundAmountByUid(uid);
        if (refundAmount.compareTo(maxCanRefund) > 0) {
            return new SyncResult(1, "申请金额超过可退款现金上限（含不可退活动金额）");
        }

        List<ConsumeOrderRefundApplyDetailEntity> orderList = new ArrayList<>();
        BigDecimal remainingToRefund = refundAmount;
        BigDecimal tracedTotalBalance = BigDecimal.ZERO; // 追溯水位线

        String applySn = createOrderSn();
        long lastYearTimeStamp = TimeUtil.getAddDayTimestamp(-365);
        int page = 1;

        // 2. 查找可退款订单并拆分金额（逻辑需同步 getRefundAmountByUid）
        while (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
            ConsumeOrdersEntity order = ConsumeOrdersEntity.getInstance()
                    .where("product_type", "recharge")
                    .where("uid", uid)
                    .where("refund_status", 1) // 确保状态位统一
                    .where("payment_status", 2)
                    .order("pay_time desc")
                    .page(page)
                    .findEntity();

            if (order == null) break;
            page++;

            // 订单贡献的总额度（实付 + 积分抵扣）
            BigDecimal orderContribution = order.pay_price.add(order.discount_price);

            // A. 过滤：一年之前的订单
            if (order.pay_time < lastYearTimeStamp) {
                tracedTotalBalance = tracedTotalBalance.add(orderContribution);
                continue;
            }

            // B. 过滤：检查配置（是否有赠送 reward_balance）
            ConsumeOrderItemsEntity item = consumeOrderItemsService.getItemsByOrderSn(order.order_sn);
            RechargeConfigEntity config = (item != null) ? RechargeConfigEntity.getInstance().getInfoByProductId(item.product_id) : null;

            // 如果包含赠送，该订单贡献了余额水位，但不提供退款额度
            if (config == null || BigDecimal.valueOf(config.reward_balance).compareTo(BigDecimal.ZERO) > 0) {
                tracedTotalBalance = tracedTotalBalance.add(orderContribution);
                continue;
            }

            // --- 核心计算逻辑：现金优先 ---

            // 1. 计算这笔订单在当前余额水位中的有效“残余总额”
            // RemainingInWallet = currentBalance - tracedTotalBalance
            BigDecimal remainingInWallet = currentBalance.subtract(tracedTotalBalance);
            if (remainingInWallet.compareTo(BigDecimal.ZERO) <= 0) {
                break; // 之后的订单已经被消费光了
            }

            // 这笔订单实际在余额里的部分（不能超过订单总贡献，也不能超过当前水位残余）
            BigDecimal effectiveAmountInBalance = remainingInWallet.min(orderContribution);

            // 2. 计算这笔订单可退现金上限（不能超过实付，也不能超过它在余额里的残余）
            BigDecimal maxCashFromThisOrder = effectiveAmountInBalance.min(order.pay_price);

            if (maxCashFromThisOrder.compareTo(BigDecimal.ZERO) > 0) {
                // 3. 确定最终从这笔订单退多少
                BigDecimal actualRefund = maxCashFromThisOrder.min(remainingToRefund);

                ConsumeOrderRefundApplyDetailEntity detail = new ConsumeOrderRefundApplyDetailEntity();
                detail.uid = uid;
                detail.apply_sn = applySn;
                detail.consume_order_sn = order.order_sn;
                detail.refund_amount = actualRefund;
                detail.created_time = TimeUtil.getTimestamp();
                orderList.add(detail);

                remainingToRefund = remainingToRefund.subtract(actualRefund);
            }

            // 更新追溯水位，继续下一笔
            tracedTotalBalance = tracedTotalBalance.add(orderContribution);
        }

        // 3. 结果校验
        if (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
            return new SyncResult(1, "拆分退款金额失败，部分资金可能已消费或不可退");
        }

        // 4. 持久化数据
        try {
            boolean saved = saveToDatabase(uid, applySn, refundAmount,reason, orderList);
            if (!saved) return new SyncResult(1, "数据库写入失败");
        } catch (Exception e) {
            return new SyncResult(1, "保存异常: " + e.getMessage());
        }

        // 5. 扣减/冻结用户余额
        userSummaryService.updateBalance(
                uid,
                -refundAmount.negate().doubleValue(),
                "apply_refund",
                "申请退款: " + applySn,
                applySn
        );
        return new SyncResult(0, "申请成功");
    }

    /**
     * 处理退款申请（回调/后台审核通过后调用）
     * 优化点：支持部分失败后的重试，确保幂等性
     */
    public SyncResult handleRefundApply(String applySn) {
        // 1. 获取并校验主申请单
        ConsumeOrderRefundApplyEntity apply = getApplyInfoBySn(applySn);
        if (apply == null) return new SyncResult(1, "退款申请不存在");
        if (apply.refund_status == 2) return new SyncResult(0, "该申请已处理完成");
        if (apply.refund_status != 1) return new SyncResult(1, "申请单状态不支持处理");

        // 2. 获取明细列表
        List<ConsumeOrderRefundApplyDetailEntity> details = consumeOrderRefundApplyDetailService.getOrderList(applySn);
        if (details.isEmpty()) return new SyncResult(1, "未找到可退款的订单明细");

        int successCount = 0;
        int failCount = 0;
        String lastErrorMsg = "";

        for (ConsumeOrderRefundApplyDetailEntity detail : details) {
            // 幂等校验：如果该明细已经处理成功（状态为2），直接跳过，防止重复退款
            if (detail.refund_status == 2) {
                successCount++;
                continue;
            }

            ConsumeOrdersEntity originOrder = consumeOrdersService.findByOrderSn(detail.consume_order_sn);
            if (originOrder == null) {
                failCount++;
                continue;
            }

            try {
                PaymentService paymentService = paymentServiceFactory.getPaymentService(originOrder.payment_type);
                String refundOrderSn = consumeOrdersRefundService.createOrderSn(originOrder.order_sn);

                // 3. 调用支付网关原路退款
                SyncResult payRes = paymentService.refund(originOrder, refundOrderSn, detail.refund_amount, "用户余额自助退款");

                if (payRes.code == 0) {
                    // --- 退款成功处理 ---
                    PaymentRefundResponse refundResponse = (PaymentRefundResponse) payRes.data;

                    // A. 记录退款成功日志
                    ConsumeOrderRefundsEntity refundRecord = new ConsumeOrderRefundsEntity();
                    refundRecord.order_id = originOrder.id;
                    refundRecord.uid = originOrder.uid;
                    refundRecord.order_sn = originOrder.order_sn;
                    refundRecord.refund_amount = detail.refund_amount;
                    refundRecord.refund_order_sn = refundOrderSn;
                    refundRecord.refund_bank_order_no = refundResponse.refund_bank_order_no;
                    refundRecord.refund_bank_trx_no = refundResponse.refund_bank_trx_no;
                    refundRecord.status = "SUCCESS";
                    refundRecord.create_time = TimeUtil.getTimestamp();
                    refundRecord.insertGetId();

                    // B. 更新原订单状态（2=已退款/部分退款）
                    Map<String, Object> orderUpdate = new LinkedHashMap<>();
                    orderUpdate.put("refund_status", 2);
                    orderUpdate.put("update_time", TimeUtil.getTimestamp());
                    ConsumeOrdersEntity.getInstance().where("id", originOrder.id).update(orderUpdate);

                    // C. 更新该笔明细状态（2=已处理）
                    Map<String, Object> detailUpdate = new LinkedHashMap<>();
                    detailUpdate.put("refund_status", 2);
                    detailUpdate.put("update_time", TimeUtil.getTimestamp());
                    ConsumeOrderRefundApplyDetailEntity.getInstance().where("id", detail.id).update(detailUpdate);

                    successCount++;
                } else {
                    // --- 退款失败处理 ---
                    failCount++;
                    lastErrorMsg = payRes.msg;
                    // 注意：这里不直接 return，允许处理下一笔明细
                }
            } catch (Exception e) {
                failCount++;
                lastErrorMsg = "系统异常: " + e.getMessage();
            }
        }

        // 4. 更新主申请单状态
        // 如果全部明细都处理成功了，才将主表改为 2 (已完成)
        if (successCount == details.size()) {
            Map<String, Object> applyUpdate = new LinkedHashMap<>();
            applyUpdate.put("refund_status", 2);
            applyUpdate.put("update_time", TimeUtil.getTimestamp());
            applyUpdate.put("refund_time", TimeUtil.getTimestamp());
            ConsumeOrderRefundApplyEntity.getInstance().where("id", apply.id).update(applyUpdate);

            return new SyncResult(0, "退款全部成功");
        } else {
            // 如果有部分失败，主表状态保持 1 (待处理)，方便后台手动重试或系统重试
            return new SyncResult(1, String.format("退款未完全完成：成功%d笔，失败%d笔。最后一次错误：%s",
                    successCount, failCount, lastErrorMsg));
        }
    }

    /**
     * 执行数据库持久化：保存退款申请主表和详情表
     * * @param uid          用户ID
     * @param applySn      申请单号
     * @param refundAmount 总退款金额
     * @param orderList    拆分后的订单详情列表
     * @return boolean 是否保存成功
     */
    private boolean saveToDatabase(long uid, String applySn, BigDecimal refundAmount,String reason, List<ConsumeOrderRefundApplyDetailEntity> orderList) {
        // 1. 组装主表数据
        Map<String, Object> applyData = new HashMap<>();
        applyData.put("uid", uid);
        applyData.put("apply_sn", applySn);
        applyData.put("product_type", "recharge");
        applyData.put("refund_amount", refundAmount);          // 实际要退款的金额（现金）
        applyData.put("apply_refund_amount", refundAmount);    // 申请时的金额
        applyData.put("refund_reason", reason);
        applyData.put("refund_status", 1);                     // 1=待处理
        applyData.put("created_time", TimeUtil.getTimestamp());

        // 插入主表并获取主键 ID
        long mainId = ConsumeOrderRefundApplyEntity.getInstance().insertGetId(applyData);

        if (mainId <= 0) {
            return false;
        }

        // 2. 循环插入详情表数据
        for (ConsumeOrderRefundApplyDetailEntity detail : orderList) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("uid", uid);
            detailMap.put("apply_sn", applySn);
            detailMap.put("consume_order_sn", detail.consume_order_sn);
            detailMap.put("refund_amount", detail.refund_amount);
            detailMap.put("created_time", TimeUtil.getTimestamp());
            detailMap.put("refund_status", 1); // 1=待处理

            long detailId = ConsumeOrderRefundApplyDetailEntity.getInstance().insertGetId(detailMap);

            if (detailId <= 0) {
                // 注意：如果这里失败了，在没有分布式事务或本地事务的情况下，建议记录严重错误日志
                // 或者抛出异常触发外部事务回滚
                throw new RuntimeException("退款申请详情插入失败, applySn: " + applySn);
            }
        }

        return true;
    }
}