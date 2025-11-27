package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电站消防物料明细表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class ChargeStationFireSafetyDetailEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 充电站消防物料对照表ID
     */
    public long safety_id ;
    /**
     * 物料ID
     */
    public int material_id ;
    /**
     * 实际数量
     */
    public String quantity ;
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
    public static ChargeStationFireSafetyDetailEntity getInstance() {
        return new ChargeStationFireSafetyDetailEntity();
    }
}
