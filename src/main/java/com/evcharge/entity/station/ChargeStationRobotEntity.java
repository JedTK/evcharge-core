package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩机器人;
 *
 * @author : JED
 * @date : 2022-11-30
 */
public class ChargeStationRobotEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public long CSId;
    /**
     * 用户ID
     */
    public long uid;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationRobotEntity getInstance() {
        return new ChargeStationRobotEntity();
    }
}
