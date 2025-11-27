package com.evcharge.entity.order.settlement.v3;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.xyzs.entity.SyncResult;

/**
 * 充电订单V3版本结算接口
 */
public interface IChargingSettlementV3 {
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
    SyncResult chargeFinish(ChargeOrderV3Entity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText);
}
