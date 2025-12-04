package com.evcharge.entity.user;

import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EUserRegType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户表;
 *
 * @author : Jay
 * @date : 2022-9-15
 */
public class UserEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 昵称
     */
    public String nickname;
    /**
     * 头像
     */
    public String avatar;
    /**
     * 手机号码
     */
    public String phone;

    /**
     * 分享码
     */
    public String sharecode;
    /**
     * 注册所在省
     */
    public String register_province;
    /**
     * 注册所在市
     */
    public String register_city;
    /**
     * 注册所在区
     */
    public String register_district;

    /**
     * 注册来源
     */
    public int reg_id;

    /**
     * 状态 0正常 1封号 -3重复创建
     */
    public int status;

    /**
     * 机器人标识：0=否，1=使用中，2=待使用,3=内部用户
     */
    public int is_robot;
    /**
     * 备注
     */
    public String memo;
    /**
     * 创建时间戳
     */
    public long createTime;
    /**
     * 更新时间戳
     */
    public long updateTime;

    private final static String SHARECODE_CHARS = "sncGzMJvSEwgiWDtkUemdyuIVaLRAPKpZTjFXrqoNYOChlfxHQ";
    //endregion

    public static UserEntity getInstance() {
        return new UserEntity();
    }

    /**
     * 创建用户token
     *
     * @param uid
     * @return
     */
    public static String createToken(long uid) {
        try {
            return common.md5(Long.toString(uid) + Long.toString(TimeUtil.getTimestamp()));
        } catch (Exception e) {
            LogsUtil.error(e, "【调试信息】%s.%s ,%s", UserEntity.class.getPackageName(), UserEntity.class.getName(), e.getMessage());
            return "";
        }
    }

    /**
     * 根据openid 获取用户信息 2025-09-16 转移到UserService使用
     *
     * @param openId
     * @return
     */
    @Deprecated
    public UserEntity findUserByOpenID(String openId) {

        return this
                //   .cache("UserInfo:" + openId)
                .name("User")
                .alias("u")
                .field("u.id,i.open_id,u.nickname,u.avatar,u.status,u.phone,u.sharecode")
                .leftJoin("UserSourceInfo i", "u.id=i.uid")
                .where("open_id", openId)
                .findEntity();
    }

    /**
     * 根据openid 获取用户信息 2025-09-16 转移到UserService使用
     *
     * @param openId
     * @param regId
     * @return
     */
    @Deprecated
    public UserEntity findUserByOpenID(String openId, int regId) {

        return this
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
     * 根据分享码查找用户信息
     *
     * @param shareCode
     * @return
     */
    public UserEntity findUserByShareCode(String shareCode) {
        return this
                .cache(String.format("User:Info:%s", shareCode), 86400 * 1000)
                .name("User")
                .where("sharecode", shareCode)
                .findEntity();
    }


    /**
     * 根据openid 获取用户信息
     *
     * @param openId
     * @return
     */
    public Map<String, Object> getUserByOpenID(String openId) {
        return this
                //   .cache("UserInfo:" + openId)
                .name("User")
                .alias("u")
                .field("u.id,i.open_id,u.nickname,u.avatar,u.status,u.phone,u.sharecode")
                .leftJoin("UserSourceInfo i", "u.id=i.uid")
                .where("open_id", openId)
                .find();
    }

    /**
     * 根据openid 获取用户信息
     *
     * @param phone
     * @return
     */
    public UserEntity getUserByPhone(String phone) {
        return this
//                .cache(String.format("User:Info:Phone:%s", phone), 86400 * 1000)
                .name("User")
                .where("phone", phone)
                .findEntity();
    }

    /**
     * 根据用户id获取用户信息
     *
     * @param uid
     * @return
     */
    public UserEntity findUserByUid(long uid) {
//        return this.alias("u")
////                .field("u.id,i.open_id,u.nickname,u.avatar,u.status,u.phone,u.sharecode")
////                .join("UserSourceInfo i", "u.id=i.uid")
//                .where("u.id", uid)
//                .findEntity();
        return findUserByUid(uid, false);
    }

    /**
     * 根据用户id获取用户信息
     *
     * @param uid
     * @return
     */
    public UserEntity findUserByUid(long uid, boolean inCache) {
        if (uid <= 0) return null;
        if (inCache) this.cache(String.format("User:Info:%s", uid));
        return this.findEntity(uid);
    }

    /**
     * 获取登录信息
     *
     * @return
     */
    public static SyncResult getLoginInfo() {
        String token = HttpRequestUtil.getHeader("token");
        if (StringUtil.isEmpty(token)) {
//            LogsUtil.warn(UserEntity.class.getSimpleName(), "无法获取头部token参数，正在尝试获取user_token参数");
            HttpRequestUtil.getString("user_token");
        }
        if (StringUtil.isEmpty(token)) {
//            LogsUtil.warn(UserEntity.class.getSimpleName(), "无法获得token值");
            return new SyncResult(99, "登录超时");
        }

        long userId = DataService.getMainCache().getLong(String.format("User:Token:%s", token));
        if (userId == 0) {
            LogsUtil.warn(UserEntity.class.getSimpleName(), "无法读取token信息 - %s", token);
            return new SyncResult(99, "登录超时");
        }

        //token存在，则更新token时间
        UserEntity userInfo = DataService.getMainCache().getObj(String.format("User:Info:%s", userId));
        if (userInfo == null) {
            LogsUtil.warn(UserEntity.class.getSimpleName(), "无法更新token时间 - %s", token);
            return new SyncResult(1, "登录失败");
        } else {
            userInfo = UserEntity.getInstance().findUserByUid(userId);
            if (userInfo == null) {
                LogsUtil.warn(UserEntity.class.getSimpleName(), "用户信息不存在 - %s", userId);
                return new SyncResult(1, "登录失败");
            }
        }

        DataService.getMainCache().setObj(String.format("User:Info:%s", userId), userInfo, 86400 * 1000 * 7);
        DataService.getMainCache().set(String.format("User:Token:%s", token), userId, 86400 * 1000 * 7);
        return new SyncResult(0, "success", userInfo);
    }

    /**
     * 获取用户uid
     *
     * @return 0=未登录
     */
    public static long getUIDWithRequest() {
        String token = HttpRequestUtil.getHeader("token");
        if (!StringUtils.hasLength(token)) return 0;
        return DataService.getMainCache().getLong(String.format("User:Token:%s", token), 0);
    }


    /**
     * 获取当前用户信息
     *
     * @return
     */
    public static UserEntity getUserInfo() {
        String token = HttpRequestUtil.getHeader("token");
        long userId = DataService.getMainCache().getLong(String.format("User:Token:%s", token));
        UserEntity userInfo = DataService.getMainCache().getObj(String.format("User:Info:%s", userId));
        DataService.getMainCache().setObj(String.format("User:Info:%s", userId), userInfo, 86400 * 1000 * 7);
        DataService.getMainCache().set(String.format("User:Token:%s", token), userId, 86400 * 1000 * 7);
        return userInfo;
//        return getUserInfo(EUserRegType.wechat);
    }


    /**
     * 根据用户id获取用户信息
     *
     * @param uid
     * @return
     */
    public Map<String, Object> findWxUserByID(long uid) {
//        return this.alias("u")
//                .field("u.id,i.open_id,u.nickname,u.avatar,u.status")
//                .join("UserSourceInfo i", "u.id=i.uid")
//                .where("u.id", uid)
//                .where("reg_id",regId)
//                .find();
        return findSourceUserByID(uid, EUserRegType.wechatId);
    }


    /**
     * 根据用户id获取用户信息
     *
     * @param uid
     * @return
     */
    public Map<String, Object> findSourceUserByID(long uid, int regId) {
        return this.alias("u")
                .field("u.id,i.open_id,u.nickname,u.avatar,u.status")
                .join("UserSourceInfo i", "u.id=i.uid")
                .where("u.id", uid)
                .where("i.reg_id", regId)
                .find();
    }


    /**
     * 检查手机号码是否已经注册
     *
     * @param phone
     * @return
     */
    public boolean checkPhoneIsReg(String phone) {
        int count = this.where("phone", phone)
                //.cache(String.format("User:%s:RegCount", phone), 300 * 1000)
                .count();
        return count > 0;

    }

    /**
     * 更新用户注册信息
     *
     * @param uid
     * @param deviceCode
     */
    public void updateRegisterInfo(long uid, String deviceCode) {
        UserEntity userEntity = this.findUserByUid(uid);

        if (userEntity == null) return;

        if (StringUtils.hasLength(userEntity.register_province) && StringUtils.hasLength(userEntity.register_city) && StringUtils.hasLength(userEntity.register_district)) {
            return;
        }
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(deviceCode);

        if (chargeStationEntity == null) return;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("register_province", chargeStationEntity.province);
        data.put("register_city", chargeStationEntity.city);
        data.put("register_district", chargeStationEntity.district);

        this.where("id", uid).update(data);
    }


    /**
     * 初始化分享码
     *
     * @param uid
     */
    public String createShareCode(long uid) {
        return common.randomStr(SHARECODE_CHARS, uid + 1000000);
    }

    /**
     * 创建机器人
     *
     * @param connection
     * @return
     */
    public SyncResult createRobotTransaction(Connection connection) {
        //用户信息
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("nickname", "微信用户");
        user.put("avatar", "https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132");
        user.put("create_time", TimeUtil.getTimestamp());
        user.put("status", 0);
        user.put("reg_id", 1);
        user.put("is_robot", 1);

        //微信用户信息
        Map<String, Object> wxUser = new LinkedHashMap<>();
        wxUser.put("nickname", MapUtil.getString(user, "nickname"));
        wxUser.put("avatar", MapUtil.getString(user, "avatar"));
        wxUser.put("gender", 0);
        wxUser.put("city", "");
        wxUser.put("province", "");
        wxUser.put("country", "");
        wxUser.put("create_time", TimeUtil.getTimestamp());
        wxUser.put("open_id", String.format("o4C6w4%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomStr(8)));

        try {
            long uid = DataService.getMainDB().name("User").insertGetIdTransaction(connection, user);
            if (uid == 0) return new SyncResult(1, "插入用户表失败，请稍后再试!");
            wxUser.put("uid", uid);
            if (DataService.getMainDB().name("UserSourceInfo").insertTransaction(connection, wxUser) == 0) {
                return new SyncResult(1, "插入用户信息表失败，请稍后再试");
            }
            return new SyncResult(0, "success");
        } catch (Exception e) {
            return new SyncResult(1, String.format("创建机器人失败，失败原因：%s", e.getMessage()));
        }


    }

    /**
     * 检查是否为测试用户
     *
     * @param uid 用户id
     * @return 是/否
     */
    public boolean isTestUser(long uid) {
        int isTestCode = initCache().getInt(String.format("Test:User:%s", uid), 0);
        if (isTestCode == 9527) return true;
        return false;
    }
}