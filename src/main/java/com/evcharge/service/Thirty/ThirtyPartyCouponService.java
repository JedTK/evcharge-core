package com.evcharge.service.Thirty;

import com.evcharge.entity.coupon.CouponRuleV1Entity;
import com.evcharge.entity.coupon.ThirdPartyCouponDealersEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.entity.user.coupon.UserPlatformCouponEntity;
import com.evcharge.entity.user.coupon.UserThirdPartyCouponEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ThirtyPartyCouponService {

    public static ThirtyPartyCouponService getInstance() {
        return new ThirtyPartyCouponService();
    }


    public void checkCoupon(String phone) {
        List<ThirdPartyCouponDealersEntity> list = ThirdPartyCouponDealersEntity.getInstance()
                .where("status", 0)
                .selectList();

        if (list.isEmpty()) return;

        for (ThirdPartyCouponDealersEntity thirdPartyCouponDealersEntity : list) {
            checkDealerCoupon(thirdPartyCouponDealersEntity, phone);
        }
    }

    public void checkDealerCoupon(ThirdPartyCouponDealersEntity thirdPartyCouponDealersEntity, String phone) {
        List<UserThirdPartyCouponEntity> list = UserThirdPartyCouponEntity.getInstance()
                .where("dealer_id", thirdPartyCouponDealersEntity.id)
                .where("phone", phone)
                .where("status", 0)
                .selectList();

        if (list.isEmpty()) {
            return;
        }
        UserEntity userEntity = UserEntity.getInstance().getUserByPhone(phone);
        if (userEntity == null) {
            return;
        }
        try {
            long expiredTime = TimeUtil.getTimestamp() + 86400L * 1000 * 30;
            if (thirdPartyCouponDealersEntity.id == 3) {
                expiredTime = TimeUtil.getTimestamp() + 86400L * 1000 * 30 * 3;
            }
            for (UserThirdPartyCouponEntity userThirdPartyCouponEntity : list) {
                CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(userThirdPartyCouponEntity.coupon_id);
                if (couponRuleV1Entity == null) {
                    continue;
                }
                String couponCode = common.randomStr(16);
                Map<String, Object> userPlatformCouponData = new LinkedHashMap<>();
                userPlatformCouponData.put("uid", userEntity.id);
                userPlatformCouponData.put("rule_id", couponRuleV1Entity.id);
                userPlatformCouponData.put("amount", couponRuleV1Entity.amount);
                userPlatformCouponData.put("discount", couponRuleV1Entity.discount);
                userPlatformCouponData.put("charge_duration", couponRuleV1Entity.charge_duration);
                userPlatformCouponData.put("start_time", TimeUtil.getTimestamp());
                userPlatformCouponData.put("end_time", expiredTime);
                userPlatformCouponData.put("create_time", TimeUtil.getTimestamp());
                userPlatformCouponData.put("status", 0);
                userPlatformCouponData.put("coupon_code", couponCode);
                UserPlatformCouponEntity.getInstance().insert(userPlatformCouponData);
                UserThirdPartyCouponEntity.getInstance().where("id", userThirdPartyCouponEntity.id).update(new LinkedHashMap<>() {{
                    put("status", 1);
                }});
                UserSummaryEntity.getInstance().updateUserCouponCount(userEntity.id, list.size());
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), String.format("更新" + thirdPartyCouponDealersEntity.name + "优惠券失败，手机号码=%s，失败原因=%s", phone, e.getMessage()));
        }


    }

}
