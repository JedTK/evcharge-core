package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 巡检模板表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class InspectionTemplateEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 模板名称
     */
    public String template_name ;
    /**
     * 巡检类型
     */
    public String inspection_type ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static InspectionTemplateEntity getInstance() {
        return new InspectionTemplateEntity();
    }
}