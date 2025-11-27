package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 商品类型表;
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopGoodsTypeEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 父级id
     */
    public long pid ;
    /**
     * 描述
     */
    public String desc ;
    /**
     * 排序
     */
    public int sort ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;
    /**
     * icon
     */
    public String icon ;
    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ShopGoodsTypeEntity getInstance() {
        return new ShopGoodsTypeEntity();
    }
}