package com.evcharge.entity.gamemate.skin;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 皮肤表;null
 *
 * @author : Jay
 * @date : 2025-10-22
 */
@TargetDB("evcharge_game_meta")
public class SkinEntity extends BaseEntity implements Serializable {
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
     * 卡id，关联ChargeCardConfig表,;
     */
    public long card_id;

    /**
     * 类型 system=系统默认 active=活动,;
     */
    public String type_code;
    /**
     * 皮肤编码,;
     */
    public String code;
    /**
     * 获取方式,;
     */
    public String obtain_method;
    /**
     * 稀有度,;
     */
    public String rarity;
    /**
     * 标题,;
     */
    public String name;
    /**
     * 价格,;
     */
    public BigDecimal price;
    /**
     * 描述,;
     */
    public String description;
    /**
     * 主图,;
     */
    public String main_image;
    /**
     * 皮肤有效期（天），0表示永久有效,;
     */
    public int duration_days;
    /**
     * 皮肤标签（逗号分隔，如：限时,节日）,;
     */
    public String tags;
    /**
     * 权益,;
     */
    public String rights;
    /**
     * 排序,;
     */
    public int sort;
    /**
     * 状态 0=禁用 1=启用,;
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
    public static SkinEntity getInstance() {
        return new SkinEntity();
    }
}
