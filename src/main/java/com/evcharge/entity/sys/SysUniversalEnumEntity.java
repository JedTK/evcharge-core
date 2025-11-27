package com.evcharge.entity.sys;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 通用枚举表;
 * @author : Jay
 * @date : 2024-9-18
 */
@TargetDB("evcharge_rbac")
public class SysUniversalEnumEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 父级id
     */
    public long parent_id ;
    /**
     * 标题
     */
    public String name ;
    /**
     * 代码
     */
    public String code ;
    /**
     * 值
     */
    public String enum_value ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static SysUniversalEnumEntity getInstance() {
        return new SysUniversalEnumEntity();
    }
}