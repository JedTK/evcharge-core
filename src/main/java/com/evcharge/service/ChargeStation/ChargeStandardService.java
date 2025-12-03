package com.evcharge.service.ChargeStation;

import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import lombok.NonNull;

import java.util.List;
import java.util.Map;


/**
 * 充电收费标准 - 业务逻辑
 */
public class ChargeStandardService {
    /**
     * 获得一个实例
     */
    public static ChargeStandardService getInstance() {
        return new ChargeStandardService();
    }

    /**
     * 获取收费标准列表 - 客户端使用
     *
     * @param deviceCode 设备编号
     * @param page       第几页
     * @param limit      每页显示多少条
     */
    public ISyncResult getListByDeviceCode(@NonNull String deviceCode, int page, int limit) {
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "设备还未上线，请稍后再试");

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(deviceEntity.CSId);
        if (chargeStationEntity == null || chargeStationEntity.id == 0)
            return new SyncResult(3, "站点还未上线，请稍后再试");

        if (page <= 0) page = 1;
        if (limit <= 0) limit = 50;

        long chargeStandardConfigId = chargeStationEntity.chargeStandardConfigId;
        if (chargeStandardConfigId == 0) {
            chargeStandardConfigId = deviceEntity.chargeStandardConfigId;
        }

        List<Map<String, Object>> list = ChargeStandardItemEntity.getInstance()
                .field("id,minPower,maxPower,electricityFeePrice,serviceFeePrice,price,chargeCardConsumeTimeRate,integralConsumeRate")
                .cache(String.format("ChargeStandardItem:%s:%s_%s", chargeStandardConfigId, page, limit))
                .where("configId", chargeStandardConfigId)
                .page(page, limit)
                .order("minPower")
                .select();
        return new SyncResult(0, "", list);
    }
}
