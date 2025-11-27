package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 转盘奖品列表;
 * @author : Jay
 * @date : 2023-1-10
 */
public class ActiveLotteryWheelPrizeEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 配置id
     */
    public long config_id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 中奖几率
     */
    public int percentage ;
    /**
     * 图标
     */
    public String icon ;
    /**
     * 奖品类型 1=优惠券 2=商品
     */
    public int type_id ;
    /**
     * 商品id
     */
    public long goods_id ;
    /**
     * 优惠券id
     */
    public long coupon_id ;
    /**
     * 数量
     */
    public int amount ;
    /**
     * 排序
     */
    public int sort ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ActiveLotteryWheelPrizeEntity getInstance() {
        return new ActiveLotteryWheelPrizeEntity();
    }

    /**
     * 通过id获取配置信息
     * @param prizeId
     * @return
     */
    public ActiveLotteryWheelPrizeEntity getPrizeById(long prizeId){
        return this.cache(String.format("LotteryWheel:Prize:info:%s",prizeId))
                .where("id",prizeId)
                .findModel();
    }


}