package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 巡检日志表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class InspectionLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 巡检人员id
     */
    public long admin_id ;
    /**
     * 充电站ID
     */
    public String cs_uuid ;
    /**
     * 使用的模板ID
     */
    public long template_id ;
    /**
     * 巡检日期
     */
    public long inspection_date ;
    /**
     * 总体状态
     */
    public int overall_status ;
    /**
     * 备注
     */
    public String remarks ;
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
    public static InspectionLogEntity getInstance() {
        return new InspectionLogEntity();
    }
}