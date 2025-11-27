package com.evcharge.entity.inspect.log;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 巡检模板表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("inspect")
public class InspectTemplateEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 模板名称
     */
    public String template_name;
    /**
     * 站点属性
     */
    public int station_attr;
    /**
     * 巡检周期 周/月/季度/年 等
     */
    public String inspect_cycle ;
    /**
     * 巡检类型 1=一般巡检 0=紧急巡检
     */
    public String inspection_type;
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
    public static InspectTemplateEntity getInstance() {
        return new InspectTemplateEntity();
    }


    public InspectTemplateEntity getTempByAttr(int attr, int type) {
        return this.where("station_attr", attr)
                .where("inspection_type", type)
                .cache(String.format("Inspect:Template:%s:%s",attr,type))
                .findEntity();
    }


}