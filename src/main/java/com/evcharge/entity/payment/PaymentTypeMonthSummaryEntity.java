package com.evcharge.entity.payment;

import com.evcharge.entity.chargecard.UserChargeCardOrderEntity;
import com.evcharge.entity.chargecard.UserChargeCardRefundOrderEntity;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.entity.recharge.RechargeRefundOrderEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * 支付类型月统计;
 *
 * @author : JED
 * &#064;date  : 2024-4-24
 */
public class PaymentTypeMonthSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 支付ID
     */
    public long paymentTypeId;
    /**
     * 支付标题
     */
    public String paymentTypeText;
    /**
     * 充值订单数
     */
    public int rechargeOrderCount;
    /**
     * 充值金额（毛）
     */
    public BigDecimal rechargeAmount;
    /**
     * 充值人数
     */
    public int rechargeUsers;
    /**
     * 充值退款订单数
     */
    public int rechargeRefundOrderCount;
    /**
     * 充值退款订单金额
     */
    public BigDecimal rechargeRefundAmount;
    /**
     * 充电卡订单数
     */
    public int chargeCardOrderCount;
    /**
     * 充电卡金额（毛）
     */
    public BigDecimal chargeCardAmount;
    /**
     * 充电卡人数
     */
    public int chargeCardUsers;
    /**
     * 充电卡退款订单数
     */
    public int chargeCardRefundOrderCount;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal chargeCardRefundAmount;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static PaymentTypeMonthSummaryEntity getInstance() {
        return new PaymentTypeMonthSummaryEntity();
    }

    /**
     * 同步任务详情
     *
     * @param date_timestamp 统计时间戳（这里泛指按月统计）
     */
    public SyncResult syncTaskJob(long paymentTypeId, long date_timestamp) {
        if (paymentTypeId == 0) return new SyncResult(2, "无效支付类型");
        if (date_timestamp == 0) return new SyncResult(2, "无效统计日期");

        PaymentTypeEntity paymentTypeEntity = PaymentTypeEntity.getInstance()
                .cache(String.format("BaseData:Payment:%s", paymentTypeId), ECacheTime.DAY * 7)
                .where("id", paymentTypeId)
                .findEntity();
        if (paymentTypeEntity == null) return new SyncResult(2, "无效支付类型");

        String date = TimeUtil.toTimeString(date_timestamp, "yyyy-MM");
        try {
            //当月凌晨时间戳
            long startTime = TimeUtil.toMonthBegin00(date_timestamp);
            //当月结束时间戳
            long endTime = TimeUtil.toMonthEnd24(date_timestamp);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("paymentTypeId", paymentTypeEntity.id);
            data.put("paymentTypeText", String.format("%s(%s)", paymentTypeEntity.title, paymentTypeEntity.operator));
            data.put("platform_code", paymentTypeEntity.platform_code);
            data.put("organize_code", paymentTypeEntity.organize_code);
            //充值金额
            BigDecimal rechargeAmount = new BigDecimal(0);
            //充值退款金额
            BigDecimal rechargeRefundAmount = new BigDecimal(0);
            //充电卡金额
            BigDecimal chargeCardAmount = new BigDecimal(0);
            //充电卡退款金额
            BigDecimal chargeCardRefundAmount = new BigDecimal(0);

            //region 充值订单数、充值金额、充值人数、充值退款订单数、充值退款订单金额

            //充值订单数次数、总充值金额
            Map<String, Object> recarge_data = RechargeOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as rechargeOrderCount" +
                            ",IFNULL(COUNT(DISTINCT uid),0) AS rechargeUsers" +
                            ",IFNULL(SUM(pay_price),0) AS rechargeAmount"
                    )
                    .where("status", 2) //状态;1=未支付 2=已完成 -1=已取消
                    .where("paytype_id", paymentTypeEntity.id)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充值金额
            rechargeAmount = MapUtil.getBigDecimal(recarge_data, "rechargeAmount");
            data.put("rechargeAmount", rechargeAmount);
            data.put("rechargeOrderCount", recarge_data.get("rechargeOrderCount"));//充值订单数
            data.put("rechargeUsers", recarge_data.get("rechargeUsers"));//充值人数

            //充值退款订单数、退款订单金额
            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
                    .where("paytype_id", paymentTypeEntity.id)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            rechargeRefundAmount = MapUtil.getBigDecimal(recharge_refund_data, "refund_amount");
            data.put("rechargeRefundAmount", rechargeRefundAmount);//充值退款订单金额
            data.put("rechargeRefundOrderCount", recharge_refund_data.get("OrderCount"));//充值退款订单数

            //endregion

            //region 充电卡订单数、充电卡金额、充电卡人数、充电卡退款订单数、充电卡退款订单金额

            Map<String, Object> chargecard_data = UserChargeCardOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as chargeCardOrderCount" +
                            ",IFNULL(COUNT(DISTINCT uid),0) AS chargeCardUsers" +
                            ",IFNULL(SUM(totalAmount),0) AS chargeCardAmount"
                    )
                    .where("status", 1) //状态;0=等待支付，1=支付成功
                    .where("payTypeId", paymentTypeEntity.id)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充电卡金额
            chargeCardAmount = MapUtil.getBigDecimal(chargecard_data, "chargeCardAmount");
            data.put("chargeCardAmount", chargeCardAmount);
            data.put("chargeCardOrderCount", chargecard_data.get("chargeCardOrderCount"));//充电卡订单数
            data.put("chargeCardUsers", chargecard_data.get("chargeCardUsers"));//充电卡人数

            //充电卡退款订单数、退款订单金额
            Map<String, Object> chargecard_refund_data = UserChargeCardRefundOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
                    .where("paytype_id", paymentTypeEntity.id)
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            chargeCardRefundAmount = MapUtil.getBigDecimal(chargecard_refund_data, "refund_amount");
            data.put("chargeCardRefundAmount", chargeCardRefundAmount);//充电卡退款订单金额
            data.put("chargeCardRefundOrderCount", MapUtil.getInt(chargecard_refund_data, "OrderCount", 0));//充电卡退款订单数

            //endregion

            data.put("date_time", startTime);
            data.put("update_time", TimeUtil.getTimestamp());
            if (PaymentTypeMonthSummaryEntity.getInstance()
                    .where("paymentTypeId", paymentTypeEntity.id)
                    .where("date", date)
                    .exist()) {
                PaymentTypeMonthSummaryEntity.getInstance().where("date", date).update(data);
            } else {
                data.put("date", date);
                data.put("create_time", TimeUtil.getTimestamp());
                PaymentTypeMonthSummaryEntity.getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "平台月数据汇总", "汇总数据发生错误，date=%s", date);
        }
        return new SyncResult(0, "");
    }

    /**
     * 同步数据逻辑，使用循环同步所有时间
     */
    public SyncResult syncData(long paymentTypeId, String paymentTypeText, long date_timestamp) {
        long startTime = TimeUtil.toMonthBegin00(date_timestamp);
        long month = TimeUtil.getMonthBegin00();

        while (startTime <= month) {
            String date = TimeUtil.toTimeString(startTime, "yyyy-MM");
            LogsUtil.info("支付类型月数据汇总", "%s 开始修复 %s 的数据", paymentTypeText, date);
            SyncResult r = PaymentTypeMonthSummaryEntity.getInstance().syncTaskJob(paymentTypeId, startTime);
            if (r.code != 0) {
                break; // 如果同步失败，退出循环
            }
            startTime = TimeUtil.getAddMonthTimestamp(startTime, 1); // 进入下一个月
        }

        LogsUtil.info("支付类型月数据汇总", "%s 修复结束！！！", paymentTypeText);
        return new SyncResult(1, "");
    }

    /**
     * 开始同步逻辑
     */
    public void startSync(long start_time) {
        List<Map<String, Object>> list = PaymentTypeEntity.getInstance()
                .field("id,title,operator")
                .where("status", 1)
                .select();
        for (Map<String, Object> nd : list) {
            long paymentTypeId = MapUtil.getLong(nd, "id");
            String paymentTypeText = String.format("%s(%s)", MapUtil.getString(nd, "title"), MapUtil.getString(nd, "operator"));
            ThreadPoolManager.getInstance().execute("", () -> {
                //从指定日期开始修复
                if (start_time > 0) {
                    syncData(paymentTypeId, paymentTypeText, start_time);
                    return;
                }
                syncData(paymentTypeId, paymentTypeText, TimeUtil.getTimestamp());
            });
        }
    }
}
