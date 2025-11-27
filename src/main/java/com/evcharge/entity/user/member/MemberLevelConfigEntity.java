package com.evcharge.entity.user.member;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 会员等级配置表;会员等级配置表
 *
 * @author : Jay
 * @date : 2025-10-11
 */
public class MemberLevelConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 标题,;
     */
    public String title;
    /**
     * 描述,;
     */
    public String description;
    /**
     * 升级条件,;
     */
    public String conditions;
    /**
     * 权益,;
     */
    public String rights;
    /**
     * 排序,;
     */
    public int sort;
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
    public static MemberLevelConfigEntity getInstance() {
        return new MemberLevelConfigEntity();
    }
}