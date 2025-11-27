package com.evcharge.entity.ad;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 广告类型;
 * @author : Jay
 * @date : 2023-2-24
 */
public class AdTypeEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 父id
     */
    public long parent_id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 创建时间
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static AdTypeEntity getInstance() {
        return new AdTypeEntity();
    }
}