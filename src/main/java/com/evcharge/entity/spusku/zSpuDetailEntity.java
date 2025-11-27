package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 商品详情;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuDetailEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * spu ID
     */
    public long spu_id;
    /**
     * 详情
     */
    public String detail;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpuDetailEntity getInstance() {
        return new zSpuDetailEntity();
    }
}
