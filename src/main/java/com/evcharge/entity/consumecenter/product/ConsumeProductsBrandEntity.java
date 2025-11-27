package com.evcharge.entity.consumecenter.product;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 品牌表;
 *
 * @author : Jay
 * @date : 2025-10-15
 */
public class ConsumeProductsBrandEntity extends BaseEntity implements Serializable {
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
     * 状态 1=上架 0=下架,;
     */
    public byte status;
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
    public static ConsumeProductsBrandEntity getInstance() {
        return new ConsumeProductsBrandEntity();
    }
}