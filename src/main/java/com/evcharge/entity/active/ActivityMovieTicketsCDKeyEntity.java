package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 活动电影票;
 * @author : JED
 * @date : 2023-6-13
 */
public class ActivityMovieTicketsCDKeyEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 类型 1=cdkey 0=无cdkey
     */
    public int type;

    /**
     * 电影id
     */
    public long movie_id ;
    /**
     * cdkey
     */
    public String cd_key ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 充值订单编号
     */
    public String recharge_ordersn ;
    /**
     * 状态 0-未领取 1-已领取
     */
    public int status ;
    /**
     * 到期时间
     */
    public String expire_date ;
    /**
     * 领取时间
     */
    public long pickup_time ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ActivityMovieTicketsCDKeyEntity getInstance() {
        return new ActivityMovieTicketsCDKeyEntity();
    }



    /**
     * 获取未使用的cdkey
     * @return
     */
    public ActivityMovieTicketsCDKeyEntity getUnUseKey(long movieId){

        return this
                .where("status",0)
                .where("movie_id",movieId)
                .order("id asc")
                .findModel();

    }


    /**
     * 获取未使用的cdkey
     * @return
     */
//    public ActivityMovieTicketsCDKeyEntity getUnUseKey(long movieId){
//
//        return this.where("status",0)
//                .where("movie_id",movieId)
//                .where("type",1)
//                .order("id asc")
//                .findModel();
//
//    }
    /**
     * 获取未使用的cdkey
     * @return
     */
    public ActivityMovieTicketsCDKeyEntity getUnUseKeyNoTicket(long movieId){

        return this.where("status",0)
                .where("movie_id",movieId)
                .where("type",0)
                .order("id asc")
                .findModel();

    }

    /**
     * 准备弃用
     * 获取用户cdkey
     * @param uid
     * @return
     */
    public ActivityMovieTicketsCDKeyEntity getUserKey(long uid){
        return this.where("status",1)
                .where("uid",uid)
                .where("type",1)
                .where("status",1)
                .order("id asc")
                .findModel();
    }

    /**
     *
     * @param uid
     * @param movieId
     * @return
     */
    public ActivityMovieTicketsCDKeyEntity getUserKeyByMovieId(long uid,long movieId){
        return this
                .where("uid",uid)
                .where("movie_id",movieId)
                .whereIn("type","1,2,3")
                .whereIn("status","1,2")
                .order("id asc")
                .findModel();
    }

}