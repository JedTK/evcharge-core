package com.evcharge.entity.gamemate.skin;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 皮肤位置表;null
 *
 * @author : Jay
 * @date : 2025-10-22
 */
@TargetDB("evcharge_game_meta")
public class SkinPositionEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 皮肤ID,;
     */
    public long skin_id;
    /**
     * 所属页面,;
     */
    public String pages;
    /**
     * 位置代码,;
     */
    public String position_code;
    /**
     * 图片,;
     */
    public String image_url;
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
    public static SkinPositionEntity getInstance() {
        return new SkinPositionEntity();
    }
}
