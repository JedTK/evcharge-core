package com.evcharge.entity.shop;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 优惠券配置表;
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopCouponRuleConfigV1Entity extends BaseEntity implements Serializable{
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
     * 优惠形式：1-优惠金额，2-优惠折扣，3-优惠随机金额，4-优惠随机折扣
     */
    public int rule_type ;
    /**
     * 发放总量：0-不限制，1-限制
     */
    public int count_stint ;
    /**
     * 金额使用限制：0-不限制，1-满多少元可用
     */
    public int amount_use_stint ;
    /**
     * 购买量使用限制：0-不限制，1-满多少件可用
     */
    public int count_use_stint ;
    /**
     * 指定商品使用限制：0-不限制，1-限制
     */
    public int goods_use_stint ;
    /**
     * 指定商品类目使用限制：0-不限制，1-限制
     */
    public int goods_type_use_stint ;
    /**
     * 用户组领取限制：0-不限制，1-限制用户组
     */
    public int user_group_get_stint ;
    /**
     * vip用户领取使用限制：0-不限制，1-限制
     */
    public int vip_stint ;
    /**
     * 数量领取限制：0-不限制，1-指定领取数量
     */
    public int count_get_stint ;
    /**
     * 有效期限制：0-无期限，1-固定日期，2-领取卷当日开始N天内有效，3-领到卷次日开始N天内有效
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
    public static ShopCouponRuleConfigV1Entity getInstance() {
        return new ShopCouponRuleConfigV1Entity();
    }


    /**
     * 通过配置id获取配置信息
     * @param configId
     * @return
     */
    public ShopCouponRuleConfigV1Entity getCouponConfigV1ById(long configId){

        return this.cache(String.format("Shop:Coupon:RuleConfigV1Info:%s",configId),86400*1000)
                .where("id",configId)
                .findModel();

    }







}