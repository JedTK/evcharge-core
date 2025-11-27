package com.evcharge.utils;

import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class AbnormalChargeOrdersHandle {

    /**
     * 关于 2023-06-24 后新录入的设备功率没有得到限制的充电订单
     */
    public long AbnormalChargeOrders(long id) {
        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("id", ">", id)
//                .where("id", "=", id)
                .where("status", 2)
//                .where("limitChargePower", 0)
//                .where("maxPower", ">", 200)
                .where("create_time", ">=", 1687536000000L)
                .order("id")
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) {
            LogsUtil.info("无功率限制异常订单", "任务结束");
            return -1;
        }

        SyncResult r;
        if (orderEntity.paymentTypeId == 1) {
            //余额扣费
            r = BalanceHandle(orderEntity);
        } else {
            //充电卡扣费
            r = ChargeCardHandle(orderEntity);
        }
        if (r.code == 0) return orderEntity.id;
        return -1;
    }

    /**
     * 余额扣费充电订单处理
     */
    public SyncResult BalanceHandle(ChargeOrderEntity orderEntity) {
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(2, "订单数据为空");

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "找不到对应的设备数据");

        long chargeStandardConfigId = deviceEntity.chargeStandardConfigId;
        //订单的停止时间如果小于：2023-09-07 10:45:00 的话，证明收费标准配置是 4 不然则按照最新设备收费标准
        if (orderEntity.stopTime <= 1694054700000L) {
            chargeStandardConfigId = 2;
        }

        //应收金额
        double receivableAmount = 0.0;

        //region 计算真实扣费金额

        //region 根据充电功率来判断此次充电的单价：一般情况默认200w充电，但是用户使用了超级快充，则按用户设定的充电功率来计费
        //2023-6-1 更新日志：无论怎么样还是按照限制功率来进行计费，因为就算超出功率计费，应该是功率过大停止了充电，也应该按照用户选择的档位来计费
        double chargePower = orderEntity.limitChargePower;
        if (chargePower == 0) chargePower = orderEntity.maxPower;

        ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                .getItemWithConfig(chargeStandardConfigId, chargePower);
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            LogsUtil.error("", "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
            return new SyncResult(2, "系统错误：收费标准数据不正确");
        }
        //endregion

        //2023-06-06 更新日志：更新为以实际充电时间来进行扣费
        long chargeTime = Convert.toLong((orderEntity.stopTime - orderEntity.startTime) / 1000);

        // 2023-05-25 更新日志：计算粒度更新 充电收费 = 计算周期 * 计算周期单价
        long chargeTimeDivision = TimeUtil.division(chargeTime, chargeStandardItemEntity.billingInterval);
        receivableAmount = chargeTimeDivision * chargeStandardItemEntity.billingPrice;
        //安心充电费用
        receivableAmount += orderEntity.safeChargeFee;

        //2023-06-12 更新日志：检查扣费是否超过预设扣费,一般情况下是不可能超过预扣费的
        if (receivableAmount > orderEntity.estimateAmount) receivableAmount = orderEntity.estimateAmount;

        //region 针对计时结算充电，当充电启动时间和停止时间不超过设定分钟数不收钱
        int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 0);
        if (orderEntity.stopTime <= orderEntity.startTime + startChargeFreeTime * 1000) {
            receivableAmount = 0.0;
        }
        //endregion

        //实收金额
        double actuallyAmount = orderEntity.totalAmount;
        //差异金额
        double differenceAmount = receivableAmount - orderEntity.totalAmount;

        Map<String, Object> data = new LinkedHashMap<>();
        //实收金额
        data.put("actuallyAmount", actuallyAmount);
        //应收金额
        data.put("receivableAmount", receivableAmount);
        //差异金额
        data.put("differenceAmount", differenceAmount);

        orderEntity.update(orderEntity.id, data);

        LogsUtil.info("无功率限制异常订单", "[%s] 已经处理，应收金额：%s 实收金额：%s 差额：%s", orderEntity.OrderSN, receivableAmount, actuallyAmount, differenceAmount);

        return new SyncResult(0, "");
    }

    /**
     * 充电卡扣费充电订单处理
     */
    public SyncResult ChargeCardHandle(ChargeOrderEntity orderEntity) {
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(2, "订单数据为空");

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return new SyncResult(3, "找不到对应的设备数据");

        double receivableAmount = 0.0;

        //region 计算真实扣费金额

        //region 根据充电功率来判断此次充电的单价：一般情况默认200w充电，但是用户使用了超级快充，则按用户设定的充电功率来计费
        //2023-6-1 更新日志：无论怎么样还是按照限制功率来进行计费，因为就算超出功率计费，应该是功率过大停止了充电，也应该按照用户选择的档位来计费
        double chargePower = orderEntity.limitChargePower;
        if (chargePower == 0) chargePower = orderEntity.maxPower;

        long chargeStandardConfigId = deviceEntity.chargeStandardConfigId;
        //订单的停止时间如果小于：2023-09-07 10:45:00 的话，证明收费标准配置是 4 不然则按照最新设备收费标准
        if (orderEntity.stopTime <= 1694054700000L) {
            chargeStandardConfigId = 2;
        }

        ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                .getItemWithConfig(chargeStandardConfigId, chargePower);
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            LogsUtil.error("", "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
            return new SyncResult(2, "系统错误：收费标准数据不正确");
        }
        //endregion

        //2023-06-06 更新日志：更新为以实际充电时间来进行扣费
        long chargeTime = Convert.toLong(orderEntity.totalChargeTime * orderEntity.chargeCardConsumeTimeRate);

        // 2023-05-25 更新日志：计算粒度更新 充电收费 = 计算周期 * 计算周期单价
        long chargeTimeDivision = TimeUtil.division(chargeTime, chargeStandardItemEntity.billingInterval);
        receivableAmount = chargeTimeDivision * chargeStandardItemEntity.billingPrice;

        //实收金额
        double actuallyAmount = orderEntity.chargeCardConsumeAmount;
        //差异金额
        double differenceAmount = receivableAmount - orderEntity.chargeCardConsumeAmount;

        Map<String, Object> data = new LinkedHashMap<>();
        //实收金额
        data.put("actuallyAmount", actuallyAmount);
        //应收金额
        data.put("receivableAmount", receivableAmount);
        //差异金额
        data.put("differenceAmount", differenceAmount);

        orderEntity.update(orderEntity.id, data);

        LogsUtil.info("无功率限制异常订单", "[%s] 已经处理，应收金额：%s 实收金额：%s 差额：%s", orderEntity.OrderSN, receivableAmount, actuallyAmount, differenceAmount);

        return new SyncResult(0, "");
    }
}
