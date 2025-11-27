package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 店铺基础信息（预留表，字段可扩充）;
 * @author : JED
 * @date : 2023-1-6
 */
public class zShopBaseEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 店铺名称
     */
    public String name ;
    /**
     * 拼音
     */
    public String pinyin ;
    /**
     * 店铺Logo
     */
    public String logo ;
    /**
     * 拥有者ID
     */
    public String owner_id ;
    /**
     * 组织ID
     */
    public long organize_id ;
    /**
     * 创建时间戳
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static zShopBaseEntity getInstance() {
        return new zShopBaseEntity();
    }
}
