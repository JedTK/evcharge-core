package com.evcharge.service.ConsumeCenter;

import com.evcharge.entity.consumecenter.product.DiscountWhitelistEntity;
import com.evcharge.entity.user.UserEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

@Service
public class DiscountWhitelistService {

    /**
     * 添加用户 默认打九折
     *
     * @param phone String
     * @return SyncResult
     */
    public SyncResult add(String phone) {
        return add(phone, 10);
    }


    /**
     * 添加用户
     *
     * @param phone String
     * @param discountRate  int
     * @return SyncResult
     */
    public SyncResult add(String phone, int discountRate) {
        UserEntity userEntity = UserEntity.getInstance().where("phone", phone).findEntity();
        DiscountWhitelistEntity discountWhitelistEntity = new DiscountWhitelistEntity();
        if (userEntity == null) {
            return new SyncResult(1, "用户不存在");
        }
        if (getUserByPhone(phone) != null) {
            return new SyncResult(1, "已经存在白名单");
        }
        discountWhitelistEntity.create_time = TimeUtil.getTimestamp();
        discountWhitelistEntity.phone = phone;
        discountWhitelistEntity.uid = userEntity.id;
        discountWhitelistEntity.status = 1;
        discountWhitelistEntity.discount_rate = discountRate;

        if (discountWhitelistEntity.insertGetId() == 0) {
            return new SyncResult(1, "添加失败");
        }
        return new SyncResult(0, "添加成功");
    }

    /**
     * 获取白名单
     *
     * @param uid long
     * @return DiscountWhitelistEntity
     */
    public DiscountWhitelistEntity getUserByUid(long uid) {
        return DiscountWhitelistEntity.getInstance()
                .cache(String.format("Consume:DiscountWhitelist:%s", uid), 86400 * 1000 * 7)
                .where("uid", uid).findEntity();
    }

    /**
     * 获取白名单
     *
     * @param phone String
     * @return DiscountWhitelistEntity
     */
    public DiscountWhitelistEntity getUserByPhone(String phone) {
        return DiscountWhitelistEntity.getInstance()
                .cache(String.format("Consume:DiscountWhitelist:%s", phone), 86400 * 1000 * 7)
                .where("phone", phone).findEntity();
    }


}
