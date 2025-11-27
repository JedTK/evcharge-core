package com.evcharge.strategy.ChargeCard;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.enumdata.EChargeMode;
import com.evcharge.enumdata.EChargePaymentType;
import com.evcharge.utils.EvChargeHelper;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseChargeCardStrategy implements IChargeCardStrategy {
    private final static String TAG = "充电卡默认策略";

    /**
     * 执行 查询充电卡信息 预处理
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 充电前检查
     */
    @Override
    public ISyncResult executeQueryPreHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        // 查询充电桩
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(deviceEntity.deviceCode);
        if (chargeStationEntity == null) {
            return new SyncResult(3003, "无效充电卡");
        }

        // 检查此充电卡是否可以在此充电桩使用
        SyncResult cardCheckResult = cardEntity.checkChargeCardForStation(chargeStationEntity, cardEntity);
        if (cardCheckResult.code != 0) {
            return new SyncResult(3011, "充电卡无法在此设备使用");
        }
        return new SyncResult(0, "");
    }

    /**
     * 执行 查询充电卡信息
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 充电前检查
     */
    @Override
    public ISyncResult executeQueryHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ChargeMode", EChargeMode.MONTH);
        data.put("moneyBalance", 0); // 当充电卡为余额卡时使用
        data.put("expireTimestamp", cardEntity.end_time);
        return new SyncResult(0, "", data);
    }

    /**
     * 执行充电卡充电能力前的预处理
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 是否通过预处理
     */
    @Override
    public ISyncResult executeChargingPreHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        String deviceCode = deviceEntity.deviceCode;

        //查询充电桩
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(deviceCode);
        if (chargeStationEntity == null) {
            return new SyncResult(3003, "无效充电卡");
        }

        //region 检查此充电卡是否可以在此充电桩使用
        SyncResult cardCheckResult = cardEntity.checkChargeCardForStation(chargeStationEntity, cardEntity);
        if (cardCheckResult.code != 0) {
            LogsUtil.warn(TAG, "[%s - %s] - 充电卡无法在此设备使用：%s"
                    , cardEntity.uid
                    , cardEntity.cardNumber
                    , cardCheckResult.msg
            );
            return new SyncResult(3011, "充电卡无法在此设备使用");
        }
        //endregion

        //region 限制用户同时充电订单
        Map<String, Object> codata = ChargeOrderEntity.getInstance()
                .field("OrderSN")
                .where("uid", cardEntity.uid)
                .where("status", 1)
                .order("id DESC")
                .find();
        if (!codata.isEmpty()) {
            return new SyncResult(3013, "您目前有一笔订单正在充电中，请先完成充电再发起新的充电");
        }
        //endregion
        return new SyncResult(0, "");
    }

    /**
     * 处理卡的能力，一般为启动充电
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 是否成功执行能力
     */
    @Override
    public ISyncResult executeChargingHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        String deviceCode = deviceEntity.deviceCode;
        int port = JsonUtil.getInt(trans_data, "port");

        //启动充电
        EvChargeHelper helper = EvChargeHelper.getInstance()
                .setUid(cardEntity.uid)
                .setDeviceCode(deviceCode)
                .setPort(port)
                .setCardNumber(cardEntity.cardNumber)
                .setSafeCharge(false)
                .setPaymentType(EChargePaymentType.ChargeCard)
                .setLimitChargePower(1000)
                .setOverloadRestart(false);
        SyncResult chargeResult = helper.start();

        LogsUtil.info(TAG, "NFC刷卡 - 充电请求 - %s - %s", deviceCode, chargeResult.toJsonString());
        if (chargeResult.code != 0) return new SyncResult(3001, "启动充电失败");
        return new SyncResult(0, "");
    }

}
