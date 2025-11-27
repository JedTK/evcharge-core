package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 优惠券表;
 *
 * @author : JED
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopCouponRuleV1Entity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 规则key，用于缓存或其他用读取用途
     */
    public String rule_key;
    /**
     * 规则名
     */
    public String rule_name;
    /**
     * 规则配置ID
     */
    public long setting_id;
    /**
     * 状态：0-下架，1-上架
     */
    public int status;
    /**
     * 是否允许使用：0=关闭使用，1=允许使用
     */
    public int use_status;
    /**
     * 发放总量
     */
    public int count;
    /**
     * 优惠金额：优惠形式为1-金额 时存在值
     */
    public BigDecimal amount;
    /**
     * 优惠金额：随机金额最小值：优惠形式为3-随机金额 时存在值
     */
    public BigDecimal amount_min;
    /**
     * 优惠金额：随机金额最大值：优惠形式为3-随机金额 时存在值
     */
    public BigDecimal amount_max;
    /**
     * 优惠折扣：优惠形式为2-指定折扣 时存在值
     */
    public int discount;
    /**
     * 优惠折扣：随机折扣最小值：优惠形式为4-指定折扣 时存在值
     */
    public int discount_min;
    /**
     * 优惠折扣：随机折扣最大值：优惠形式为4-指定折扣 时存在值
     */
    public int discount_max;
    /**
     * 使用限制：金额限制：满xx元减xx元，满xx元打xx折
     */
    public BigDecimal amount_factor;
    /**
     * 使用限制：购买数量限制：满xx件减xx元，满xx件打xx折
     */
    public int count_factor;
    /**
     * 领取限制：指定那些用户组领取，默认：0
     */
    public int user_group_id;
    /**
     * 领取限制：每个用户能领取的数量
     */
    public int can_get_count;
    /**
     * 其他限制：固定日期：生效时间
     */
    public long start_time;
    /**
     * 其他限制：固定日期：过期时间
     */
    public long end_time;
    /**
     * 其他限制：N天内有效
     */
    public int n_day;
    /**
     * 其他限制：互斥规则：0-不互斥，1-同一规则内互斥 (保留字段)
     */
    public int only_stint;
    /**
     * 权重：用于排序，并优先默认使用，数字越小，权重越大，默认1000
     */
    public int weight;
    /**
     * 规则说明
     */
    public String desc;
    /**
     * 优惠券副标题
     */
    public String subtitle;
    /**
     * 备注
     */
    public String remark;
    /**
     * ip地址
     */
    public String ip;
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
    public static ShopCouponRuleV1Entity getInstance() {
        return new ShopCouponRuleV1Entity();
    }

    /**
     * 根据规则key获取优惠券信息
     *
     * @param ruleKey
     * @return
     */
    public ShopCouponRuleV1Entity getRuleByKey(String ruleKey) {
        return this.cache(String.format("Shop:Coupon:RuleV1Info:%s", ruleKey), 86400 * 1000)
                .where("rule_key", ruleKey)
                .findModel();
    }

    /**
     * 根据规则id获取优惠券信息
     *
     * @param id
     * @return
     */
    public ShopCouponRuleV1Entity getRuleById(long id) {
        return this.cache(String.format("Shop:Coupon:RuleV1Info:%s", id), 86400 * 1000)
                .where("id", id)
                .findModel();
    }


    /**
     * 创建优惠券描述
     *
     * @param setting_id
     * @param couponInfo
     * @return
     */
    public String createCouponDesc(long setting_id, Map<String, Object> couponInfo) {
        //获取优惠券配置
        ShopCouponRuleConfigV1Entity shopCouponRuleConfigV1Entity = ShopCouponRuleConfigV1Entity.getInstance().getCouponConfigV1ById(setting_id);
        if (shopCouponRuleConfigV1Entity == null) {
            LogsUtil.trace("", "获取优惠券配置失败");
            return "";
        }
        String desc = "";
        if (shopCouponRuleConfigV1Entity.amount_use_stint == 1 && shopCouponRuleConfigV1Entity.count_use_stint == 1) {
            desc = String.format("满%s元并满%s件", MapUtil.getDouble(couponInfo, "amount_factor"), MapUtil.getInt(couponInfo, "count_factor"));
        } else {
            if (shopCouponRuleConfigV1Entity.amount_use_stint == 1) {
                desc = String.format("满%s元", MapUtil.getDouble(couponInfo, "amount_factor"));
            }
            if (shopCouponRuleConfigV1Entity.count_use_stint == 1) {
                desc = String.format("满%s件", MapUtil.getInt(couponInfo, "count_factor"));
            }
        }

        switch (shopCouponRuleConfigV1Entity.rule_type) {
            case 1:
                desc = String.format("%s,优惠%s元", desc, MapUtil.getDouble(couponInfo, "amount"));
                break;
            case 2:
                desc = String.format("%s,优惠%s折", desc, MapUtil.getDouble(couponInfo, "discount"));
                break;
            case 3:
                desc = String.format("%s,优惠%s~%s元", desc, MapUtil.getDouble(couponInfo, "amount_min"), MapUtil.getDouble(couponInfo, "amount_min"));
                break;
            case 4:
                desc = String.format("%s,优惠%s~%s折", desc, MapUtil.getDouble(couponInfo, "discount_min"), MapUtil.getDouble(couponInfo, "discount_min"));
                break;
        }

        if (shopCouponRuleConfigV1Entity.count_use_stint == 1) {
            desc = String.format("%s,(每个用户限领 %s张)", desc, MapUtil.getInt(couponInfo, "can_get_count"));
        }

        switch (shopCouponRuleConfigV1Entity.expired_stint) {
            case 0:
                desc = String.format("%s,无期限使用", desc);
                break;
            case 1:
                desc = String.format("%s,有效期：%s~%s"
                        , desc
                        , TimeUtil.toTimeString(MapUtil.getLong(couponInfo, "start_time"), "yyyy-MM-dd")
                        , TimeUtil.toTimeString(MapUtil.getLong(couponInfo, "end_time"), "yyyy-MM-dd"));
                break;
            case 2:
                desc = String.format("%s,领券当日开始%s天内有效", desc, MapUtil.getString(couponInfo, "n_day"));
                break;
            case 3:
                desc = String.format("%s,领券次日开始%s天内有效", desc, MapUtil.getString(couponInfo, "n_day"));
                break;
        }

        if (shopCouponRuleConfigV1Entity.goods_use_stint == 1) {
            desc = String.format("%s,（此优惠券特定商品可用）", desc);
        }

        if (shopCouponRuleConfigV1Entity.user_group_get_stint == 1) {
            desc = String.format("%s,（此优惠券特定用户才能领取）", desc);
        }
        desc = String.format("%s。", desc);
        return desc;
    }


}