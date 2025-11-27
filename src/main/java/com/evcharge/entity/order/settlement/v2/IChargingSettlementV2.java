package com.evcharge.entity.order.settlement.v2;

import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.xyzs.entity.SyncResult;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 充电订单V3版本结算接口
 */
public interface IChargingSettlementV2 {
    /**
     * 充电完成进行结算
     *
     * @param orderEntity    充电订单实体类
     * @param deviceEntity   设备实体类
     * @param stopTime       停止时间
     * @param stopReasonCode 停止原因代码
     * @param stopReasonText 停止原因文本
     * @return
     */
    SyncResult chargeFinish(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText);

    /**
     * 计费：总收费=电费+服务费
     *
     * @param orderEntity              充电订单数据
     * @param chargeStandardItemEntity 收费标准数据
     * @param chargeTime_second        充电时间（秒）
     * @return 计费结果列表
     */
    Map<String, BigDecimal> billing(@NonNull ChargeOrderEntity orderEntity
            , @NonNull ChargeStandardItemEntity chargeStandardItemEntity
            , double settlementPower
            , long chargeTime_second);

    /**
     * 预计收费：总收费=电费+服务费
     *
     * @param chargeStandardItemEntity 收费标准数据
     * @param chargeTime_second        充电时间（秒）
     * @return 计费结果列表
     */
    @NonNull
    BigDecimal estimateBillingAmount(@NonNull ChargeStandardItemEntity chargeStandardItemEntity
            , double settlementPower
            , long chargeTime_second);
}
