package com.evcharge.entity.order;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电订单v3版本-扩展-充电柜;
 *
 * @author : JED
 * @date : 2024-3-12
 */
public class ChargeOrderV3ExCabinetEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电订单号
     */
    public String OrderSN;
    /**
     * 充电柜：门状态，0-关闭，1-打开，-1-无
     */
    public int cabinetDoorStatus;
    /**
     * 充电柜：开门时间戳，单位：毫米
     */
    public long cabinetOpenTime;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeOrderV3ExCabinetEntity getInstance() {
        return new ChargeOrderV3ExCabinetEntity();
    }
}
