package com.evcharge.entity.station;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩分润 - 合同 n - n
 *
 * @author : JED
 * @date : 2022-12-21
 */
public class ChargeStationShareProfitContractEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 充电桩分润ID
     */
    public long CSSPId;
    /**
     * 合同Id
     */
    public long contractId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationShareProfitContractEntity getInstance() {
        return new ChargeStationShareProfitContractEntity();
    }
}
