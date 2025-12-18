package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.order.*;
import com.evcharge.entity.consumecenter.order.vo.RefundAmountVo;
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


    /**
     * 获取最近7天申请退款记录
     *
     * @param uid uid
     * @return
     */
    public List<ConsumeOrderRefundApplyEntity> getList(long uid) {
        long last7DaysTimeStamp = TimeUtil.getAddDayTimestamp(-7);
        return ConsumeOrderRefundApplyEntity.getInstance()
                .where("uid", uid)
                .where("create_time", ">=", last7DaysTimeStamp)
                .selectList();
    }


    public ConsumeOrderRefundApplyEntity getApplyInfoBySn(String applySn) {
        return ConsumeOrderRefundApplyEntity.getInstance()
                .where("apply_sn", applySn)
                .findEntity();
    }

    public ConsumeOrderRefundApplyEntity getApplyInfoBySn(long uid, String applySn) {
        return ConsumeOrderRefundApplyEntity.getInstance()
                .where("apply_sn", applySn)
                .where("uid", uid)
                .findEntity();
    }

    public String createOrderSn() {
        return String.format("APPLY%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"),
                common.randomStr(4));

    }

    /**
     * 获取用户可退款余额
     * @param uid 用户id
     * @return RefundAmountVo
     */
    public RefundAmountVo getRefundAmountByUid(long uid) {
        BigDecimal currentBalance = UserSummaryEntity.getInstance().getBalanceWithUid(uid);
        RefundAmountVo result = new RefundAmountVo();
        result.deductBalance = currentBalance; // 申请退款则扣除全部余额
        result.refundCashAmount = BigDecimal.ZERO;

        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        BigDecimal remainingBalanceToTrace = currentBalance;
        int page = 1;
        long lastYearTimeStamp = TimeUtil.getAddDayTimestamp(-365);

        while (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) > 0) {
            ConsumeOrdersEntity order = ConsumeOrdersEntity.getInstance()
                    .where("product_type", "recharge")
                    .where("uid", uid)
                    .where("refund_status", 0) // 0=无退款
                    .where("payment_status", 2) // 2=已支付
                    .order("pay_time desc")
                    .page(page)
                    .findEntity();

            if (order == null) break;
            page++;

            BigDecimal orderTotalContribution = order.pay_price.add(order.discount_price);
            // 当前水位在该订单中所占的份额
            BigDecimal effectiveInBalance = remainingBalanceToTrace.min(orderTotalContribution);

            // 判定条件
            boolean isExpired = order.pay_time < lastYearTimeStamp;
            ConsumeOrderItemsEntity item = consumeOrderItemsService.getItemsByOrderSn(order.order_sn);
            RechargeConfigEntity config = (item != null) ? RechargeConfigEntity.getInstance().getInfoByProductId(item.product_id) : null;
            boolean hasReward = (config != null && BigDecimal.valueOf(config.reward_balance).compareTo(BigDecimal.ZERO) > 0);

            if (!isExpired && !hasReward) {
                // --- 核心逻辑变更：优先扣积分 ---
                // 逻辑：可退现金 = Max(0, 当前订单份额 - 该订单的折扣金额)
                // 但不能超过该订单的实付金额 (pay_price)

                // 举例：订单充20(付18,折2)，消费3元，剩余17元。
                // 17 - 0(因为该订单已消费，此时我们不关心整体，只关心这一笔的份额)
                // 这里的算法需要换个思路：如果 份额 > 折扣，那么 现金部分 = 份额 - 折扣
                if (effectiveInBalance.compareTo(order.discount_price) > 0) {
                    // 剩余份额足以覆盖折扣，扣除折扣后剩下的全是现金
                    BigDecimal cashInBalance = effectiveInBalance.subtract(order.discount_price);
                    // 确保不超付（虽然逻辑上不可能，但加个min更稳）
                    result.refundCashAmount = result.refundCashAmount.add(cashInBalance.min(order.pay_price));
                } else {
                    // 剩余份额还没折扣多，说明现金早就花光了，可退为0
                    result.refundCashAmount = result.refundCashAmount.add(BigDecimal.ZERO);
                }
            }

            remainingBalanceToTrace = remainingBalanceToTrace.subtract(orderTotalContribution);
            if (remainingBalanceToTrace.compareTo(BigDecimal.ZERO) < 0) remainingBalanceToTrace = BigDecimal.ZERO;
        }

        result.refundCashAmount = result.refundCashAmount.setScale(2, RoundingMode.DOWN);
        return result;
    }

    /**
     * 驳回申请
     *
     * @param applySn 订单编号
     * @param type    操作类型 用户自行取消还是系统后台取消 1=用户自行取消 2=系统取消
     * @return SyncResult
     */
    public SyncResult cancelRefundApply(long uid, String applySn, int type) {
        // 1. 获取并校验主申请单
        ConsumeOrderRefundApplyEntity apply = getApplyInfoBySn(uid, applySn);
        if (apply == null) return new SyncResult(1, "退款申请不存在");
        if (apply.refund_status == 2) return new SyncResult(0, "该申请已处理完成");
        if (apply.refund_status != 1) return new SyncResult(1, "申请单状态不支持处理");
        Map<String, Object> data = new HashMap<>();
        data.put("refund_status", type == 1 ? 3 : 4);
        data.put("update_time", TimeUtil.getTimestamp());
        long res = ConsumeOrderRefundApplyEntity.getInstance().where("id", apply.id).update(data);
        if (res == 0) return new SyncResult(1, "取消失败");

        userSummaryService.updateBalance(
                apply.uid,
                apply.refund_amount.doubleValue(),
                "apply_refund",
                "申请退款取消: " + applySn,
                applySn
        );


        return new SyncResult(0, "success");
    }

    /**
     * 退款申请
     * @param uid 用户id
     * @param refundAmount 退款金额
     * @param reason 退款原因
     * @param descriptionImage 图片描述
     * @return SyncResult
     */
    public SyncResult applyRefundForRecharge(long uid, BigDecimal refundAmount, String reason, String descriptionImage) {

        // 0. 前置校验
        int chargeCount = ChargeOrderEntity.getInstance().where("uid", uid).where("status", 1).count();
        if (chargeCount > 0) return new SyncResult(1, "还有充电中的订单没有结束，请结束充电订单后再申请退款");

        int count = ConsumeOrderRefundApplyEntity.getInstance().where("uid", uid).where("refund_status", 1).count();
        if (count > 0) return new SyncResult(1, "退款申请处理中，请勿重复操作");

        // 1. 获取上限计算结果
        RefundAmountVo calc = getRefundAmountByUid(uid);
        BigDecimal currentBalance = calc.deductBalance; // 总余额 (37.0)
        BigDecimal maxCanRefund = calc.refundCashAmount; // 现金上限 (36.0)

        // 2. 校验
        if (currentBalance.compareTo(refundAmount) < 0) {
            return new SyncResult(1, "余额不足，无法申请退款");
        }
        if (refundAmount.compareTo(maxCanRefund) > 0) {
            return new SyncResult(1, "申请金额超过可退现金上限（系统已优先扣除您的积分/折扣额度）");
        }

        List<ConsumeOrderRefundApplyDetailEntity> orderList = new ArrayList<>();
        BigDecimal remainingToRefund = refundAmount; // 用户申请的现金 (36.0)

        // 【定义当前水位】：从用户总余额开始倒推
        BigDecimal currentWaterLevel = currentBalance;

        String applySn = createOrderSn();
        long lastYearTimeStamp = TimeUtil.getAddDayTimestamp(-365);
        int page = 1;

        // 3. 查找可退款订单并拆分金额
        while (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
            ConsumeOrdersEntity order = ConsumeOrdersEntity.getInstance()
                    .where("product_type", "recharge")
                    .where("uid", uid)
                    .where("refund_status", 0)
                    .where("payment_status", 2)
                    .order("pay_time desc")
                    .page(page)
                    .findEntity();

            if (order == null) break;
            page++;

            // --- 显式定义 orderTotalContribution ---
            // 这一笔订单给余额池贡献的总水量 = 实付现金 + 积分抵扣/优惠金额
            BigDecimal orderTotalContribution = order.pay_price.add(order.discount_price);

            // 判定订单合规性
            ConsumeOrderItemsEntity item = consumeOrderItemsService.getItemsByOrderSn(order.order_sn);
            RechargeConfigEntity config = (item != null) ? RechargeConfigEntity.getInstance().getInfoByProductId(item.product_id) : null;
            boolean isExpired = order.pay_time < lastYearTimeStamp;
            boolean hasReward = (config != null && BigDecimal.valueOf(config.reward_balance).compareTo(BigDecimal.ZERO) > 0);

            if (!isExpired && !hasReward) {
                // --- 核心：优先扣积分拆分逻辑 ---
                // 只要当前余额水位还覆盖了这笔订单的一部分，就尝试从这部分里提现现金
                // 能够从这笔订单拿走的现金上限 = Min(当前水位, 该订单实付现金)
                BigDecimal cashAvailableInThisOrder = currentWaterLevel.min(order.pay_price);

                if (cashAvailableInThisOrder.compareTo(BigDecimal.ZERO) > 0) {
                    // 实际拿走金额 = Min(可退现金上限, 还没凑够的退款金额)
                    BigDecimal actualRefund = cashAvailableInThisOrder.min(remainingToRefund);

                    ConsumeOrderRefundApplyDetailEntity detail = new ConsumeOrderRefundApplyDetailEntity();
                    detail.uid = uid;
                    detail.apply_sn = applySn;
                    detail.consume_order_sn = order.order_sn;
                    detail.order_amount = order.pay_price;
                    detail.refund_amount = actualRefund;
                    detail.create_time = TimeUtil.getTimestamp();
                    orderList.add(detail);

                    remainingToRefund = remainingToRefund.subtract(actualRefund);
                }
            }

            // 重要：水位不仅要扣除现金，还要扣除积分贡献的部分，因为它们都占了余额
            currentWaterLevel = currentWaterLevel.subtract(orderTotalContribution);
            if (currentWaterLevel.compareTo(BigDecimal.ZERO) < 0) {
                currentWaterLevel = BigDecimal.ZERO;
            }
        }

        // 4. 校验拆分结果
        if (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
            return new SyncResult(1, "拆分退款金额失败，现金部分已被消费");
        }

        // 5. 保存并扣款
        try {
            boolean saved = saveToDatabase(uid, applySn, refundAmount, reason, descriptionImage, orderList);
            if (!saved) return new SyncResult(1, "数据库写入失败");
        } catch (Exception e) {
            return new SyncResult(1, "保存异常: " + e.getMessage());
        }

        // 全额清空余额：扣除 currentBalance
        userSummaryService.updateBalance(
                uid,
                currentBalance.negate().doubleValue(),
                "apply_refund",
                "申请退款清空余额: " + applySn,
                applySn
        );

        return new SyncResult(0, "申请成功");
    }

     /**
     * 处理退款申请（回调/后台审核通过后调用）
     * 优化点：支持部分失败后的重试，确保幂等性
     */
    public SyncResult handleRefundApply(long uid,String applySn) {
        // 1. 获取并校验主申请单
        ConsumeOrderRefundApplyEntity apply = getApplyInfoBySn(uid,applySn);
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
                    orderUpdate.put("refund_status", 1);
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
     *
     * @param applySn      申请单号
     * @param refundAmount 总退款金额
     * @param orderList    拆分后的订单详情列表
     * @return boolean 是否保存成功
     */
    private boolean saveToDatabase(long uid, String applySn, BigDecimal refundAmount, String reason, String descriptionImage, List<ConsumeOrderRefundApplyDetailEntity> orderList) {
        // 1. 组装主表数据
        Map<String, Object> applyData = new HashMap<>();
        applyData.put("uid", uid);
        applyData.put("apply_sn", applySn);
        applyData.put("product_type", "recharge");
        applyData.put("refund_amount", refundAmount);          // 实际要退款的金额（现金）
        applyData.put("apply_refund_amount", refundAmount);    // 申请时的金额
        applyData.put("refund_reason", reason);
        applyData.put("description_image", descriptionImage);
        applyData.put("refund_status", 1);                     // 1=待处理
        applyData.put("create_time", TimeUtil.getTimestamp());

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
            detailMap.put("order_amount", detail.order_amount);
            detailMap.put("refund_amount", detail.refund_amount);
            detailMap.put("create_time", TimeUtil.getTimestamp());
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