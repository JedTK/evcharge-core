package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 商品订单详情;
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopOrderGoodsEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 订单id
     */
    public long order_id ;
    /**
     * 商品id
     */
    public long goods_id ;
    /**
     * 商品标题
     */
    public String goods_title ;
    /**
     * 商品主图
     */
    public String main_image ;
    /**
     * 商品价格
     */
    public double price ;
    /**
     * 购买数量
     */
    public int amount ;
    /**
     * 单位中文名
     */
    public String unit ;
    /**
     * 单个商品规格重量，单位千克(kg)，用于发货计算
     */
    public int weight ;
    /**
     * 单个商品长度，单位厘米(cm)，用于发货计算
     */
    public int space_x ;
    /**
     * 单个商品宽度，单位厘米(cm)，用于发货计算
     */
    public int space_y ;
    /**
     * 单个商品高度，单位厘米(cm)，用于发货计算
     */
    public int space_z ;
    /**
     * 状态：0=未发货，1=已发货，2=确认收货
     */
    public int status ;
    /**
     * 订单运费id
     */
    public long order_fare_id ;
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
    public static ShopOrderGoodsEntity getInstance() {
        return new ShopOrderGoodsEntity();
    }
}