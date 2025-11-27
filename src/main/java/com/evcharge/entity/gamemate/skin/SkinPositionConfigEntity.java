package com.evcharge.entity.gamemate.skin;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 皮肤位置配置表;null
 *
 * @author : Jay
 * @date : 2025-10-22
 */
@TargetDB("evcharge_game_meta")
public class SkinPositionConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 所属页面,;
     */
    public String pages;
    /**
     * 位置代码,;
     */
    public String position_code;
    /**
     * 描述,;
     */
    public String description;
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
    public static SkinPositionConfigEntity getInstance() {
        return new SkinPositionConfigEntity();
    }
}