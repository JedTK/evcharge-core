package com.evcharge.entity.user.coupon;


import com.evcharge.entity.coupon.CouponRuleConfigV1Entity;
import com.evcharge.entity.coupon.CouponRuleV1Entity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户券表;
 *
 * @author : Jay
 * @date : 2024-8-14
 */
public class UserPlatformCouponEntity extends BaseEntity implements Serializable {
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
     * 优惠券码 唯一码
     */
    public String coupon_code;
    /**
     * 关联优惠券规则id
     */
    public long rule_id;
    /**
     * 优惠金额
     */
    public BigDecimal amount;
    /**
     * 折扣
     */
    public BigDecimal discount;
    /**
     * 使用时长，毫秒级时间 比如2小时是 60*60*2*1000
     */
    public long charge_duration;
    /**
     * 开始时间
     */
    public long start_time;
    /**
     * 结束时间
     */
    public long end_time;
    /**
     * 使用时间
     */
    public long use_time;
    /**
     * 订单编号
     */
    public String order_sn;
    /**
     * 状态 0=未使用 1=已使用 -1=已过期
     */
    public int status;
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
     *
     * @return
     */
    public static UserPlatformCouponEntity getInstance() {
        return new UserPlatformCouponEntity();
    }


    /**
     * 消耗优惠券
     *
     * @param couponId
     * @return
     */
    public SyncResult consume(long couponId, String orderSn) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", 1);
        data.put("order_sn", orderSn);
        data.put("use_time", TimeUtil.getTimestamp());

        long r = this.where("id", couponId).update(data);
        if (r > 0) {
            //删除缓存
            DataService.getMainCache().del(String.format("User:CouponInfo:%s", couponId));
//            UserSummaryEntity.getInstance().updateUserCouponCount(uid, -1);
            return new SyncResult(0, "success");
        }
        LogsUtil.trace("", String.format("消耗优惠券失败，优惠券ID=%s", couponId));
        return new SyncResult(1, "消耗失败，请稍后再试");
    }

    public Map<String, Object> getCouponInfoById(long uid, long couponId) {
        this.field("cr.rule_name,crf.setting_name,cr.amount_factor,cr.discount_factor,crf.amount_use_stint, uc.*")
                .alias("uc")
                .leftJoin(CouponRuleV1Entity.getInstance().theTableName(), "cr", "uc.rule_id=cr.id")
                .leftJoin(CouponRuleConfigV1Entity.getInstance().theTableName(), "crf", "crf.id=cr.setting_id");
        this.where("uc.uid", uid);
        this.where("uc.id", couponId);
        this.where("uc.status", 0);
        this.where("uc.start_time", "<", TimeUtil.getTimestamp());
        return this.find();
    }


    /**
     * 获取可以使用的优惠券
     *
     * @param typeIds 优惠券类型
     * @param uid     用户id
     * @return 数组
     */
    public List<Map<String, Object>> getCanUseCoupon(String[] typeIds, long uid) {
        return getCanUseCoupon(typeIds, uid, "uc.end_time asc");
    }

    /**
     * 获取未使用优惠券
     *
     * @param typeIds 优惠券类型
     * @param uid     用户id
     * @param orderBy 排序
     * @return object
     */
    public List<Map<String, Object>> getCanUseCoupon(String[] typeIds, long uid, String orderBy) {
//        List<Map<String,Object>> list =
        this.field("cr.rule_name,cr.subtitle,cr.rule_key,crf.setting_name,cr.amount_factor,cr.discount_factor,crf.amount_use_stint, uc.*")
                .alias("uc")
                .leftJoin(CouponRuleV1Entity.getInstance().theTableName(), "cr", "uc.rule_id=cr.id")
                .leftJoin(CouponRuleConfigV1Entity.getInstance().theTableName(), "crf", "crf.id=cr.setting_id");
        this.where("uc.uid", uid);
        this.where("uc.status", 0);
        this.where("uc.start_time", "<", TimeUtil.getTimestamp());
        this.whereIn("crf.rule_type", typeIds);
        this.order(orderBy);
        return this.select();
    }


    /**
     * 获取全部优惠券
     *
     * @param typeIds 优惠券类型
     * @param uid     用户id
     * @param orderBy 排序
     * @return object
     */
    public List<Map<String, Object>> getAllCoupon(String[] typeIds, long uid, int status, String orderBy) {
        this.field("cr.rule_name,cr.subtitle,cr.rule_key,crf.setting_name,cr.amount_factor,cr.discount_factor,crf.amount_use_stint, uc.*")
                .alias("uc")
                .leftJoin(CouponRuleV1Entity.getInstance().theTableName(), "cr", "uc.rule_id=cr.id")
                .leftJoin(CouponRuleConfigV1Entity.getInstance().theTableName(), "crf", "crf.id=cr.setting_id");
        this.where("uc.uid", uid);
        this.where("uc.status", status);
        this.whereIn("crf.rule_type", typeIds);
        this.order(orderBy);
        return this.select();
    }


    public void receiveThirtyPartyCoupon(long uid, long couponId, int amount, long expiredTime) {
        try {
            DataService.getMainDB().beginTransaction(connection -> {
                return receiveThirtyPartyCouponTransaction(connection, uid, couponId, amount, expiredTime);
            });
        } catch (Exception e) {
            return;
        }
    }

    /**
     * @param uid         用户id
     * @param couponId    券id coupon_rule_v1表
     * @param amount      数量
     * @param expiredTime 过期时间 毫秒级时间戳 -1为不过期
     */
    //接收第三方优惠券
    public SyncResult receiveThirtyPartyCouponTransaction(Connection connection, long uid, long couponId, int amount, long expiredTime) {
        CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(couponId);
        if (couponRuleV1Entity == null) {
            return new SyncResult(1, "优惠券不存在，id=" + couponId);
        }
        try {
            Map<String, Object> data = getStringObjectMap(uid, expiredTime, couponRuleV1Entity);
            if (amount > 1) {
                for (int i = 0; i <= amount; i++) {
                    this.insertTransaction(connection, data);
                }
            } else {
                this.insertTransaction(connection, data);
            }
        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }
        return new SyncResult(0, "success");
    }

    @NotNull
    private static Map<String, Object> getStringObjectMap(long uid, long expiredTime, CouponRuleV1Entity couponRuleV1Entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", uid);
        data.put("rule_id", couponRuleV1Entity.id);
        data.put("amount", couponRuleV1Entity.amount);
        data.put("discount", couponRuleV1Entity.discount);
        data.put("charge_duration", couponRuleV1Entity.charge_duration);
        data.put("start_time", TimeUtil.getTimestamp());
        data.put("end_time", expiredTime);
        data.put("create_time", TimeUtil.getTimestamp());
        data.put("status", 0);
        return data;
    }


}