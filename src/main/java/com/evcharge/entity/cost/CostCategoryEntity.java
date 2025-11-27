package com.evcharge.entity.cost;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 项目成本类别：如原材料、人工、运输等。;
 *
 * @author : JED
 * @date : 2025-1-7
 */
public class CostCategoryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 类别名称
     */
    public String category_name;
    /**
     * 类别编码
     */
    public String category_code;
    /**
     * 可回收：0-否，1-是
     */
    public int is_recyclable;
    /**
     * 父级编码
     */
    public String parent_code;
    /**
     * 类别描述
     */
    public String description;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static CostCategoryEntity getInstance() {
        return new CostCategoryEntity();
    }

    /**
     * 通过类别编码获取信息
     *
     * @param code 类别编码
     * @return
     */
    public CostCategoryEntity getByCode(String code) {
        return getByCode(code, true);
    }

    /**
     * 通过类别编码获取信息
     *
     * @param code    类别编码
     * @param inCache 是否优先从缓存中获取
     * @return
     */
    public CostCategoryEntity getByCode(String code, boolean inCache) {
        CostCategoryEntity entity = new CostCategoryEntity();
        if (inCache) entity.cache(String.format("BaseData:CostCategory:%s", code));
        return entity.where("category_code", code)
                .findEntity();
    }
}
