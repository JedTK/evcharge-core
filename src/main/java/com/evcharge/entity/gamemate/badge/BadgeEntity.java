package com.evcharge.entity.gamemate.badge;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 徽章表;null
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class BadgeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键ID,;
     */
    public long id;
    /**
     * 徽章名称,;
     */
    public String name;
    /**
     * 徽章类型（如 general, consume, charge）,;
     */
    public String type_code;
    /**
     * 徽章事件类型 关联BadgeEventsType,;
     */
    public String event_code;
    /**
     * 是否可重复获得 0=否 1=是 保留，暂时不可重复领取,;
     */
    public int is_repeatable;
    /**
     * 展示排序,;
     */
    public int sort;
    /**
     * 是否隐藏 0=否 1=是,;
     */
    public int is_hidden;
    /**
     * 徽章描述,;
     */
    public String description;
    /**
     * 徽章图标URL,;
     */
    public String image_url;
    /**
     * 稀有度 (如: Bronze, Silver, Gold),;
     */
    public String rarity;
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
    public static BadgeEntity getInstance() {
        return new BadgeEntity();
    }
}