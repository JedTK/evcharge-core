package com.evcharge.entity.basedata;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收费标准配置项;
 *
 * @author : JED
 * @date : 2024-3-12
 */
public class ChargeStandardEnergyItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置ID
     */
    public long configId;
    /**
     * 时间段开始时间，24小时制
     */
    public String startTime;
    /**
     * 时间段结束时间，24小时制
     */
    public String endTime;
    /**
     * 电费价格，单位：元/度
     */
    public BigDecimal elecPrice ;
    /**
     * 服务费价格，单位：元/度
     */
    public BigDecimal servicePrice ;
    /**
     * 人民币对比积分比率
     */
    public BigDecimal integralConsumeRate ;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStandardEnergyItemEntity getInstance() {
        return new ChargeStandardEnergyItemEntity();
    }
}
