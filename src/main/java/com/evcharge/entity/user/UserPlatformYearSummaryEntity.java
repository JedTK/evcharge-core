package com.evcharge.entity.user;


import com.evcharge.entity.chargecard.UserChargeCardOrderEntity;
import com.evcharge.entity.chargecard.UserChargeCardRefundOrderEntity;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.entity.recharge.RechargeRefundOrderEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户汇总平台年度数据;
 *
 * @author : JED
 * @date : 2024-1-20
 */
public class UserPlatformYearSummaryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 日期，格式：yyyy-MM-dd
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 昵称
     */
    public String nickname;
    /**
     * 手机号码
     */
    public String phone;
    /**
     * 计次充电的次数
     */
    public int pay_per_charge_count;
    /**
     * 计次充电的时长
     */
    public long pay_per_charge_duration;
    /**
     * 计次充电消费金额
     */
    public BigDecimal pay_per_charge_amount;
    /**
     * 充电卡充电的次数
     */
    public int card_charge_count;
    /**
     * 充电卡充电的时长
     */
    public long card_charge_duration;
    /**
     * 充电卡消费金额
     */
    public BigDecimal card_charge_amount;
    /**
     * 充值订单数
     */
    public int rechargeOrderCount;
    /**
     * 充值金额（毛）
     */
    public BigDecimal rechargeAmount;
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
     * 充电卡退款订单数
     */
    public int chargeCardRefundOrderCount;
    /**
     * 充电卡退款订单金额
     */
    public BigDecimal chargeCardRefundAmount;
    /**
     * 历史最高功率
     */
    public Double historyMaxPower;
    /**
     * 注册时间
     */
    public long regTime;
    /**
     * 首次充电时间
     */
    public long firstChargeTime;
    /**
     * 最后充电时间
     */
    public long lastChargeTime;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static UserPlatformYearSummaryEntity getInstance() {
        return new UserPlatformYearSummaryEntity();
    }

    /**
     * 同步数据
     *
     * @param uid 用户id
     * @return
     */
    public SyncResult syncData(long uid, long yearTime) {
        if (yearTime == 0) yearTime = TimeUtil.getTimestamp();

        String date = TimeUtil.toTimeString(yearTime, "yyyy");
        long startTime = TimeUtil.toYearBegin00(yearTime);
        long endTime = TimeUtil.toYearEnd24(yearTime);

        LogsUtil.info("用户汇总平台年度数据", "[%s] - %s 汇总数据任务执行", uid, date);
        try {
            Map<String, Object> user_data = UserEntity.getInstance()
                    .where("id", uid)
                    .find();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("uid", uid);
            data.put("nickname", MapUtil.getString(user_data, "nickname"));
            data.put("phone", MapUtil.getString(user_data, "phone"));
            data.put("regTime", MapUtil.getString(user_data, "create_time"));

            //region 计次充电的次数、计次充电消费金额、计次充电的时长
            Map<String, Object> pay_per_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS pay_per_charge_count" +
                            ",IFNULL(SUM(totalAmount),0) AS pay_per_charge_amount" +
                            ",IFNULL(SUM(totalChargeTime),0) AS pay_per_charge_duration"
                    )
                    .where("uid", uid)
                    .where("status", 2)
                    .where("paymentTypeId", 1)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //计次充电的次数
            data.put("pay_per_charge_count", pay_per_charge_data.get("pay_per_charge_count"));
            //计次充电消费金额
            data.put("pay_per_charge_amount", pay_per_charge_data.get("pay_per_charge_amount"));
            //计次充电的时长
            data.put("pay_per_charge_duration", pay_per_charge_data.get("pay_per_charge_duration"));
            //endregion

            //region 充电卡充电的次数、充电卡消费金额、充电卡充电的时长

            Map<String, Object> card_charge_data = ChargeOrderEntity.getInstance()
                    .field("COUNT(1) AS card_charge_count" +
                            ",IFNULL(SUM(chargeCardConsumeAmount),0) AS card_charge_amount" +
                            ",IFNULL(SUM(totalChargeTime),0) AS card_charge_duration"
                    )
                    .where("uid", uid)
                    .where("status", 2)
                    .where("paymentTypeId", 2)//支付方式：1=余额，2=充电卡
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充电卡充电的次数
            data.put("card_charge_count", card_charge_data.get("card_charge_count"));
            //充电卡消费金额
            data.put("card_charge_amount", card_charge_data.get("card_charge_amount"));
            //充电卡充电的时长
            data.put("card_charge_duration", card_charge_data.get("card_charge_duration"));

            //endregion

            //region 充值订单数、充值金额（毛）、充值退款订单数、充值退款订单金额

            //充值订单数次数、总充值金额（毛）
            Map<String, Object> recarge_data = RechargeOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as rechargeOrderCount" +
                            ",IFNULL(SUM(pay_price),0) AS rechargeAmount"
                    )
                    .where("uid", uid)
                    .whereIn("status", "2,3,4") //状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充值金额（毛）
            data.put("rechargeOrderCount", recarge_data.get("rechargeOrderCount"));//充值订单数
            data.put("rechargeAmount", MapUtil.getBigDecimal(recarge_data, "rechargeAmount"));

            //充值退款订单数、退款订单金额
            Map<String, Object> recharge_refund_data = RechargeRefundOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .where("uid", uid)
                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
                    .where("isTest", 0)
                    .find();
            //充值退款订单数
            data.put("rechargeRefundOrderCount", recharge_refund_data.get("OrderCount"));
            //充值退款订单金额
            data.put("rechargeRefundAmount", MapUtil.getBigDecimal(recharge_refund_data, "refund_amount"));

            //endregion

            //region 充电卡订单数、充电卡金额（毛）、充电卡退款订单数、充电卡退款订单金额

            Map<String, Object> chargecard_data = UserChargeCardOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as chargeCardOrderCount" +
                            ",IFNULL(SUM(totalAmount),0) AS chargeCardAmount"
                    )
                    .where("uid", uid)
                    .whereIn("status", "1,2,3") //状态;0=等待支付，1=支付成功，2=全额退款，3=部分退款
                    .where("isTest", 0)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .find();
            //充电卡订单数
            data.put("chargeCardOrderCount", chargecard_data.get("chargeCardOrderCount"));
            //充电卡金额（毛）
            data.put("chargeCardAmount", MapUtil.getBigDecimal(chargecard_data, "chargeCardAmount"));

            //充电卡退款订单数、退款订单金额
            Map<String, Object> chargecard_refund_data = UserChargeCardRefundOrderEntity.getInstance()
                    .field("IFNULL(COUNT(1),0) as OrderCount,IFNULL(SUM(refund_amount),0) AS refund_amount")
                    .where("uid", uid)
                    .where("refund_status", "2") //状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
                    .where("isTest", 0)
                    .find();
            //充电卡退款订单数
            data.put("chargeCardRefundOrderCount", MapUtil.getInt(chargecard_refund_data, "OrderCount", 0));
            //充电卡退款订单金额
            data.put("chargeCardRefundAmount", MapUtil.getBigDecimal(chargecard_refund_data, "refund_amount"));

            //endregion

            //region 历史最高功率
            double historyMaxPower = 0.0;
            Map<String, Object> historyMaxPowerData = ChargeOrderEntity.getInstance()
                    .field("id,maxPower")
                    .where("uid", uid)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .order("maxPower DESC")
                    .find();
            if (!historyMaxPowerData.isEmpty()) {
                historyMaxPower = MapUtil.getDouble(historyMaxPowerData, "maxPower");
            }
            data.put("historyMaxPower", historyMaxPower);
            //endregion

            //region 首次充电时间
            double firstChargeTime = 0.0;
            Map<String, Object> firstChargeTimeData = ChargeOrderEntity.getInstance()
                    .field("id,startTime")
                    .where("uid", uid)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .order("id")
                    .find();
            if (!firstChargeTimeData.isEmpty()) {
                firstChargeTime = MapUtil.getDouble(firstChargeTimeData, "startTime");
            }
            data.put("firstChargeTime", firstChargeTime);
            //endregion

            //region 最后充电时间
            double lastChargeTime = 0.0;
            Map<String, Object> lastChargeTimeData = ChargeOrderEntity.getInstance()
                    .field("id,startTime")
                    .where("uid", uid)
                    .where("create_time", ">=", startTime)
                    .where("create_time", "<=", endTime)
                    .order("id DESC")
                    .find();
            if (!lastChargeTimeData.isEmpty()) {
                lastChargeTime = MapUtil.getDouble(lastChargeTimeData, "startTime");
            }
            data.put("lastChargeTime", lastChargeTime);
            //endregion

            data.put("date_time", startTime);
            data.put("update_time", TimeUtil.getTimestamp());
            if (getInstance().where("uid", uid)
                    .where("date", date)
                    .exist()) {
                getInstance().where("uid", uid)
                        .where("date", date)
                        .update(data);
            } else {
                data.put("date", date);
                data.put("create_time", TimeUtil.getTimestamp());
                getInstance().insert(data);
            }
        } catch (Exception e) {
            LogsUtil.error(e, "用户汇总平台年度数据", "汇总数据发生错误，uid=%s", uid);
        }
        return new SyncResult(0, "");
    }

    /**
     * 用来修复数据的（辅助使用）
     *
     * @param start_uid 起始查询的用户id
     */
    public void repairData(long start_uid, long yearTime) {
        if (yearTime == 0) yearTime = TimeUtil.getTimestamp();

        long endTime = TimeUtil.toYearEnd24(yearTime);

        Map<String, Object> data = UserEntity.getInstance()
                .field("id")
                .where("id", ">", start_uid)
                .where("create_time", "<=", endTime)
                .order("id")
                .find();
        if (data == null || data.size() == 0) {
            LogsUtil.info("用户汇总平台年度数据", "结束任务");
            return;
        }
        long uid = MapUtil.getLong(data, "id");
        if (uid == 0) {
            LogsUtil.info("用户汇总平台年度数据", "结束任务");
            return;
        }
        syncData(uid, yearTime);
        long finalYearTime = yearTime;
        ThreadUtil.getInstance().execute("", () -> repairData(uid, finalYearTime));
    }
}
