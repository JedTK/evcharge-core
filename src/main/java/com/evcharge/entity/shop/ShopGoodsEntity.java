package com.evcharge.entity.shop;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品表;
 *
 * @author : Jay
 * @date : 2022-12-16
 */
@TargetDB("evcharge_shop")
public class ShopGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 商品编号
     */
    public String numbering ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 副标题
     */
    public String subtitle ;
    /**
     * 品牌id
     */
    public long brand_id ;
    /**
     * 类型id
     */
    public long type_id ;
    /**
     * 发布状态，0下架，1上架 ，2添加未完成待恢复数据
     */
    public int status ;
    /**
     * 排序
     */
    public long sort ;
    /**
     * 详情
     */
    public String details ;
    /**
     * 广告图
     */
    public String ad_image ;
    /**
     * 主图
     */
    public String main_image ;
    /**
     * 描述
     */
    public String desc ;
    /**
     * 总库存
     */
    public int total_amount ;
    /**
     * 库存
     */
    public int stock ;
    /**
     * 原价
     */
    public BigDecimal original_price ;
    /**
     * 售价
     */
    public BigDecimal sale_price ;
    /**
     * 发货地址
     */
    public String ship_address ;
    /**
     * 商品规格重量，单位千克(kg)，用于发货计算
     */
    public BigDecimal weight ;
    /**
     * 是否拥有多规格 1=有 0=无
     */
    public int has_spec ;
    /**
     * 单位
     */
    public String unit ;
    /**
     * 商品长度，单位厘米(cm)，用于发货计算
     */
    public BigDecimal space_x ;
    /**
     * 商品宽度，单位厘米(cm)，用于发货计算
     */
    public BigDecimal space_y ;
    /**
     * 商品高度，单位厘米(cm)，用于发货计算
     */
    public BigDecimal space_z ;
    /**
     * 销量
     */
    public int sale_count ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;
    /**
     * 标签，用,隔开
     */
    public String tags ;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ShopGoodsEntity getInstance() {
        return new ShopGoodsEntity();
    }

    /**
     * 根据商品id获取商品详情
     * @param goodsId
     * @return
     */
    public ShopGoodsEntity getGoodsInfoById(long goodsId){
        return this.cache(String.format("Shop:GoodsInfo:%s",goodsId),86400*1000)
                .field("id,numbering,title,subtitle,brand_id,tags,total_amount,ship_address,type_id,status,details,main_image,desc,original_price,sale_price,unit,space_x,space_y,space_z,sale_count")
                .where("id",goodsId)
                .findModel();
    }

    /**
     * 删除商品缓存
     * @param goodsId
     */
    public void delCache(long goodsId){
        DataService.getMainCache().del(String.format("Shop:GoodsInfo:%s",goodsId));
    }

}