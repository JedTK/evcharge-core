package com.evcharge.entity.user;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户邀请表;
 *
 * @author : Jay
 * @date : 2022-11-8
 */
public class UserInviteLogEntity extends BaseEntity implements Serializable {
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
     * 被邀请者id
     */
    public long invite_id;
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
    public static UserInviteLogEntity getInstance() {
        return new UserInviteLogEntity();
    }

    /**
     * 插入邀请记录
     *
     * @param inviteUid 邀请人id
     * @param uid 用户id
     */
    public void insertInviteLog(long inviteUid, long uid) {
        Map<String, Object> inviteInfo = new LinkedHashMap<>();
        inviteInfo.put("uid", uid);
        inviteInfo.put("invite_id", inviteUid);
        inviteInfo.put("create_time", TimeUtil.getTimestamp());
        this.insert(inviteInfo);
    }

}