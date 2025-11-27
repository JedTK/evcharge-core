package com.evcharge.entity.basedata;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电时长配置;
 * @author : JED
 * @date : 2022-10-8
 */
public class ChargeTimeConfigEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 配置名
     */
    public String configName ;
    /**
     * 创建者ID
     */
    public long creator_id ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ChargeTimeConfigEntity getInstance() {
        return new ChargeTimeConfigEntity();
    }
}