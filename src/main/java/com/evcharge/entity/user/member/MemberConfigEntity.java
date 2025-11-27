package com.evcharge.entity.user.member;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员配置表;会员配置表
 *
 * @author : Jay
 * @date : 2025-10-11
 */
public class MemberConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 产品ID,;
     */
    public long product_id;
    /**
     * 标题,;
     */
    public String title;
    /**
     * 有效期(天),;
     */
    public int expire_day;
    /**
     * 价格,;
     */
    public BigDecimal price;
    /**
     * 状态（0=禁用，1=启用）,;
     */
    public int status;
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
    public static MemberConfigEntity getInstance() {
        return new MemberConfigEntity();
    }

    public MemberConfigEntity getInfoByProductId(long productId){
        return this
                .cache("BaseData:Member:ConfigInfo:ProductId:Cache" + productId, 86400 * 1000 * 7)
                .where("product_id", productId)
                .findEntity();
    }
}