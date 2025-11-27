package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 购物车列表;
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopCartEntity extends BaseEntity implements Serializable{
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
     * 商品id
     */
    public long goods_id ;
    /**
     * 数量
     */
    public long amount ;
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
    public static ShopCartEntity getInstance() {
        return new ShopCartEntity();
    }
}