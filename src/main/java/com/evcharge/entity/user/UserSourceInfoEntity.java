package com.evcharge.entity.user;

import com.evcharge.enumdata.EUserRegType;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 用户注册源信息;
 *
 * @author : Jay
 * @date : 2022-9-15
 */
public class UserSourceInfoEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 注册来源id
     */
    public int reg_id;
    /**
     * 用户id
     */
    public long uid;
    /**
     * openid
     */
    public String open_id;
    /**
     * 手机号码
     */
    public String phone;

    /**
     * 昵称
     */
    public String nickname;
    /**
     * 用户头像
     */
    public String avatar;
    /**
     * 性别
     */
    public int gender;
    /**
     * 省市
     */
    public String city;
    /**
     * 省份
     */
    public String province;
    /**
     * 国家
     */
    public String country;
    /**
     *
     */
    public String union_id;
    /**
     * 设备号
     */
    public String device_code;
    /**
     * 设备端口
     */
    public String device_port;
    /**
     * 设备id
     */
    public String device_index;
    /**
     * 充电站id
     */
    public long cs_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    public static UserSourceInfoEntity getInstance() {
        return new UserSourceInfoEntity();
    }


    /**
     * 获取用户注册信息
     *
     * @param uid
     * @return
     */
    public UserSourceInfoEntity getInfo(long uid) {
        return this.where("uid", uid)
                .cache(String.format("User:%s:SourceInfo", uid))
                .findEntity();
    }

    public UserSourceInfoEntity getInfo(long uid, int regType) {
        return this
                .where("reg_id", regType)
                .where("uid", uid)
                .cache(String.format("User:%s:%s:SourceInfo", uid, regType))
                .findEntity();
    }

    /**
     * 根据openid获取信息
     *
     * @param openId
     * @return
     */
    public UserSourceInfoEntity getInfoByOpenId(String openId) {
        return this.where("open_id", openId).cache(String.format("User:%s:SourceInfo", openId)).findEntity();
    }
    /**
     * 根据openid获取信息
     *
     * @param openId
     * @return
     */
    public UserSourceInfoEntity getInfoByOpenId(String openId,int regId) {
        return this.where("open_id", openId)
                .where("reg_id",regId)
                .cache(String.format("User:%s:SourceInfo", openId))
                .findEntity();
    }
    /**
     * 根据unionid获取信息
     * @param unionId String
     * @return entity
     */
    public UserSourceInfoEntity getInfoByUnionId(String unionId) {
        return this.where("union_id", unionId)
                .cache(String.format("User:%s:SourceInfo", unionId))
                .findEntity();
    }
}