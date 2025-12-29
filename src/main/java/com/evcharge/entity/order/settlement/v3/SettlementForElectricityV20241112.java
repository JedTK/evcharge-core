package com.evcharge.entity.order.settlement.v3;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.basedata.ChargeStandardEnergyItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.order.ChargeOrderV3Entity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserBalanceLogEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.enumdata.EChargePaymentType;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电量计费：总价 = 电费(元/度) + 服务费(元/度)
 */
public class SettlementForElectricityV20241112 implements IChargingSettlementV3 {

    private final static String TAG = "按电量结算v3";

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

        SyncResult r;
        //根据扣费模式进行结算
        EChargePaymentType ePaymentType = EChargePaymentType.valueOf(orderEntity.paymentType);
        switch (ePaymentType) {
            case Balance://使用余额进行扣费
                r = chargeFinishWithBalance(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
                break;
            case ChargeCard:
                return new SyncResult(11, "电量计费 - 暂不支持充电卡扣费");
            case Integral:
                return new SyncResult(11, "电量计费 - 暂不支持积分扣费");
            default:
                return new SyncResult(11, "电量计费 - 无效扣费方式");
        }
        if (r.code != 0) {
            LogsUtil.info(TAG, "[%s] - %s:%s 结算失败：%s", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port, r.msg);
        }
        return r;
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

        return orderEntity.beginTransaction(connection -> {
            try {
                int paymentStatus = 0;//支付状态：0=未支付，1=已支付
                int status = 2;//状态,-1=错误，0=待启动，1=充电中，2=已完成
                String status_msg = "";

                //电费金额 = 电量 * 电费电价
                BigDecimal electricityFeeAmount = orderEntity.electricityFeeAmount;
                //服务费金额 = 电量 * 服务费单价
                BigDecimal serviceFeeAmount = orderEntity.serviceFeeAmount;
                //安全充电保险费用
//                BigDecimal safeChargeFee = orderEntity.safeCharge == 1 ?
//                        new BigDecimal(orderEntity.safeChargeFee).setScale(6, RoundingMode.HALF_UP)
//                        : BigDecimal.ZERO;

                //停车费 = 未知
                BigDecimal parkingFeeAmount = new BigDecimal(0);

                //优惠金额
                BigDecimal discountAmount = orderEntity.discountAmount;
                if (discountAmount == null) discountAmount = new BigDecimal(0);

                //应扣费金额 = (电费+服务费+安全充电保险费+停车费)
                BigDecimal receivableAmount = new BigDecimal(0);
                receivableAmount = receivableAmount
                        .add(electricityFeeAmount)
                        .add(serviceFeeAmount)
//                        .add(safeChargeFee)
                        .add(parkingFeeAmount)
                        .setScale(6, RoundingMode.HALF_UP);
                //实际扣费金额 = 应扣费金额 - 优惠金额
                BigDecimal totalAmount = receivableAmount
                        .subtract(discountAmount)
                        .setScale(6, RoundingMode.HALF_UP);

                //查询收费价格

                //region 充电预扣费退款
                if (orderEntity.estimateAmount != null && orderEntity.estimateAmount.compareTo(BigDecimal.ZERO) != 0) {
                    SyncResult r = orderEntity.esChargeRefundTransaction(connection
                            , orderEntity.uid
                            , orderEntity.estimateAmount
                            , orderEntity.OrderSN);
                    //记录一下日志
                    if (r.code != 0) {
                        LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：返回用户充电预扣费失败 OrderSN=%s DeviceCode=%s msg=%s", orderEntity.OrderSN, deviceEntity.deviceCode, r.msg);
                    }
                }
                //endregion

                UserSummaryEntity userSummaryEntity = UserSummaryEntity.getInstance();

                //region 进行扣费操作

                if (orderEntity.paymentStatus == 1) {
                    LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：此笔充电订单用户已经支付过了，发生重复结算情况 OrderSN=%s DeviceCode=%s ORDER=%s", orderEntity.OrderSN, deviceEntity.deviceCode, JSONObject.toJSONString(orderEntity));
                    return new SyncResult(11, "重复支付");
                }

                //如果扣费金额为0可能是免结算时间内停止充电
                if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
                    int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
                    status = -1;
                    status_msg = String.format("在%s秒内进行结算无需扣款", startChargeFreeTime);
                } else {

                    //检查用户是否足够余额
                    BigDecimal balance = userSummaryEntity.getBalanceWithUid(orderEntity.uid).setScale(6, RoundingMode.HALF_UP);
                    if (balance.compareTo(totalAmount) < 0) {
                        // 余额小于总金额
                        //未支付
                        LogsUtil.error(this.getClass().getSimpleName(), "[%s][%s] - %s 充电结算失败：用户余额不足，当前余额 %s", orderEntity.OrderSN, deviceEntity.deviceCode, orderEntity.uid, balance);
                    }

                    // 检查是否已经扣费了
                    if (!UserBalanceLogEntity.getInstance()
                            .where("type", EBalanceUpdateType.charge)
                            .where("orderSN", orderEntity.OrderSN)
                            .existTransaction(connection)) {
                        // 不管余额是否足够扣除，直接扣费
                        SyncResult payResult = userSummaryEntity.updateBalanceTransaction(connection, orderEntity.uid
                                , totalAmount.negate() //这里取负数值进行计算
                                , EBalanceUpdateType.charge
                                , "充电扣费"
                                , orderEntity.OrderSN);
                        if (payResult.code != 0) {
                            LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：用户扣款失败 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                        } else paymentStatus = 1;//已支付
                    } else paymentStatus = 1;//已支付
                }
                //endregion

                Map<String, Object> set_data = new LinkedHashMap<>();
                set_data.put("status", status);//状态,-1=错误，0=待启动，1=充电中，2=已完成
                set_data.put("status_msg", status_msg);
                set_data.put("paymentStatus", paymentStatus);//支付状态：0=未支付，1=已支付

                set_data.put("totalAmount", totalAmount);//实际扣费金额
                set_data.put("electricityFeeAmount", electricityFeeAmount);//电费
                set_data.put("serviceFeeAmount", serviceFeeAmount);//服务费
                set_data.put("parkingFeeAmount", parkingFeeAmount);//停车费
                set_data.put("receivableAmount", receivableAmount);//应扣费金额

                set_data.put("stopTime", stopTime);
                set_data.put("stopReasonCode", stopReasonCode);
                set_data.put("stopReasonText", stopReasonText);
                if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                    LogsUtil.error(TAG, "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                    return new SyncResult(1, "结算失败");
                }
                return new SyncResult(0, "", set_data);
            } catch (Exception e) {
                LogsUtil.error(e, TAG, "充电结算错误：OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
            }
            return new SyncResult(1, "结算失败");
        });
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

        //TODO 使用充电卡 按电量进行结算

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

        //TODO 使用积分 按电量进行结算

        return new SyncResult(11, "暂不支持充电卡扣费");
    }

    /**
     * 判断时间戳是否在对应的时间段上(兼容跨越午夜的时间区间)
     *
     * @param timeStamp 时间戳
     * @param start     开始时间段
     * @param end       结束时间段
     * @return 是/否
     */
    public static boolean isTimeWithinInterval(long timeStamp, String start, String end) {
        // 使用 Calendar 实例来处理和转换时间戳
        Calendar calendar = Calendar.getInstance();
        // 将传入的时间戳（单位为毫秒）设置为 Calendar 的当前时间
        calendar.setTimeInMillis(timeStamp);

        // 从 Calendar 实例中提取当前时间的小时和分钟
        int hours = calendar.get(Calendar.HOUR_OF_DAY); // 24小时制
        int minutes = calendar.get(Calendar.MINUTE);

        // 使用辅助方法转换时间，减少重复代码
        int currentTimeInMinutes = convertToMinutes(hours, minutes);
        int startTimeInMinutes = convertToMinutes(start);
        int endTimeInMinutes = convertToMinutes(end);

        // 如果结束时间小于开始时间，说明时间区间跨越了午夜
        if (endTimeInMinutes <= startTimeInMinutes) {
            // 如果当前时间小于结束时间，说明当前时间在午夜后
            // 如果当前时间大于等于开始时间，说明当前时间在午夜前
            return currentTimeInMinutes < endTimeInMinutes || currentTimeInMinutes >= startTimeInMinutes;
        } else {
            // 时间区间不跨越午夜，直接比较
            return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes;
        }
    }

    /**
     * 辅助方法：将时间字符串转换为分钟
     *
     * @param time 时间(00:00)
     * @return 分钟数
     */
    private static int convertToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /**
     * 辅助方法：将小时和分钟转换为分钟
     *
     * @param hours   小时
     * @param minutes 分钟
     * @return 分钟数
     */
    private static int convertToMinutes(int hours, int minutes) {
        return hours * 60 + minutes;
    }
}

