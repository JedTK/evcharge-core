package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * SPU商品品牌（基础表）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuBrandEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 品牌名
     */
    public String name;
    /**
     * 拼音
     */
    public String pinyin;
    /**
     * 品牌Logo
     */
    public String logo;
    /**
     * 品牌Icon
     */
    public String icon;
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
    public static zSpuBrandEntity getInstance() {
        return new zSpuBrandEntity();
    }

    /**
     * 根据品牌id查询品牌名
     *
     * @param brand_id
     * @return
     */
    public String getNameWithId(long brand_id) {
        Map<String, Object> data = this.field("id,name")
                .cache(String.format("BaseData:SpuBrand:%s:name", brand_id))
                .where("id", brand_id)
                .find();
        if (data.size() == 0) return "";
        return MapUtil.getString(data, "name");
    }
}
