package com.evcharge.entity.recharge;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.*;

/**
 * 充值订单配置;
 *
 * @author : JED
 * @date : 2022-9-26
 */
public class RechargeConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 类型 1-普通充值 1-其他充值
     */
    public int type_id ;

    /**
     * 产品id，关联ConsumeProducts表s
     */
    public long product_id;

    /**
     * 赠送积分模版id
     */
    public long integral_temp_id ;
    /**
     * 积分扣减比例 10就是10%，充值20就可以减2
     */
    public int use_integral_rate ;
    /**
     * 标题
     */
    public String title;
    /**
     * 充值金额
     */
    public double price;
    /**
     * 到账余额
     */
    public double balance;
    /**
     * 奖励余额
     */
    public double reward_balance ;
    /**
     * 奖励标题，当reward_balance不能为空的时候填写
     */
    public String reward_title ;
    /**
     * 奖励标题，当reward_balance不能为空的时候填写
     */
    public String reward_sub_title ;
    /**
     * 状态 0=启用 1=禁用
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
    /**
     * 是否为测试订单，0=否，1=是
     */
    public int isTest;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static RechargeConfigEntity getInstance() {
        return new RechargeConfigEntity();
    }

    public Map<String, Object> getInfo(long configId) {

        return this
                .cache("RechargeConfigInfoCache" + configId, 86400 * 1000 * 7)
                .where("id", configId)
                .find();


    }

    public RechargeConfigEntity getInfoById(long configId) {

        return this
                .cache("BaseData:Recharge:ConfigInfo:Cache" + configId, 86400 * 1000 * 7)
                .where("id", configId)
                .findEntity();

    }

    public RechargeConfigEntity getInfoByProductId(long productId) {

        return this
                .cache("BaseData:Recharge:ConfigInfo:ProductId:Cache" + productId, 86400 * 1000 * 7)
                .where("product_id", productId)
                .findEntity();


    }
}
