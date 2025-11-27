package com.evcharge.entity.gamemate.badge;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 徽章获取条件表;null
 *
 * @author : Jay
 * @date : 2025-10-27
 */
public class BadgeConditionEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键ID,;
     */
    public long id;
    /**
     * 徽章ID 与Badge表一对一关系,;
     */
    public long badge_id;
    /**
     * 暂时不用，条件类型 (如: POST_COUNT, LOGIN_DAYS),;
     */
    public String condition_type;
    /**
     * 事件类型代码,;
     */
    public String event_type_code;
    /**
     * 暂时不用，条件组编号，用于同徽章多个条件分组,;
     */
    public String group_id;
    /**
     * 暂时不用，目标值,;
     */
    public int target_value;
    /**
     * 目标次数 比如累计多少次,;
     */
    public int target_count;
    /**
     * 目标费用 比如累计多少元,;
     */
    public BigDecimal target_fee;
    /**
     * 复杂规则的附加信息,;
     */
    public int rule_details;
    /**
     * 对 condition_type,target_count,target_fee md5，为了防止同一类型条件重复,;
     */
    public String md5;
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
    public static BadgeConditionEntity getInstance() {
        return new BadgeConditionEntity();
    }
}