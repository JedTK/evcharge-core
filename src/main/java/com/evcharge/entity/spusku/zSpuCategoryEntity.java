package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * SPU类目表;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSpuCategoryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 层级
     */
    public String level;
    /**
     * 父级ID
     */
    public long parent_id;
    /**
     * 类名
     */
    public String name;
    /**
     * 拼音
     */
    public String pinyin;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 状态：0-不显示，1-显示
     */
    public int status;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSpuCategoryEntity getInstance() {
        return new zSpuCategoryEntity();
    }

    /**
     * 根据类目id查询类目名
     *
     * @param category_id
     * @return
     */
    public String getNameWithId(long category_id) {
        Map<String, Object> data = this.field("id,name")
                .cache(String.format("BaseData:SpuCategory:%s:name", category_id))
                .where("id", category_id)
                .find();
        if (data.size() == 0) return "";
        return MapUtil.getString(data, "name");
//        Map<String, Map<String, Object>> data = this.field("id,name")
//                .cache("BaseData:SpuCategory:Key")
//                .where("id", category_id)
//                .selectForKey("id");
//        if (data.size() == 0) return "";
//        Map<String, Object> v = data.get(category_id);
//        return MapUtil.getString(v, "name");
    }
}
