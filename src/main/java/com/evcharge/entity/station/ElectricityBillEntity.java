package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 电费账单;
 *
 * @author : JED
 * @date : 2022-12-27
 */
public class ElectricityBillEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户编号
     */
    public String userNo;
    /**
     * 结算户号
     */
    public String settleAccountNo;
    /**
     * 结算户名
     */
    public String settleAccountName;
    /**
     * 用电开始时间戳
     */
    public long startTime;
    /**
     * 用电结束时间戳
     */
    public long endTime;
    /**
     * 账单类型：1=电网账单，2=手抄账单
     */
    public int billTypeId;
    /**
     * 上次表示数
     */
    public double lastTimeMeterValue;
    /**
     * 本次表示数
     */
    public double thisTimeMeterValue;
    /**
     * 结算单价
     */
    public BigDecimal settlePrice;
    /**
     * 结算电量（千瓦时）
     */
    public BigDecimal settle_kwh;
    /**
     * 结算价格合计
     */
    public BigDecimal settleTotalAmount;
    /**
     * 平均电价
     */
    public BigDecimal avgPrice;
    /**
     * 账单合计电量（千瓦时）
     */
    public BigDecimal bill_kwh;
    /**
     * 系统合计电量，参考使用，根据充电订单统计
     */
    public BigDecimal system_kwh;
    /**
     * 应收电费合计
     */
    public BigDecimal billTotalAmount;
    /**
     * 供电方管理员ID
     */
    public long powerSupplierAdminId;
    /**
     * 状态：1=未支付，2=已支付，3=异常
     */
    public int status;
    /**
     * 充电站ID，辅助查询
     */
    public long CSId;
    /**
     * 电表编号
     */
    public String meterNo;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ElectricityBillEntity getInstance() {
        return new ElectricityBillEntity();
    }
}
