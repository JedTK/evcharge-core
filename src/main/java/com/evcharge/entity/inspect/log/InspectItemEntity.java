package com.evcharge.entity.inspect.log;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 巡检项目表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class InspectItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 所属模板ID
     */
    public long template_id;
    /**
     * 项目名称
     */
    public String item_name;
    /**
     * 项目描述
     */
    public String item_description;
    /**
     * 值类型 int,string,bool
     */
    public String value_type;
    /**
     * 可选项
     */
    public String options;
    /**
     * 是否必填，是=1，否=0
     */
    public int is_required;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static InspectItemEntity getInstance() {
        return new InspectItemEntity();
    }


    public List<InspectItemEntity> getItemByTempId(long templateId) {
        return this.where("template_id", templateId)
                .cache(String.format("Inspect:item:%s", templateId))
                .selectList();
    }


}