package com.evcharge.entity.gamemate.badge;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 徽章事件表;
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class BadgeEventsTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id,;
     */
    public long id;
    /**
     * 父级id,;
     */
    public long parent_id;
    /**
     * 名称,;
     */
    public String title;
    /**
     * 代码,;
     */
    public String code;
    /**
     * 描述,;
     */
    public String desc;
    /**
     * 是否重复 0=可重复 1=不可重复,;
     */
    public int is_repeat;
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
    public static BadgeEventsTypeEntity getInstance() {
        return new BadgeEventsTypeEntity();
    }
}