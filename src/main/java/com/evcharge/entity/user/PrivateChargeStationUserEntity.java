package com.evcharge.entity.user;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.Map;

/**
 * 私有充电桩用户表;
 *
 * @author : JED
 * @date : 2023-7-31
 */
public class PrivateChargeStationUserEntity extends BaseEntity implements Serializable {
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
     * 手机号码
     */
    public long phone;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PrivateChargeStationUserEntity getInstance() {
        return new PrivateChargeStationUserEntity();
    }

    /**
     * 检查授权
     *
     * @param CSId 充电桩id
     * @param uid  用户id
     * @return 是否授权
     */
    public boolean checkAuth(long CSId, long uid) {
        //检查用户信息
        UserEntity userEntity = UserEntity.getInstance().findUserByUid(uid);
        if (userEntity == null || userEntity.id == 0) return false;

//        Map<String, Object> data = this.field("id,CSId")
////                .cache(String.format("ChargeStation:%s:Private:User:%s_%s", CSId, userEntity.phone, userEntity.id))
//                .where("CSId", CSId)
//                .where("(", "uid", "=", uid, "")
//                .whereOr("phone", "", userEntity.phone, ")")
//                .find();

        return this.field("id,CSId")
                .where("CSId", CSId)
                .where("(", "uid", "=", uid, "")
                .whereOr("phone", "=", userEntity.phone, ")")
                .exist();
    }
}
