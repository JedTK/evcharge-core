package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 巡检项目表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class InspectionItemEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 所属模板ID
     */
    public long template_id ;
    /**
     * 项目名称
     */
    public String item_name ;
    /**
     * 项目描述
     */
    public String item_description ;
    /**
     * 是否必填，是=1，否=0
     */
    public int is_required ;
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
    public static InspectionItemEntity getInstance() {
        return new InspectionItemEntity();
    }
}