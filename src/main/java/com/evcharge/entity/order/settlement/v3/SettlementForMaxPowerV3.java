package com.evcharge.entity.order.settlement.v3;

import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.enumdata.EChargePaymentType;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;

/**
 * 峰值功率计费：总价 = 最高峰值功率 * 充电时长
 */
public class SettlementForMaxPowerV3 implements IChargingSettlementV3 {
    private final static String TAG = "按峰值功率结算v3";

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
    public SyncResult chargeFinish(ChargeOrderV3Entity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        LogsUtil.info(TAG, "[%s] - %s:%s", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        //根据扣费模式进行结算
        EChargePaymentType ePaymentType = EChargePaymentType.valueOf(orderEntity.paymentType);
        switch (ePaymentType) {
            case Balance://使用余额进行扣费
                return chargeFinishWithBalance(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
            case ChargeCard:
                return chargeFinishWithChargeCard(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
            case Integral:
                return chargeFinishWithIntegral(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
            default:
                return new SyncResult(11, "%s - 无效扣费方式", TAG);
        }
    }

    /**
     * 使用余额进行扣费
     * 计费方式：总价=电费(元/度) + 服务费(元/度)
     *
     * @param orderEntity    订单实体类
     * @param deviceEntity   设备实体类
     * @param stopTime       停止时间
     * @param stopReasonCode 停止原因代码
     * @param stopReasonText 停止原因文本
     * @return
     */
    private SyncResult chargeFinishWithBalance(ChargeOrderV3Entity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        LogsUtil.info(TAG, "[%s] - %s:%s 余额结算", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        //TODO 使用余额 按峰值功率进行结算

        return new SyncResult(11, "暂不支持充电卡扣费");
    }

    /**
     * 使用充电卡结算
     *
     * @param orderEntity    充电订单详情
     * @param deviceEntity   充电设备实体类
     * @param stopReasonCode 停止充电代码
     * @param stopReasonText 停止充电原因文本
     * @return
     */
    private SyncResult chargeFinishWithChargeCard(ChargeOrderV3Entity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        LogsUtil.info(TAG, "[%s] - %s:%s 充电卡结算", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        //TODO 使用充电卡 按峰值功率进行结算

        return new SyncResult(11, "暂不支持充电卡扣费");
    }

    /**
     * 使用积分结算
     *
     * @param orderEntity    订单实体类
     * @param deviceEntity   设备实体类
     * @param stopReasonCode 停止原因编码
     * @param stopReasonText 停止原因文本
     * @return
     */
    private SyncResult chargeFinishWithIntegral(ChargeOrderV3Entity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        LogsUtil.info(TAG, "[%s] - %s:%s 积分结算", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        //TODO 使用积分 按峰值功率进行结算

        return new SyncResult(11, "暂不支持充电卡扣费");
    }
}
