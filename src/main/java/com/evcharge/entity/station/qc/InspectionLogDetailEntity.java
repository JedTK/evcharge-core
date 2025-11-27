package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 巡检日志明细表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class InspectionLogDetailEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 关联的日志ID
     */
    public long log_id ;
    /**
     * 关联的巡检项目ID
     */
    public long item_id ;
    /**
     * 巡检项目名称
     */
    public String item_name ;
    /**
     * 状态
     */
    public int status ;
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
    public static InspectionLogDetailEntity getInstance() {
        return new InspectionLogDetailEntity();
    }
}