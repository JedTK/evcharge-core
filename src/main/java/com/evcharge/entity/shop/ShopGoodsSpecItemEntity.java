package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 规格明细表;
 * @author : Jay
 * @date : 2022-12-20
 */
@TargetDB("evcharge_shop")
public class ShopGoodsSpecItemEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 规格id
     */
    public long spec_id ;
    /**
     * 具体sku名称(黄色/20g)
     */
    public String item_title ;
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
    public static ShopGoodsSpecItemEntity getInstance() {
        return new ShopGoodsSpecItemEntity();
    }
}