package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 规格值1-n（表示一个规格拥有对应的值）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpecValueEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 规格ID
     */
    public long spec_id;
    /**
     * 规格值
     */
    public String specValue;
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
    public static zSpecValueEntity getInstance() {
        return new zSpecValueEntity();
    }


    /**
     * 根据spec获取value值
     * @param specId
     * @return
     */
    public List<Map<String,Object>> getSpecValueBySpecID(long specId){

        if(specId==0) return new LinkedList<>();

        return this.where("spec_id",specId)
                .cache(String.format("Skuspu:Spec:ValueListCache%s",specId),86400*1000)
                .select();
    }



}
