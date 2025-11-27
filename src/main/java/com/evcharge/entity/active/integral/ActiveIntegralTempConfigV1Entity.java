package com.evcharge.entity.active.integral;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 积分模版配置表;
 * @author : Jay
 * @date : 2023-11-30
 */
public class ActiveIntegralTempConfigV1Entity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 配置名
     */
    public String setting_name ;
    /**
     * 发放总量：0-不限制，1-限制
     */
    public int count_stint ;
    /**
     * 用户组领取限制：0-不限制，1-限制用户组
     */
    public int user_group_get_stint ;
    /**
     * 数量领取限制：0-不限制，1-指定领取数量
     */
    public int count_get_stint ;
    /**
     * 有效期限制：0-无期限，1-固定日期，2-领取积分当日开始N天内有效，3-领到积分次日开始N天内有效
     */
    public int expired_stint ;
    /**
     * 规则说明
     */
    public String desc ;
    /**
     * ip地址
     */
    public String ip ;
    /**
     * 状态
     */
    public int status ;
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
    public static ActiveIntegralTempConfigV1Entity getInstance() {
        return new ActiveIntegralTempConfigV1Entity();
    }

    /**
     * 通过id获取配置信息
     * @param id
     * @return
     */
    public ActiveIntegralTempConfigV1Entity getCouponConfigV1ById(long id){
        return this.cache(String.format("Active:Integral:ConfigV1Info:%s",id),86400*1000)
                .where("id",id)
                .findModel();
    }




}