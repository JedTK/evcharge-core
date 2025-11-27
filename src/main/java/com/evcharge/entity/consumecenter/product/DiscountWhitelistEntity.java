package com.evcharge.entity.consumecenter.product;


import com.evcharge.entity.user.UserEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
/**
 * 折扣优惠白名单;
 *
 * @author : Jay
 * @date : 2025-11-24
 */
@TargetDB("evcharge_consumecenter")
public class DiscountWhitelistEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 手机号,;
     */
    public String phone;
    /**
     * 折扣金额,;
     */
    public int discount_rate;
    /**
     * 状态,;
     */
    public int status;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static DiscountWhitelistEntity getInstance() {
        return new DiscountWhitelistEntity();
    }



}