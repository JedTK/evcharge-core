package com.evcharge.entity.spusku;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * SPU规格n-n（表示一个SPU有哪些关键规格）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuSpecEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * SPU ID
     */
    public long spu_id;
    /**
     * 规格ID
     */
    public long spec_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpuSpecEntity getInstance() {
        return new zSpuSpecEntity();
    }
}
