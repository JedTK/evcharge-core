package com.evcharge.entity.recharge;


import com.evcharge.entity.user.UserEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;

/**
 * 充值白名单;
 *
 * @author : Jay
 * @date : 2024-2-1
 */
public class RechargeWhitelistEntity extends BaseEntity implements Serializable {
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
     * 手机号
     */
    public String phone;
    /**
     * 折扣金额
     */
    public int discount_rate;
    /**
     * 状态
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
    public static RechargeWhitelistEntity getInstance() {
        return new RechargeWhitelistEntity();
    }

    /**
     * 添加用户 默认打九折
     *
     * @param phone
     * @return
     */
    public SyncResult add(String phone) {
        return add(phone, 10);
    }


    /**
     * 添加用户
     *
     * @param phone
     * @param discountRate
     * @return
     */
    public SyncResult add(String phone, int discountRate) {
        UserEntity userEntity = UserEntity.getInstance().where("phone", phone).findModel();
        if (userEntity == null) {
            return new SyncResult(1, "用户不存在");
        }
        if (getUserByPhone(phone) != null) {
            return new SyncResult(1, "已经存在白名单");
        }
        this.create_time = TimeUtil.getTimestamp();
        this.phone = phone;
        this.uid = userEntity.id;
        this.status = 1;
        this.discount_rate = discountRate;

        if (this.insertGetId() == 0) {
            return new SyncResult(1, "添加失败");
        }
        return new SyncResult(0, "添加成功");
    }

    /**
     * 获取白名单
     *
     * @param uid
     * @return
     */
    public RechargeWhitelistEntity getUserByUid(long uid) {
        return this
                .cache(String.format("Recharge:Whitelist:%s", uid), 86400 * 1000 * 7)
                .where("uid", uid).findModel();
    }

    /**
     * 获取白名单
     *
     * @param phone
     * @return
     */
    public RechargeWhitelistEntity getUserByPhone(String phone) {
        return this
                .cache(String.format("Recharge:Whitelist:%s", phone), 86400 * 1000 * 7)
                .where("phone", phone).findModel();
    }


}