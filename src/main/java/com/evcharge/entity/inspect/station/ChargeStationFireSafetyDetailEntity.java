package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;

@TargetDB("inspect")
public class ChargeStationFireSafetyDetailEntity extends BaseEntity implements Serializable {
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
    public String data_value ;
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

    /**
     * 根据safety_id 批量删除信息
     * @param safetyId
     * @return
     */
    public SyncResult delBySafetyId(long safetyId){
        long r= this.where("safety_id",safetyId).del();

        if(r==0){
            return new SyncResult(1,"批量删除失败");
        }
        return new SyncResult(0,"success,执行成功");

    }
}
