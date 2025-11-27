package com.evcharge.entity.station.qc;


import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 充电站消防物料对照表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class ChargeStationFireSafetyEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 充电站ID
     */
    public String cs_uuid ;
    /**
     * 使用的模板ID
     */
    public long template_id ;
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
    public static ChargeStationFireSafetyEntity getInstance() {
        return new ChargeStationFireSafetyEntity();
    }


    public ChargeStationFireSafetyEntity getWithUUID(String uuid){
        return getWithUUID(uuid, true);
    }

    public ChargeStationFireSafetyEntity getWithUUID(String uuid, boolean inCache) {
        if (!StringUtils.hasLength(uuid)) return null;
        this.where("uuid", uuid);
        if (inCache) this.cache(String.format("ChargeStation:FireSafety:uuid:%s", uuid));
        return this.findEntity();
    }

}