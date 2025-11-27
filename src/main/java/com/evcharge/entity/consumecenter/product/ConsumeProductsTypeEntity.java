package com.evcharge.entity.consumecenter.product;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 产品类型表;
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumeProductsTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * ;
     */
    public long id;
    /**
     * 类型代码,;
     */
    public String code;
    /**
     * 标题,;
     */
    public String title;
    /**
     * 属性 商品=product 服务=services,;
     */
    public String property;
    /**
     * 能力标签 NEED_SHIPPING ALLOW_INTEGRAL ALLOW_COUPON NEED_STATION,NEED_EBike,NOT_NEED_PAY
     */
    public String capability_tags;
    /**
     * 规则 比如是否检查库存，是否测试等,;
     */
    public String rule;
    /**
     * 是否显示活动页 1=显示 0=隐藏,;
     */
    public int sale_status;
    /**
     * 状态 1=上架 0=下架,;
     */
    public int status;
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
    public static ConsumeProductsTypeEntity getInstance() {
        return new ConsumeProductsTypeEntity();
    }
}