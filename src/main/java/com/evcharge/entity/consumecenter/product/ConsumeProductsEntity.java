package com.evcharge.entity.consumecenter.product;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 核心产品表;null
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeProductsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 产品ID,;
     */
    public long id;
    /**
     * 产品名称,;
     */
    public String title;
    /**
     * 副标题,;
     */
    public String subtitle;
    /**
     * 产品类型,;
     */
    public String type_code;
    /**
     * 品牌id,;
     */
    public long brand_id;
    /**
     * 轮播图,;
     */
    public String banner;
    /**
     * 销售价格,;
     */
    public BigDecimal price;
    /**
     * 标签,;
     */
    public String tags;
    /**
     * 开始时间,;
     */
    public long start_time;
    /**
     * 截止时间,;
     */
    public long end_time;
    /**
     * 主图,;
     */
    public String main_image;
    /**
     * 产品描述,;
     */
    public String description;
    /**
     * 总数,;
     */
    public int total_amount;
    /**
     * 库存,;
     */
    public int stock;
    /**
     * 跳转地址,;
     */
    public String link;
    /**
     * 跳转类型 webview / mp,;
     */
    public String link_type;
    /**
     * 产品状态 1=上架 0=待上架 -1=已下架,;
     */
    public int status;
    /**
     * 是否可以退款 0=不可以退款 1=可以退款,;
     */
    public int can_refund;
    /**
     * 是否在活动页显示 0=不显示 1=显示,;
     */
    public int can_sale;
    /**
     * 是否在支付宝系活动页显示 0=不显示 1=显示,;
     */
    public int can_sale_alipay;
    /**
     * 排序,;
     */
    public int sort;
    /**
     * 备注,;
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
    public static ConsumeProductsEntity getInstance() {
        return new ConsumeProductsEntity();
    }
}