package com.evcharge.entity.gamemate.badge;


import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 用户已获得徽章表;null
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class UserBadgeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键ID,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 徽章ID,;
     */
    public long badge_id;
    /**
     * 徽章获取时间,;
     */
    public long awarded_time;
    /**
     * 是否佩戴中 0=否 1=是,;
     */
    public int is_equipped;
    /**
     * 备注信息,;
     */
    public String memo;
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
    public static UserBadgeEntity getInstance() {
        return new UserBadgeEntity();
    }


    //添加徽章
    public JSONObject addBadge() {



        return new JSONObject();
    }



}