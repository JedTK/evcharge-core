package com.evcharge.entity.order.settlement.v3;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.enumdata.EBillingType;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;

/**
 * 新能源汽车充电订单结算
 */
public class SettlementForNEVCarV3 implements IChargingSettlementV3 {
    private final static String TAG = "新能源汽车充电结算v3";

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
    @Override
    public SyncResult chargeFinish(ChargeOrderV3Entity orderEntity, DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        LogsUtil.info(TAG, "[%s] - %s:%s", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        //根据计费方式进行收费
        EBillingType billingType = EBillingType.valueOf(orderEntity.billingType);
        switch (billingType) {
            case ChargeMaxPower:    // 按峰值功率结算
                return new SettlementForMaxPowerV3().chargeFinish(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
            case Electricity:       // 按电量结算
                return new SettlementForElectricityV20241112().chargeFinish(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
            default:
                return new SyncResult(11, "无效计费方式");
        }
    }
}
