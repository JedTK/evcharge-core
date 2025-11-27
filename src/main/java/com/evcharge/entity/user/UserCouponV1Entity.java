package com.evcharge.entity.user;

import com.evcharge.entity.shop.*;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import lombok.NonNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户优惠券表;
 *
 * @author : Jay
 * @date : 2022-12-21
 */
public class UserCouponV1Entity extends BaseEntity implements Serializable {

    public static final int CouponStatusUnused = 1;//未使用
    public static final int CouponStatusUsed = 2;//已使用
    public static final int CouponStatusExpired = 3;//已过期
    public static final int CouponStatusWaitPaymentUse = 4;//待支付使用中
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
     * 优惠券名
     */
    public String title;
    /**
     * 副标题,优惠券副标题
     */
    public String subtitle;
    /**
     * 规则id
     */
    public long rule_id;
    /**
     * 状态：1-未使用，2-已使用，3-已过期
     */
    public int status;
    /**
     * 优惠金额
     */
    public BigDecimal coupon_amount;
    /**
     * 优惠折扣
     */
    public int discount;
    /**
     * 订单id
     */
    public long order_id;
    /**
     * 生效时间
     */
    public long start_time;
    /**
     * 过期时间：-1 不过期
     */
    public long end_time;
    /**
     * 权重：用于排序，并优先默认使用，数字越小，权重越大，默认500
     */
    public long weight;
    /**
     * 使用时间
     */
    public long use_time;
    /**
     * ip地址
     */
    public String ip;
    /**
     * 领取时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * 管理员id
     */
    public long admin_id;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static UserCouponV1Entity getInstance() {
        return new UserCouponV1Entity();
    }

    /**
     * 领取优惠券
     *
     * @param uid
     * @param ruleKey
     * @return
     */
    public SyncResult receive(long uid, String ruleKey) {
        return this.receive(uid, ruleKey, 0);
    }

    /**
     * 领取优惠券（后台管理员派发优惠券）
     *
     * @param uid
     * @param ruleKey
     * @return
     */
    public SyncResult receive(long uid, String ruleKey, long admin_id) {
        ShopCouponRuleV1Entity shopCouponRuleV1Entity = ShopCouponRuleV1Entity.getInstance().getRuleByKey(ruleKey);
        if (shopCouponRuleV1Entity == null) {
            return new SyncResult(1, "请检查优惠券规则配置");
        }
        if (shopCouponRuleV1Entity.status == 0) {
            return new SyncResult(1, "此优惠活动已关闭");
        }
        ShopCouponRuleConfigV1Entity shopCouponRuleConfigV1Entity = ShopCouponRuleConfigV1Entity.getInstance().getCouponConfigV1ById(shopCouponRuleV1Entity.setting_id);
        //数量领取限制：0-不限制，1-指定领取数量
        long count = 0;
        long ruleId = shopCouponRuleV1Entity.id;
        if (shopCouponRuleConfigV1Entity.count_use_stint == 1) {
            count = this.where("uid", uid).where("rule_id", ruleId).count();
            if (count >= shopCouponRuleV1Entity.can_get_count) {
                return new SyncResult(1, String.format("每个用户限领%s张", shopCouponRuleV1Entity.can_get_count));
            }
        }
        //用户组领取限制：0-不限制，1-限制用户组
        //发放总量：0-不限制，1-限制
        if (shopCouponRuleConfigV1Entity.count_get_stint == 1) {
            count = this.where("uid", uid).where("rule_id", ruleId).count();
            if (count > shopCouponRuleV1Entity.can_get_count) {
                return new SyncResult(1, "优惠券已领取完毕");
            }
        }

        double couponAmount = 0;
        double discount = 0;

        switch (shopCouponRuleConfigV1Entity.rule_type) {
            case 1:
                couponAmount = shopCouponRuleV1Entity.amount.doubleValue();
                break;
            case 2:
                discount = shopCouponRuleV1Entity.discount;
                break;
            case 3:
                couponAmount = common.randomDouble(shopCouponRuleV1Entity.amount_min.doubleValue(), shopCouponRuleV1Entity.amount_max.doubleValue());
                break;
            case 4:
                discount = common.randomDouble(shopCouponRuleV1Entity.discount_min, shopCouponRuleV1Entity.discount_max);
                break;
        }

        long startTime = 0;
        long endTime = 0;
        //有效期限制：0-无期限，1-固定日期，2-领取卷当日开始N天内有效，3-领到卷次日开始N天内有效
        switch (shopCouponRuleConfigV1Entity.expired_stint) {
            case 0:
                startTime = TimeUtil.getTimestamp();
                // endTime = 0; //2033-12-31 00:00:00
                break;
            case 1: //固定日期
                startTime = shopCouponRuleV1Entity.start_time;
                endTime = shopCouponRuleV1Entity.end_time;
                break;
            case 2:
                startTime = TimeUtil.getTimestamp();
                endTime = TimeUtil.getAddDayTimestamp(startTime, shopCouponRuleV1Entity.n_day);
                break;
            case 3:
                startTime = TimeUtil.getTimestamp();
                endTime = TimeUtil.getAddDayTimestamp(startTime + 86400 * 1000, shopCouponRuleV1Entity.n_day);
                break;

        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", uid);
        data.put("title", shopCouponRuleV1Entity.rule_name);
        data.put("subtitle", shopCouponRuleV1Entity.subtitle);
        data.put("rule_id", ruleId);
        data.put("status", CouponStatusUnused);//状态：1-未使用，2-已使用，3-已过期
        data.put("coupon_amount", couponAmount);
        data.put("discount", discount);
        data.put("order_id", 0);
        data.put("create_time", TimeUtil.getTimestamp());
        data.put("start_time", startTime);
        data.put("end_time", endTime);
        data.put("use_time", 0);
        data.put("admin_id", admin_id);
        long r = this.insertGetId(data);

        if (r == 0) {
            return new SyncResult(1, "领取失败，请稍后再试");
        }
        UserSummaryEntity.getInstance().updateUserCouponCount(uid, 1);
        return new SyncResult(0, String.format("获得一张%s优惠券", shopCouponRuleV1Entity.rule_name));
    }

    /**
     * 检查优惠券是否可以使用
     *
     * @param userCouponV1Entity
     * @param shopOrderEntity
     * @param goodsArr
     * @param goodsTypeArr
     * @return
     */
    public SyncResult checkCoupon(@NonNull UserCouponV1Entity userCouponV1Entity, ShopOrderEntity shopOrderEntity, List<Map<String, Object>> goodsArr, List<Map<String, Object>> goodsTypeArr) {

        switch (userCouponV1Entity.status) {
            case CouponStatusExpired:
                return new SyncResult(1, "优惠券已过期");
            case CouponStatusUsed:
                return new SyncResult(1, "优惠券已使用");
            case CouponStatusWaitPaymentUse:
                return new SyncResult(1, "优惠券锁定中");
        }
        ShopCouponRuleV1Entity shopCouponRuleV1Entity = ShopCouponRuleV1Entity.getInstance().getRuleById(userCouponV1Entity.rule_id);

        if (shopCouponRuleV1Entity == null) return new SyncResult(1, "请检查优惠活动key");

        if (shopCouponRuleV1Entity.use_status != 1) return new SyncResult(1, "优惠券不允许使用");

        ShopCouponRuleConfigV1Entity shopCouponRuleConfigV1Entity = ShopCouponRuleConfigV1Entity.getInstance().getCouponConfigV1ById(shopCouponRuleV1Entity.setting_id);

        if (shopCouponRuleConfigV1Entity == null) return new SyncResult(1, "请检查优惠券规则配置");

        //expired_stint
        if (shopCouponRuleConfigV1Entity.expired_stint == 1) { //如果是有效期之内
            //检查优惠券是否在有效期内
            if (userCouponV1Entity.start_time > TimeUtil.getTimestamp()) {
                return new SyncResult(1, "优惠券还没生效");
            }
            if (userCouponV1Entity.end_time < TimeUtil.getTimestamp()) {
                Map<String, Object> update = new LinkedHashMap<>();
                update.put("status", 3);
                this.where("id", userCouponV1Entity.id).update(update);
                return new SyncResult(1, "优惠券已过期");
            }
        }


        //检查使用限制
        //金额使用限制：0-不限制，1-满多少元可用
        if (shopCouponRuleConfigV1Entity.amount_use_stint == 1) {
            if (shopOrderEntity.total_price.doubleValue() < shopCouponRuleV1Entity.amount_factor.doubleValue()) {
                return new SyncResult(1, String.format("金额未满%s元", shopCouponRuleV1Entity.amount_factor));
            }
        }
        //购买量使用限制：0-不限制，1-满多少件可用
        if (shopCouponRuleConfigV1Entity.count_use_stint == 1) {
            if (shopOrderEntity.goods_amount < shopCouponRuleV1Entity.count_factor) {
                return new SyncResult(1, "购买未满%s件", shopCouponRuleV1Entity.count_factor);
            }
        }
        /**
         * TODO 用户VIP
         */
        //指定商品使用限制：0-不限制，1-限制  (后期添加指定商品类型限制)
        boolean goodsUse = true;
        if (shopCouponRuleConfigV1Entity.goods_use_stint == 1) {
            goodsUse = false;
            List<Map<String, Object>> goods = ShopCouponToGoodsLimitV1Entity.getInstance()
                    .where("rule_id", shopCouponRuleV1Entity.id)
                    .select();

            if (goods.size() > 0) {
                Iterator it = goods.iterator();
                Iterator goodsIt = goodsArr.iterator();
                while (it.hasNext()) {
                    Map<String, Object> nd = (Map<String, Object>) it.next();
                    while (goodsIt.hasNext()) {
                        Map<String, Object> goodsNd = (Map<String, Object>) goodsIt.next();
                        if (MapUtil.getLong(nd, "goods_id") == MapUtil.getLong(goodsNd, "goods_id")) {
                            goodsUse = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!goodsUse) {
            return new SyncResult(1, "限制商品使用");
        }

        if (shopCouponRuleConfigV1Entity.goods_type_use_stint == 1) {
            goodsUse = false;
            List<Map<String, Object>> goodsType = ShopCouponToGoodsTypeLimitV1Entity.getInstance()
                    .where("rule_id", shopCouponRuleV1Entity.id)
                    .select();

            if (goodsType.size() > 0) {
                Iterator it = goodsType.iterator();
                Iterator goodsTypeIt = goodsTypeArr.iterator();
                while (it.hasNext()) {
                    Map<String, Object> nd = (Map<String, Object>) it.next();
                    while (goodsTypeIt.hasNext()) {
                        Map<String, Object> goodsNd = (Map<String, Object>) goodsTypeIt.next();
                        if (MapUtil.getLong(nd, "goods_type_id") == MapUtil.getLong(goodsNd, "type_id")) {
                            goodsUse = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!goodsUse) {
            return new SyncResult(1, "限制商品类型使用");
        }
        return new SyncResult(0, "success");
    }

    /**
     * 消耗优惠券
     *
     * @param couponId
     * @return
     */
    public SyncResult consume(long couponId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", CouponStatusUsed);
        data.put("use_time", TimeUtil.getTimestamp());

        long r = this.where("id", couponId).update(data);
        if (r > 0) {
            //删除缓存
            DataService.getMainCache().del(String.format("User:Shop:CouponInfo:%s", couponId));
            UserSummaryEntity.getInstance().updateUserCouponCount(uid, -1);
            return new SyncResult(0, "success");
        }
        ;

        LogsUtil.trace("", String.format("消耗优惠券失败，优惠券ID=%s", couponId));
        return new SyncResult(1, "消耗失败，请稍后再试");

    }

    public UserCouponV1Entity getCouponInfoById(long couponId) {

        return this.cache(String.format("User:Shop:CouponInfo:%s", couponId), 86400 * 1000)
                .where("id", couponId)
                .findEntity();

    }
}