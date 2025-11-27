package com.evcharge.entity.user.coupon;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.abcbank.ABCBankActiveConfigEntity;
import com.evcharge.entity.coupon.CouponRuleV1Entity;
import com.evcharge.entity.coupon.ThirdPartyCouponConfigEntity;
import com.evcharge.entity.coupon.ThirdPartyCouponDealersEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.utils.SignatureUtils;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户第三方券;
 *
 * @author : JED
 * @date : 2024-8-14
 */
public class UserThirdPartyCouponEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 券商id
     */
    public long dealer_id;
    /**
     * 订单编号
     */
    public String order_no;
    /**
     * 频道id
     */
    public String channel_id;
    /**
     * 优惠券码 唯一码
     */
    public String coupon_code;
    /**
     * 手机号码
     */
    public String phone;
    /**
     * 关联third_party_coupon_config表
     */
    public long config_id;
    /**
     * 关联coupon_rule_v1表
     */
    public long coupon_id;
    /**
     * 接收时间
     */
    public long receive_time;
    /**
     * 过期时间
     */
    public long expired_time;
    /**
     * 状态 0=未领取 1=已领取 2=已核销
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
    public static UserThirdPartyCouponEntity getInstance() {
        return new UserThirdPartyCouponEntity();
    }

    /**
     * 检查是否有获取易动的优惠券
     *
     * @param phone
     */
    public void checkYiDongCoupon(String phone) {
        List<UserThirdPartyCouponEntity> list = this
                .where("dealer_id", 1)
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
            LogsUtil.error(this.getClass().getName(), String.format("更新易动优惠券失败，手机号码=%s，失败原因=%s", phone, e.getMessage()));
        }


    }

    /**
     * 根据优惠券id核销优惠券
     *
     * @param couponId
     */
    public void yiDongWriteOffCouponById(long couponId) {
        UserThirdPartyCouponEntity userThirdPartyCouponEntity = this.where("id", couponId).findEntity();
        if (userThirdPartyCouponEntity == null) {
            LogsUtil.error(this.getClass().getName(), "第三方优惠券核销失败，优惠券id不存在=" + couponId);
            return;
        }
        yiDongWriteOffCoupon(userThirdPartyCouponEntity.coupon_code);
    }

    /**
     * 易动优惠券核销接口
     */
    public void yiDongWriteOffCoupon(String couponCode) {
        UserThirdPartyCouponEntity userThirdPartyCouponEntity = this.where("coupon_code", couponCode).findEntity();
        if (userThirdPartyCouponEntity == null) {
            LogsUtil.error(this.getClass().getName(), "1.优惠券不存在，couponCode=" + couponCode);
            return;
        }
        if (userThirdPartyCouponEntity.status == 2) { //已经核销
            LogsUtil.error(this.getClass().getName(), "2.优惠券已经核销，couponCode=" + couponCode);
            return;
        }
        ThirdPartyCouponDealersEntity thirdPartyCouponDealersEntity = ThirdPartyCouponDealersEntity.getInstance().getInfoByID(1);
        JSONObject configObj = JSONObject.parseObject(thirdPartyCouponDealersEntity.config);

        String secretKey = configObj.getString("secret_key");

        ThirdPartyCouponConfigEntity thirdPartyCouponConfigEntity = ThirdPartyCouponConfigEntity.getInstance()
                .where("id", userThirdPartyCouponEntity.config_id)
                .findEntity();

        CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(thirdPartyCouponConfigEntity.coupon_id);
        if (couponRuleV1Entity == null) {
            LogsUtil.error(this.getClass().getName(), "3.优惠券不存在，couponCode=" + couponCode);
            return;
        }

        JSONObject signData = new JSONObject();
        signData.put("coupon_code", couponRuleV1Entity.rule_key);
        signData.put("channel", userThirdPartyCouponEntity.channel_id);
        signData.put("order_no", userThirdPartyCouponEntity.order_no);
        signData.put("mobile", userThirdPartyCouponEntity.phone);
        signData.put("g_id", thirdPartyCouponConfigEntity.mapping_id);

        String sign = SignatureUtils.generateSignature(signData, secretKey);
        signData.put("sign", sign);
        /**
         * TODO 需要更新生产环境
         */
        //测试环境
//        String url = "https://smchargeapitest.xinquanyu.top/api/ThirdCoupon/yqcNotice";

        //生产环境
        String url = "https://smchargeapi.xinquanyu.top/api/ThirdCoupon/yqcNotice";
        //
        LogsUtil.error(this.getClass().getName(), "参数信息：" + signData);
        String text = Http2Util.post(url, signData);
        if (!StringUtils.hasLength(text)) {
            LogsUtil.error(this.getClass().getName(), "第三方优惠券核销失败，url=" + url);
            return;
        }

        JSONObject json = JSONObject.parseObject(text);
        int code = JsonUtil.getInt(json, "status", -1);
        String msg = json.getString("msg");
        if (code != 1) {
            LogsUtil.error(this.getClass().getName(), "第三方优惠券核销失败，失败原因=" + msg);
            return;
        }
        this.where("id", userThirdPartyCouponEntity.id).update(new LinkedHashMap<>() {{
            put("status", 2);
        }});

    }
    //region 元气充优惠券

    public void sendEvChargeCoupon(long uid, String orderNo) {

        UserEntity userEntity = UserEntity.getInstance().findUserByUid(uid);

        List<ThirdPartyCouponConfigEntity> couponList = ThirdPartyCouponConfigEntity.getInstance()
                .where("dealer_id", 2)
                .selectList();

        if (couponList.isEmpty()) {
            LogsUtil.info(this.getClass().getName(), "无法获取元气充优惠券");
            return;
        }

        for (ThirdPartyCouponConfigEntity thirdPartyCouponConfigEntity : couponList) {
            long couponId = thirdPartyCouponConfigEntity.coupon_id;

            CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(couponId);
            if (couponRuleV1Entity == null) {
                continue;
            }
            long expiredTime = TimeUtil.getTimestamp() + 86400L * 1000 * 30 * 3; //有效期3个月
            Map<String, Object> thirtyCouponData = createThirtyCouponMap(thirdPartyCouponConfigEntity, userEntity, userEntity.phone, orderNo, "evcharge_001", expiredTime);
            Map<String, Object> userPlatformCouponData = new LinkedHashMap<>();

            String couponCode = common.randomStr(16);
            thirtyCouponData.put("coupon_code", couponCode);
            UserThirdPartyCouponEntity.getInstance().insert(thirtyCouponData);


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

        }
        return;

    }


    public void sendABCBankCoupon(long uid, String orderNo, String couponCodes) {
        UserEntity userEntity = UserEntity.getInstance().findUserByUid(uid);
        String[] couponCodeArr = couponCodes.split(",");
        List<ThirdPartyCouponConfigEntity> couponList = ThirdPartyCouponConfigEntity.getInstance()
                .where("dealer_id", 3)
                .whereIn("mapping_id", couponCodeArr)
                .selectList();

        if (couponList.isEmpty()) {
            LogsUtil.info(this.getClass().getName(), "无法获取元气充优惠券");
            return;
        }

        for (ThirdPartyCouponConfigEntity thirdPartyCouponConfigEntity : couponList) {
            long couponId = thirdPartyCouponConfigEntity.coupon_id;

            CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(couponId);
            if (couponRuleV1Entity == null) {
                continue;
            }
            long expiredTime = TimeUtil.getTimestamp() + 86400L * 1000 * 30 * 3; //有效期3个月
            Map<String, Object> thirtyCouponData = createThirtyCouponMap(thirdPartyCouponConfigEntity, userEntity, userEntity.phone, orderNo, "abcbank_005", expiredTime);
            Map<String, Object> userPlatformCouponData = new LinkedHashMap<>();

            String couponCode = common.randomStr(16);
            thirtyCouponData.put("coupon_code", couponCode);
            UserThirdPartyCouponEntity.getInstance().insert(thirtyCouponData);

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
        }

        return;
    }

    /**
     * 根据优惠券码发送优惠券
     *
     * @param userEntity
     * @param orderNo
     * @param mappingId
     */
    public SyncResult sendCoupon(UserEntity userEntity, String orderNo, String mappingId) {


//        List<ThirdPartyCouponConfigEntity> couponList= ThirdPartyCouponConfigEntity.getInstance()
//                .where("dealer_id",2)
//                .selectList();

//        if(couponList.isEmpty()){
//            LogsUtil.info(this.getClass().getName(),"无法获取元气充优惠券");
//            return;
//        }

        ThirdPartyCouponConfigEntity thirdPartyCouponConfigEntity = ThirdPartyCouponConfigEntity.getInstance()
                .where("mapping_id", mappingId).findEntity();
        if(thirdPartyCouponConfigEntity == null) {
            return new SyncResult(1,"优惠券不存在");
        }
        long couponId = thirdPartyCouponConfigEntity.coupon_id;

        CouponRuleV1Entity couponRuleV1Entity = CouponRuleV1Entity.getInstance().getRuleById(couponId);
        if (couponRuleV1Entity == null) {
            return new SyncResult(1, "优惠券不存在");
        }
        long expiredTime = TimeUtil.getTimestamp() + 86400L * 1000 * 30 * 3; //有效期3个月
        Map<String, Object> thirtyCouponData = createThirtyCouponMap(thirdPartyCouponConfigEntity, userEntity, userEntity.phone, orderNo, "evcharge_001", expiredTime);
        Map<String, Object> userPlatformCouponData = new LinkedHashMap<>();

        String couponCode = common.randomStr(16);
        thirtyCouponData.put("coupon_code", couponCode);


        return DataService.getDB().beginTransaction(connection -> {
            long id = UserThirdPartyCouponEntity.getInstance().insertGetId(thirtyCouponData);
            if (id == 0) return new SyncResult(1, "1.优惠券发送失败");

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
            long id1 = UserPlatformCouponEntity.getInstance().insertGetId(userPlatformCouponData);
            if (id1 == 0) return new SyncResult(1, "2.优惠券发送失败");

            return new SyncResult(0, "success");
        });

    }


    @NotNull
    private static Map<String, Object> createThirtyCouponMap(ThirdPartyCouponConfigEntity thirdPartyCouponConfigEntity, UserEntity userEntity, String phone, String orderNo, String channel, long expiredTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dealer_id", thirdPartyCouponConfigEntity.dealer_id);
        data.put("phone", phone);
        data.put("config_id", thirdPartyCouponConfigEntity.id);
        data.put("coupon_id", thirdPartyCouponConfigEntity.coupon_id);
        data.put("receive_time", TimeUtil.getTimestamp());
        data.put("expired_time", expiredTime);
        data.put("create_time", TimeUtil.getTimestamp());
        data.put("order_no", orderNo);
        data.put("channel_id", channel);
        if (userEntity != null) {
            data.put("status", 1);
        } else {
            data.put("status", 0);
        }
        return data;
    }
    //endregion 元气充优惠券


}