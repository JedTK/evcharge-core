package com.evcharge.entity.gamemate.skin;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 用户皮肤表;null
 *
 * @author : Jay
 * @date : 2025-10-22
 */
@TargetDB("evcharge_game_meta")
public class UserSkinEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 皮肤ID,;
     */
    public long skin_id;
    /**
     * 皮肤来源（system, task, purchase, code）,;
     */
    public String source;
    /**
     * 订单编号,;
     */
    public String order_sn;
    /**
     * 开始时间,;
     */
    public long start_time;
    /**
     * 结束时间,;
     */
    public long end_time;
    /**
     * 是否默认 0=非默认 1=默认,;
     */
    public int is_default;
    /**
     * 状态 0=禁用 1=启用 -1=过期/失效,;
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
    public static UserSkinEntity getInstance() {
        return new UserSkinEntity();
    }
}