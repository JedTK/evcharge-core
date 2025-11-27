package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * SKU分类;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSkuTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 分类标题
     */
    public String title;
    /**
     * 描述
     */
    public String describe;
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
    public static zSkuTypeEntity getInstance() {
        return new zSkuTypeEntity();
    }

    /**
     * 根据品牌id查询品牌名
     *
     * @param sku_type_id
     * @return
     */
    public String getTitleWithId(long sku_type_id) {
        Map<String, Object> data = this.field("id,title")
                .cache(String.format("BaseData:SkuType:%s:name", sku_type_id))
                .where("id", sku_type_id)
                .find();
        if (data.size() == 0) return "";
        return MapUtil.getString(data, "title");
    }
}
