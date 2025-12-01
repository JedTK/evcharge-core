package com.evcharge.service.User;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserInviteLogEntity;
import com.evcharge.entity.user.UserSourceInfoEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EUserRegType;
import com.evcharge.libsdk.wechat.WechatSDK;
import com.evcharge.service.Thirty.ThirtyPartyCouponService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {

    public static String createToken(long uid) {
        try {
            return String.valueOf(common.md5(Long.toString(uid) + Long.toString(TimeUtil.getTimestamp())));
        } catch (Exception e) {
            LogsUtil.error(e, "【调试信息】%s.%s ,%s", UserEntity.class.getPackageName(), UserEntity.class.getName(), e.getMessage());
            return "";
        }
    }

    public static void setUserEntityCache(String token, UserEntity userEntity) {
        DataService.getMainCache().setObj(String.format("User:Info:%s", userEntity.id), userEntity, 86400 * 1000 * 7);
        DataService.getMainCache().set(String.format("User:Token:%s", token), userEntity.id, 86400 * 1000 * 7);
    }

    public static void delUserEntityCache(String token, long uid) {
        DataService.getMainCache().del(String.format("User:Info:%s", uid));
        if (token != null) {
            DataService.getMainCache().del(String.format("User:Token:%s", token));
        }
    }

    /**
     * 根据微信授权码获取手机号并更新用户数据。
     *
     * @param userId 用户的ID
     * @param code   微信授权码
     * @return 更新后的用户信息
     */
//    @Deprecated
//    public UserEntity authorizeAndUpdatePhone(long userId, String code) {
//        // 1. 获取微信手机号信息
//        WechatSDK wechatSDK = new WechatSDK();
//        SyncResult result = wechatSDK.getPhoneInfo(code);
//        if (result.getCode() != 0) {
//            // 封装业务异常，便于控制器统一处理
//            throw new RuntimeException("获取微信手机信息失败：" + result.getMsg());
//        }
//
//        JSONObject phoneInfo = (JSONObject) result.getData();
//
//        String phoneNumber = phoneInfo.getString("phoneNumber");
//        // 2. 检查用户是否已绑定手机号，如果已绑定则直接返回
//        UserSourceInfoEntity sourceInfo = UserSourceInfoEntity.getInstance().getInfo(userId, EUserRegType.wechatId);
//        if (sourceInfo != null && StringUtils.hasLength(sourceInfo.phone)) {
//            return this.findUserByUid(userId);
//        }
//
//        // 3. 处理手机号已注册的情况
//        UserEntity existingUserWithPhone = UserEntity.getInstance().getUserByPhone(phoneNumber);
//        if (existingUserWithPhone != null) {
//            // 如果手机号已被注册，将当前微信用户的数据合并到该手机号用户下
//            if(existingUserWithPhone.id != userId && existingUserWithPhone.regId!=EUserRegType.wechatId){
//                mergeUserAccounts(userId, existingUserWithPhone, phoneNumber);
//                return existingUserWithPhone;
//            }
//
//        }
//
//        // 4. 新手机号，正常更新
//        updateUserAndSourceInfoWithPhone(userId, phoneNumber);
//
//        // 5. 检查第三方派券
//        ThirtyPartyCouponService.getInstance().checkCoupon(phoneNumber);
//
//        return this.findUserByUid(userId);
//
//    }
    public SyncResult authorizeAndUpdatePhone(long userId, String code, int regId) {
        WechatSDK wechatSDK = new WechatSDK();
        SyncResult result = wechatSDK.getPhoneInfo(code);
        if (result.getCode() != 0) {
            // 封装业务异常，便于控制器统一处理
            throw new RuntimeException("获取微信手机信息失败：" + result.getMsg());
        }

        JSONObject phoneInfo = (JSONObject) result.getData();

        String phoneNumber = phoneInfo.getString("phoneNumber");

        UserSourceInfoEntity sourceInfo = UserSourceInfoEntity.getInstance().getInfo(userId, EUserRegType.wechatId);
        if (sourceInfo != null && StringUtils.hasLength(sourceInfo.phone)) {
            return new SyncResult(0, "success", userId);
        }

        UserEntity existingUserWithPhone = UserEntity.getInstance().getUserByPhone(phoneNumber);

        if (existingUserWithPhone != null) {

            if (existingUserWithPhone.reg_id == 1) {
                return new SyncResult(1, "手机号码已被注册！");
            }
            if (UserSourceInfoEntity.getInstance().where("uid", "<>", userId).where("phone", phoneNumber)
                    .where("reg_id", 1).count() > 0) {
                return new SyncResult(1, "手机号码已被注册！");
            }
            if (existingUserWithPhone.reg_id == 2) {
                mergeUserAccounts(userId, existingUserWithPhone, phoneNumber);
                return new SyncResult(0, "success", existingUserWithPhone.id);
            }

        }
        // 4. 新手机号，正常更新
        updateUserAndSourceInfoWithPhone(userId, phoneNumber);
        // 5. 检查第三方派券
        ThirtyPartyCouponService.getInstance().checkCoupon(phoneNumber);
        return new SyncResult(0, "success", userId);
    }


    public SyncResult authorizeAndUpdatePhoneForAlipay(long userId, String phoneNumber) {
//        WechatSDK wechatSDK = new WechatSDK();
//        SyncResult result = wechatSDK.getPhoneInfo(code);
//        if (result.getCode() != 0) {
//            // 封装业务异常，便于控制器统一处理
//            throw new RuntimeException("获取微信手机信息失败：" + result.getMsg());
//        }
//
//        JSONObject phoneInfo = (JSONObject) result.getData();
//
//        String phoneNumber = phoneInfo.getString("phoneNumber");

        UserSourceInfoEntity sourceInfo = UserSourceInfoEntity.getInstance().getInfo(userId, EUserRegType.alipayId);
        if (sourceInfo != null && StringUtils.hasLength(sourceInfo.phone)) {
            return new SyncResult(0, "success", userId);
        }

        UserEntity existingUserWithPhone = UserEntity.getInstance().getUserByPhone(phoneNumber);

        if (existingUserWithPhone != null) {

            if (existingUserWithPhone.reg_id == EUserRegType.alipayId) {
                return new SyncResult(1, "手机号码已被注册！");
            }
            if (UserSourceInfoEntity.getInstance().where("uid", "<>", userId).where("phone", phoneNumber)
                    .where("reg_id", 2).count() > 0) {
                return new SyncResult(1, "手机号码已被注册！");
            }
            if (existingUserWithPhone.reg_id == 1) {
                mergeUserAccounts(userId, existingUserWithPhone, phoneNumber);
                return new SyncResult(0, "success", existingUserWithPhone.id);
            }

        }
        // 4. 新手机号，正常更新
        updateUserAndSourceInfoWithPhone(userId, phoneNumber);
        // 5. 检查第三方派券
        ThirtyPartyCouponService.getInstance().checkCoupon(phoneNumber);
        return new SyncResult(0, "success", userId);
    }

    /**
     * 将当前微信用户数据合并到已存在的手机号用户下。
     *
     * @param currentUserId 当前微信用户的ID
     * @param existingUser  已存在的手机号用户
     * @param phoneNumber   手机号
     */
    private void mergeUserAccounts(long currentUserId, UserEntity existingUser, String phoneNumber) {
        // 更新当前微信用户的注册信息，将手机号关联到已存在的用户ID
        Map<String, Object> updateSourceInfoData = new LinkedHashMap<>();
        updateSourceInfoData.put("uid", existingUser.id);
        updateSourceInfoData.put("phone", phoneNumber);
        UserSourceInfoEntity.getInstance().where("uid", currentUserId).update(updateSourceInfoData);

        // 将当前微信用户账号状态改为废弃，并备注
        Map<String, Object> updateUserStatusData = new LinkedHashMap<>();
        updateUserStatusData.put("status", -3);
        updateUserStatusData.put("memo", "手机号已存在，合并到用户ID：" + existingUser.id);
        UserEntity.getInstance().where("id", currentUserId).update(updateUserStatusData);
    }


    /**
     * 为新手机号用户更新数据。
     *
     * @param userId      用户ID
     * @param phoneNumber 手机号
     */
    private void updateUserAndSourceInfoWithPhone(long userId, String phoneNumber) {
        // 更新 User 表
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("phone", phoneNumber);
        userData.put("sharecode", UserEntity.getInstance().createShareCode(userId));
        UserEntity.getInstance().where("id", userId).update(userData);

        // 更新 UserSourceInfo 表
        Map<String, Object> sourceInfoData = new LinkedHashMap<>();
        sourceInfoData.put("phone", phoneNumber);
        UserSourceInfoEntity.getInstance().where("uid", userId).update(sourceInfoData);
    }

    /**
     * 根据微信的 openId 或 unionId 查找用户。
     * 优先使用 openId 查找，如果未找到，则尝试使用 unionId。
     *
     * @param openId  微信小程序的 openId
     * @param unionId 微信用户的 unionId
     * @return 找到的用户实体，如果未找到则返回 null
     */
    public UserEntity findUserByOpenIdOrUnionId(String openId, String unionId) {
        // 1. 优先根据 openId 和注册类型查找用户
        return this.findUserByOpenID(openId, EUserRegType.wechatId);

        // 2. 如果 openId 未找到，则尝试根据 unionId 查找
//        if (StringUtils.hasLength(unionId)) {
//            UserSourceInfoEntity sourceInfoByUnionId = UserSourceInfoEntity.getInstance().getInfoByUnionId(unionId);
//            if (sourceInfoByUnionId != null) {
//                return this.findUserByUid(sourceInfoByUnionId.id);
//            }
//        }
    }

    public UserEntity findUserByUnionId(String unionId) {
        // 1. 优先根据 openId 和注册类型查找用户
//        return this.findUserByOpenID(openId, EUserRegType.wechatId);

        // 2. 如果 openId 未找到，则尝试根据 unionId 查找
        if (StringUtils.hasLength(unionId)) {
            UserSourceInfoEntity sourceInfoByUnionId = UserSourceInfoEntity.getInstance().getInfoByUnionId(unionId);
            if (sourceInfoByUnionId != null) {
                return this.findUserByUid(sourceInfoByUnionId.uid);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    public UserSourceInfoEntity findUserSourceInfoByUnionId(String unionId) {
        // 1. 优先根据 openId 和注册类型查找用户
//        return this.findUserByOpenID(openId, EUserRegType.wechatId);

        // 2. 如果 openId 未找到，则尝试根据 unionId 查找
        if (StringUtils.hasLength(unionId)) {
            return UserSourceInfoEntity.getInstance().getInfoByUnionId(unionId);
        } else {
            return null;
        }
    }


    /**
     * 根据openid 获取用户信息
     *
     * @param openId
     * @return
     */
    public UserEntity findUserByOpenID(String openId) {
        return findUserByOpenID(openId, EUserRegType.wechatId);
    }


    /**
     * 根据openid 获取用户信息
     *
     * @param openId
     * @return
     */
    public UserEntity findUserByOpenID(String openId, int regId) {
        return UserEntity.getInstance()
                //   .cache("UserInfo:" + openId)
                .name("User")
                .alias("u")
                .field("u.id,i.open_id,u.nickname,u.avatar,u.status,u.phone,u.sharecode")
                .leftJoin("UserSourceInfo i", "u.id=i.uid")
                .where("i.open_id", openId)
                .where("i.reg_id", regId)
                .findEntity();
    }


    /**
     * 根据用户id获取用户信息
     *
     * @param uid
     * @return
     */
    public UserEntity findUserByUid(Long uid) {
        return UserEntity.getInstance().alias("u")
//                .field("u.id,i.open_id,u.nickname,u.avatar,u.status,u.phone,u.sharecode")
//                .join("UserSourceInfo i", "u.id=i.uid")
                .where("u.id", uid)
                .findEntity();
    }


    public void updateUnionId(long userId, String unionId) {
        UserSourceInfoEntity userSourceInfoEntity = UserSourceInfoEntity.getInstance().where("uid", userId)
                .findEntity();
        if (!StringUtils.hasLength(userSourceInfoEntity.union_id) && StringUtils.hasLength(unionId)) {
            UserSourceInfoEntity.getInstance().where("id", userSourceInfoEntity.id).update(new LinkedHashMap<>() {{
                put("union_id", unionId);
            }});
        }
    }

    /**
     * 处理微信授权登录，查找或创建用户。
     *
     * @param wxInfo     微信用户信息
     * @param sharecode  邀请码
     * @param deviceCode 设备信息
     * @return 登录后的UserEntity
     */
    public UserEntity handleWechatLogin(JSONObject wxInfo, String sharecode, String deviceCode, String devicePort, String deviceIndex, long csId) {
//        String unionId = wxInfo.getString("unionId");

        // 1. 检查是否存在unionId关联的用户
//        UserSourceInfoEntity userByUnionId = UserSourceInfoEntity.getInstance().getInfoByUnionId(unionId);

//        if (userByUnionId != null) {
//            // 手机号用户合并或已存在，直接返回
//            return this.findUserByUid(userByUnionId.uid);
//        }

        // 2. 检查是否存在openid关联的用户
        UserSourceInfoEntity userByOpenId = UserSourceInfoEntity.getInstance().where("open_id", wxInfo.getString("openId")).findEntity();

        if (userByOpenId != null) {
            // 更新用户信息，并返回
            UserSourceInfoEntity.getInstance().where("id", userByOpenId.id).update(createWxUserMap(wxInfo, deviceCode, devicePort, deviceIndex, csId));
            return this.findUserByUid(userByOpenId.uid);
        }

        // 3. 用户不存在，进行新用户注册
        return registerNewWechatUser(wxInfo, sharecode, deviceCode, devicePort, deviceIndex, csId);
    }

    private UserEntity registerNewWechatUser(JSONObject wxInfo, String sharecode, String deviceCode, String devicePort, String deviceIndex, long csId) {
        // 创建主用户信息
        Map<String, Object> userMap = createBaseUserMap(wxInfo, deviceCode, devicePort, deviceIndex, csId);
        long uid = DataService.getMainDB().name("User").insertGetId(userMap);
        if (uid == 0) {
            throw new RuntimeException("用户注册失败");
        }

        // 更新注册信息
        UserEntity.getInstance().updateRegisterInfo(uid, deviceCode);

        // 创建微信用户信息
        Map<String, Object> wxUserMap = createWxUserMap(wxInfo, deviceCode, devicePort, deviceIndex, csId);
        wxUserMap.put("uid", uid);
        DataService.getMainDB().name("UserSourceInfo").insert(wxUserMap);

        // 处理邀请关系
        if (StringUtils.hasLength(sharecode)) {
            UserEntity inviteUser = UserEntity.getInstance().where("sharecode", sharecode).findEntity();
            if (inviteUser != null) {
                UserInviteLogEntity.getInstance().insertInviteLog(inviteUser.id, uid);
            }
        }

        // 初始化用户统计数据
        UserSummaryEntity.getInstance().initSummary(uid);

        return this.findUserByUid(uid);
    }

    // 辅助方法，用于创建用户数据Map
    private Map<String, Object> createBaseUserMap(JSONObject wxInfo, String deviceCode, String devicePort, String deviceIndex, long csId) {
        // ... 创建 userMap 的逻辑
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("nickname", wxInfo.get("nickName"));
        user.put("avatar", wxInfo.get("avatarUrl"));
        user.put("create_time", TimeUtil.getTimestamp());
        user.put("status", 0);
        user.put("reg_id", EUserRegType.wechatId);

        user.put("device_code", deviceCode);
        user.put("device_port", devicePort);
        user.put("device_index", deviceIndex);
        user.put("cs_id", csId);
        user.put("create_ip", HttpRequestUtil.getIP());

        return user;
    }

    // 辅助方法，用于创建微信用户数据Map
    private Map<String, Object> createWxUserMap(JSONObject wxInfo, String deviceCode, String devicePort, String deviceIndex, long csId) {
        // ... 创建 wxUserMap 的逻辑
        Map<String, Object> wxUser = new LinkedHashMap<>();
        wxUser.put("nickname", wxInfo.get("nickName"));
        wxUser.put("avatar", wxInfo.get("avatarUrl"));
        wxUser.put("gender", wxInfo.get("gender"));
        wxUser.put("city", wxInfo.get("city"));
        wxUser.put("province", wxInfo.get("province"));
        wxUser.put("country", wxInfo.get("country"));

        wxUser.put("open_id", wxInfo.get("openId"));
        wxUser.put("union_id", wxInfo.get("unionId"));
        wxUser.put("reg_id", EUserRegType.wechatId);
        wxUser.put("device_code", deviceCode);
        wxUser.put("device_port", devicePort);
        wxUser.put("device_index", deviceIndex);
        wxUser.put("cs_id", csId);
        return wxUser;
    }


}
