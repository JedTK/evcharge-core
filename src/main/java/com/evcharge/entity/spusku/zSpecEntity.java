package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 规格（基础表）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpecEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 规格名
     */
    public String specName;
    /**
     * 父级ID
     */
    public long parent_id;
    /**
     * 排序索引
     */
    public int sort_index;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpecEntity getInstance() {
        return new zSpecEntity();
    }


    /**
     * 根据spuid 获取spec信息
     * @param specId
     * @return
     */
    public zSpecEntity getInfoWithId (long specId){
        return this.where("id",specId)
                .cache(String.format("BaseData:Spec:%s",specId))
                .findModel();
    }

}
