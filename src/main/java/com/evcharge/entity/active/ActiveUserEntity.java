package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 参与抽奖用户;
 * @author : Jay
 * @date : 2023-1-10
 */
public class ActiveUserEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 配置id
     */
    public long config_id ;
    /**
     * 抽奖次数
     */
    public int play_count ;
    /**
     * 总次数
     */
    public int total_count ;
    /**
     * 分享uid
     */
    public long share_uid ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ActiveUserEntity getInstance() {
        return new ActiveUserEntity();
    }


    /**
     * 检查用户是否已经邀请用户
     * @param configId 配置id
     * @param uid 被邀请用户
     * @param inviteUid 邀请的用户
     * @return
     */
    public boolean checkInviteUser(long configId,long uid,long inviteUid){
        long count = this
               // .cache(String.format("LotteryWheel:InviteLog:%s_%s_%s",configId,uid,inviteUid),7200*1000)
                .where("uid",uid)
                .where("share_uid",inviteUid)
                .where("config_id",configId)
                .count("id");

        return count>0;

    }
}