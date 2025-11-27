package com.evcharge.entity.order.settlement.v2;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserBalanceLogEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.enumdata.EChargePaymentType;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电费用结算类 2024-10-21 版
 * <p>
 * 该类实现了电动自行车充电的结算逻辑，包含了根据不同支付方式（余额、充电卡、积分）进行结算的具体实现。
 * 其中包括电费和服务费的计算规则、退款逻辑以及在特定情况下的费用免除处理。
 */
public class ChargeSettlementForEBike_20241021 implements IChargingSettlementV2 {
    private final static String TAG = "电自充电结算20241021版";

    /**
     * 充电完成进行结算
     * <p>
     * 结算逻辑包括：
     * 1. 根据扣费模式（余额、充电卡、积分）进行不同的结算操作。
     * 2. 记录结算日志，包括订单号、停止原因等信息。
     *
     * @param orderEntity     充电订单实体类，包含订单的详细信息。
     * @param deviceEntity    设备实体类，包含充电设备的详细信息。
     * @param settlementPower 结算时使用的功率值，用于计算费用。
     * @param stopTime        充电停止时间，以时间戳形式表示。
     * @param stopReasonCode  停止原因的代码，用于标识停止的具体原因。
     * @param stopReasonText  停止原因的文本描述，便于理解和记录。
     * @return 返回一个SyncResult对象，表示结算的结果状态和相关信息。
     */
    @Override
    public SyncResult chargeFinish(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {

        // 构建结算日志信息，包含订单号、停止原因代码和文本。
        LogsUtil.info(TAG, "\033[1;94m 充电结算：OrderSN=%s stopReasonCode=%s stopReasonText=%s \033[0m"
                , orderEntity.OrderSN
                , stopReasonCode
                , stopReasonText
        );

        // 根据订单中的支付类型决定使用哪种结算方式
        EChargePaymentType ePaymentType = EChargePaymentType.valueOf(orderEntity.paymentTypeId);
        switch (ePaymentType) {
            case Balance: // 使用余额进行扣费
                return chargeFinishWithBalance(orderEntity, deviceEntity, settlementPower, stopTime, stopReasonCode, stopReasonText);
            case ChargeCard: // 使用充电卡进行扣费
                return chargeFinishWithChargeCard(orderEntity, deviceEntity, settlementPower, stopTime, stopReasonCode, stopReasonText);
            case Integral: // 使用积分进行扣费
                return chargeFinishWithIntegral(orderEntity, deviceEntity, settlementPower, stopTime, stopReasonCode, stopReasonText);
            default:
                // 如果支付类型无效，则返回错误结果
                return new SyncResult(11, "%s - 无效扣费方式", TAG);
        }
    }

    /**
     * billing - 计费方法
     * <p>
     * 此方法基于传入的功率和充电时间计算充电费用和服务费用。
     * - 计算电费和服务费，并汇总成总费用。
     * - 使用不同的计费规则：电费按功率和时间计算，服务费仅按时间计算。
     *
     * @param orderEntity              - 充电订单数据，包含订单相关信息。
     * @param chargeStandardItemEntity - 收费标准数据，包含电费和服务费的单价。
     * @param settlementPower          - 结算功率值，用于电费计算。
     * @param chargeTime_second        - 充电时间（秒）。
     * @return Map<String, BigDecimal> - 返回Map，包含电费、服务费、总费用等详细信息。
     */
    @NonNull
    public Map<String, BigDecimal> billing(@NonNull ChargeOrderEntity orderEntity
            , @NonNull ChargeStandardItemEntity chargeStandardItemEntity
            , double settlementPower
            , long chargeTime_second) {

        BigDecimal totalAmount = BigDecimal.ZERO; // 总费用（电费+服务费）
        BigDecimal electricityFeeAmount = BigDecimal.ZERO; // 电费
        BigDecimal serviceFeeAmount = BigDecimal.ZERO; // 服务费

        // 获取收费标准中的电费单价（元/度）和服务费单价（元/小时）
        BigDecimal electricityFeePrice = chargeStandardItemEntity.electricityFeePrice;
        BigDecimal serviceFeePrice = chargeStandardItemEntity.serviceFeePrice;

        // 衍生耗电量基于峰值功率计算
        BigDecimal derivedPowerConsumption = BigDecimal.ZERO;
        // 充电小时数（将秒转换为小时）
        BigDecimal chargingHour = BigDecimal.ZERO;

        try {
            // 检查是否符合免费充电时间（如充电时间小于10分钟，免收费用）
            int freeChargeTimeLimit = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
            if (chargeTime_second > 0 && chargeTime_second > freeChargeTimeLimit) {
                // 将秒转换为小时，保留6位小数以提高精度
                chargingHour = BigDecimal.valueOf(chargeTime_second)
                        .divide(new BigDecimal(3600), 6, RoundingMode.HALF_UP);
            }

            // 检查是否超过免费充电时间，且有效充电时间大于0
            if (chargingHour.compareTo(BigDecimal.ZERO) > 0) {
                // 计算电费（基于峰值功率估算）
                if (settlementPower > 0) {
                    // 使用峰值功率估算耗电量，得出衍生耗电量
                    derivedPowerConsumption = BigDecimal.valueOf(settlementPower)
                            .divide(new BigDecimal(1000), 6, RoundingMode.HALF_UP)
                            .multiply(chargingHour);

                    // 如果未估算出有效耗电量，但设备报告了实际值，则使用设备报告的耗电量
                    if (derivedPowerConsumption.compareTo(BigDecimal.ZERO) == 0 && orderEntity.powerConsumption > 0) {
                        derivedPowerConsumption = BigDecimal.valueOf(orderEntity.powerConsumption);
                    }
                    // 计算电费金额
                    electricityFeeAmount = derivedPowerConsumption.multiply(electricityFeePrice);
                }

                // 计算服务费：服务费单价 * 充电小时数
                serviceFeeAmount = serviceFeePrice.multiply(chargingHour);
            }

            // 计算总费用：电费 + 服务费
            totalAmount = electricityFeeAmount.add(serviceFeeAmount);
        } catch (Exception e) {
            // 记录计算过程中的异常，便于调试
            LogsUtil.error(e, TAG, "");
        }

        // 构建结果Map
        Map<String, BigDecimal> data = new LinkedHashMap<>();
        data.put("totalAmount", totalAmount); // 总费用
        data.put("electricityFeeAmount", electricityFeeAmount); // 电费金额
        data.put("serviceFeeAmount", serviceFeeAmount); // 服务费金额
        data.put("electricityFeePrice", electricityFeePrice); // 电费单价
        data.put("serviceFeePrice", serviceFeePrice); // 服务费单价
        data.put("derivedPowerConsumption", derivedPowerConsumption); // 估算的耗电量

        // 输出详细的计费信息日志
        LogsUtil.info(TAG, "%s - %s", JSONObject.from(orderEntity).toJSONString(), new JSONObject(data).toJSONString());
        return data;
    }

    /**
     * estimateBillingAmount - 预计收费方法
     * <p>
     * 该方法用于估算充电的总费用，包括电费和服务费。
     * 基于充电时间（秒）和结算功率（瓦）进行计算，
     * 其中电费是基于功率估算得出的耗电量计算，
     * 服务费则按充电时长收费。
     * <p>
     * 计费规则：
     * - 总费用 = 电费 + 服务费
     * - 电费 = (结算功率 / 1000W) * 电费单价（元/度） * 充电小时数
     * - 服务费 = 服务费单价（元/小时） * 充电小时数
     *
     * @param chargeStandardItemEntity - 收费标准数据，包含电费单价和服务费单价
     * @param settlementPower          - 结算使用的功率值（瓦），用于电费计算
     * @param chargeTime_second        - 充电时间，以秒为单位
     * @return BigDecimal - 返回预计的总费用（元），包含电费和服务费的总和
     */
    @NonNull
    public BigDecimal estimateBillingAmount(@NonNull ChargeStandardItemEntity chargeStandardItemEntity
            , double settlementPower
            , long chargeTime_second) {

        // 总费用（电费 + 服务费）初始值设为0
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal electricityFeeAmount = BigDecimal.ZERO; // 电费金额
        BigDecimal serviceFeeAmount = BigDecimal.ZERO; // 服务费金额

        // 从收费标准数据获取电费单价（元/度）和服务费单价（元/小时）
        BigDecimal electricityFeePrice = chargeStandardItemEntity.electricityFeePrice;
        BigDecimal serviceFeePrice = chargeStandardItemEntity.serviceFeePrice;

        // 衍生耗电量：基于峰值功率计算的耗电量
        BigDecimal derivedPowerConsumption;
        // 充电小时数（将充电时间从秒转换为小时），保留6位小数以提高精度
        BigDecimal chargingHour = BigDecimal.ZERO;

        try {
            // 判断充电时间是否大于0，如果大于0则将秒转换为小时
            if (chargeTime_second > 0) {
                // 将充电时间（秒）转换为小时，保留6位小数
                chargingHour = BigDecimal.valueOf(chargeTime_second)
                        .divide(new BigDecimal(3600), 6, RoundingMode.HALF_UP);
            }

            // 检查转换后是否存在有效的充电时长
            if (chargingHour.compareTo(BigDecimal.ZERO) > 0) {
                /*
                 * 电费计算过程：
                 * 计算方法：电费 = (结算功率（瓦）/ 1000) * 电费单价 * 充电小时数
                 * 使用的假设：用户在整个充电过程中使用峰值功率。
                 */
                if (settlementPower > 0) {
                    // 计算衍生耗电量
                    derivedPowerConsumption = BigDecimal.valueOf(settlementPower)
                            .divide(new BigDecimal(1000), 6, RoundingMode.HALF_UP)
                            .multiply(chargingHour); // 乘以充电小时数得出耗电量
                    // 计算电费金额 = 衍生耗电量 * 电费单价
                    electricityFeeAmount = derivedPowerConsumption.multiply(electricityFeePrice);
                }

                /*
                 * 服务费计算过程：
                 * 服务费 = 服务费单价 * 充电小时数
                 */
                serviceFeeAmount = serviceFeePrice.multiply(chargingHour);
            }

            // 计算总费用 = 电费 + 服务费
            totalAmount = electricityFeeAmount.add(serviceFeeAmount);
        } catch (Exception e) {
            // 记录费用计算过程中的任何异常
            LogsUtil.error(e, TAG, "");
        }
        return totalAmount;
    }

    /**
     * chargeFinishWithBalance - 使用余额进行扣费
     * <p>
     * 该方法实现了基于用户余额的充电费用结算逻辑。
     * 整个结算过程包括以下步骤：
     * 1. 查询并验证收费标准。
     * 2. 计算实际的充电费用（电费 + 服务费）。
     * 3. 检查用户余额是否足够支付。
     * 4. 扣减用户余额并记录扣费日志。
     * 5. 更新充电订单状态并保存至数据库。
     *
     * @param orderEntity     - 订单实体类，包含订单的详细信息。
     * @param deviceEntity    - 设备实体类，包含充电设备的详细信息。
     * @param settlementPower - 结算使用的功率值（瓦），用于费用计算。
     * @param stopTime        - 充电停止时间，以时间戳形式表示。
     * @param stopReasonCode  - 停止原因的代码，用于标识停止的具体原因。
     * @param stopReasonText  - 停止原因的文本描述，便于理解和记录。
     * @return SyncResult     - 返回一个SyncResult对象，表示结算的结果状态和相关信息。
     */
    private SyncResult chargeFinishWithBalance(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {

        // 记录开始进行余额结算的日志，便于后续跟踪和调试
        LogsUtil.info(TAG, "[%s] - %s:%s 余额结算", orderEntity.OrderSN, orderEntity.deviceCode, orderEntity.port);

        // 开始数据库事务处理，确保结算过程的原子性
        return ChargeOrderEntity.getInstance().beginTransaction(connection -> {
            int payment_status = 0; // 支付状态：0=未支付，1=已支付
            int status = 2; // 订单状态：-1=错误，0=待启动，1=充电中，2=已完成
            String status_msg = ""; // 状态消息，用于描述订单状态的具体信息

            // 步骤 1：根据用户选择的充电功率查询对应的收费配置
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, settlementPower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                // 如果收费标准无效，则记录错误日志并返回失败结果
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, settlementPower);
                return new SyncResult(2, "系统错误：收费标准数据不正确");
            }

            BigDecimal totalAmount = BigDecimal.ZERO; // 电费 + 服务费
            BigDecimal electricityFeeAmount = BigDecimal.ZERO; // 电费金额
            BigDecimal serviceFeeAmount = BigDecimal.ZERO; // 服务费金额
            BigDecimal electricityFeePrice = BigDecimal.ZERO; // 电费单价(元/度)
            BigDecimal serviceFeePrice = BigDecimal.ZERO; // 服务费单价(元/小时)
            BigDecimal derivedPowerConsumption = BigDecimal.ZERO; // 衍生耗电量，基于峰值功率计算
            BigDecimal discountAmount = orderEntity.discountAmount; // 用来记录实际优惠金额

            // 使用订单中的累计充电时间进行费用计算，避免中途跳闸等情况导致的时间误差
            long actualChargeTime = orderEntity.totalChargeTime;

            // 步骤 2：调用 billing 方法计算费用
            Map<String, BigDecimal> billData = billing(orderEntity, chargeStandardItemEntity, settlementPower, actualChargeTime);
            if (!billData.isEmpty()) {
                // 提取费用计算结果
                totalAmount = MapUtil.getBigDecimal(billData, "totalAmount");
                electricityFeeAmount = MapUtil.getBigDecimal(billData, "electricityFeeAmount");
                serviceFeeAmount = MapUtil.getBigDecimal(billData, "serviceFeeAmount");
                electricityFeePrice = MapUtil.getBigDecimal(billData, "electricityFeePrice");
                serviceFeePrice = MapUtil.getBigDecimal(billData, "serviceFeePrice");
                derivedPowerConsumption = MapUtil.getBigDecimal(billData, "derivedPowerConsumption");

                // region remark - 2025-10-20 回保险费用扣减
                if (orderEntity.safeCharge == 1) {
                    totalAmount = totalAmount.add(BigDecimal.valueOf(orderEntity.safeChargeFee));
                }
                // endregion

                // region 2025-01-20 新增逻辑：检测优惠金额，如果有优惠金额则进行总数扣减
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    if (totalAmount.compareTo(discountAmount) >= 0) {
                        // 如果 totalAmount >= discountAmount，则 totalAmount = totalAmount - discountAmount
                        totalAmount = totalAmount.subtract(discountAmount);
                    } else {
                        // 否则 totalAmount < discountAmount, totalAmount = 0
                        discountAmount = totalAmount; // 实际优惠金额为 totalAmount
                        totalAmount = BigDecimal.ZERO; // 总费用置为0
                    }
                }
                // endregion
            }

            // 实例化用户汇总实体，方便操作用户余额
            UserSummaryEntity userSummaryEntity = new UserSummaryEntity();

            // 处理充电预扣费的退款逻辑
            SyncResult esChargeRefundRefundResult = estimateAmountRefundTransaction(connection
                    , orderEntity.uid
                    , orderEntity.estimateAmount
                    , orderEntity.OrderSN
            );
            // 检查退款结果
            if (esChargeRefundRefundResult.code != 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：返回用户充电预扣费失败 OrderSN=%s DeviceCode=%s msg=%s", orderEntity.OrderSN, deviceEntity.deviceCode, esChargeRefundRefundResult.msg);
            }

            // 步骤 3：进行扣费操作，检查订单是否符合免费充电时间
            int freeChargeTimeLimit = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
            if (orderEntity.totalChargeTime > freeChargeTimeLimit) {
                // 检查订单是否未支付
                if (orderEntity.payment_status == 0) {
                    // 获取用户当前余额
                    BigDecimal balance = userSummaryEntity.getBalanceWithUid(orderEntity.uid);
                    if (balance.compareTo(totalAmount) < 0) {
                        // 余额不足时记录错误日志
                        LogsUtil.error(this.getClass().getSimpleName(), "[%s][%s] - %s 充电结算失败：用户余额不足，当前余额 %s"
                                , orderEntity.OrderSN
                                , deviceEntity.deviceCode
                                , orderEntity.uid
                                , balance);
                    } else {
                        // 检查是否已扣费，防止重复操作
                        if (!UserBalanceLogEntity.getInstance()
                                .where("type", EBalanceUpdateType.charge)
                                .where("orderSN", orderEntity.OrderSN)
                                .existTransaction(connection)) {
                            // 扣减用户余额
                            SyncResult payResult = userSummaryEntity.updateBalanceTransaction(connection
                                    , orderEntity.uid
                                    , totalAmount.setScale(2, RoundingMode.HALF_UP).negate() // 取负值进行扣减
                                    , EBalanceUpdateType.charge
                                    , "充电扣费"
                                    , orderEntity.OrderSN
                            );
                            if (payResult.code != 0) {
                                // 如果扣费失败则记录错误日志
                                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：用户扣款失败 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                            } else {
                                // 扣费成功则更新支付状态为已支付
                                payment_status = 1;
                            }
                        } else {
                            payment_status = 1; // 如果已扣费，直接更新支付状态
                        }
                    }
                } else {
                    // 如果订单已支付，则记录错误日志，防止重复扣费
                    LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：此笔充电订单用户已经支付过了，发生重复结算情况 OrderSN=%s DeviceCode=%s ORDER=%s", orderEntity.OrderSN, deviceEntity.deviceCode, JSONObject.toJSONString(orderEntity));
                }
            } else {
                // 免费充电时间内停止充电，不扣费
                status = -1;
                status_msg = String.format("在%s秒内进行结算无需扣款", freeChargeTimeLimit);
            }

            // 构建需要更新到订单的数据
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", status); // 更新订单状态
            set_data.put("status_msg", status_msg); // 更新状态消息
            set_data.put("payment_status", payment_status); // 更新支付状态
            set_data.put("totalAmount", totalAmount); // 总费用
            set_data.put("discountAmount", discountAmount);
            set_data.put("electricityFeeAmount", electricityFeeAmount); // 电费金额
            set_data.put("serviceFeeAmount", serviceFeeAmount); // 服务费金额
            set_data.put("electricityFeePrice", electricityFeePrice); // 电费单价
            set_data.put("serviceFeePrice", serviceFeePrice); // 服务费单价
            set_data.put("derivedPowerConsumption", derivedPowerConsumption); // 衍生耗电量
            set_data.put("stopTime", stopTime); // 停止时间
            set_data.put("stopReasonCode", stopReasonCode); // 停止原因代码
            set_data.put("stopReasonText", stopReasonText); // 停止原因文本
            if (orderEntity.is_cabinet == 1) { // 检查是否为充电柜使用
                set_data.put("door_status", 1); // 更新门状态为打开
            }

            // 更新订单数据到数据库并返回结果
            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }
            return new SyncResult(0, "", set_data);
        });
    }

    /**
     * chargeFinishWithChargeCard - 使用充电卡进行结算
     * <p>
     * 该方法实现了基于充电卡的充电费用结算逻辑。
     * 整个结算过程包括以下步骤：
     * 1. 根据充电功率计算充电卡的消耗时间。
     * 2. 查询并验证收费标准。
     * 3. 计算实际的充电费用（电费 + 服务费）。
     * 4. 更新充电订单状态，包括扣费时间和金额。
     *
     * @param orderEntity     - 充电订单详情，包含订单的详细信息。
     * @param deviceEntity    - 充电设备实体类，包含充电设备的详细信息。
     * @param settlementPower - 结算使用的功率值（瓦），用于费用计算。
     * @param stopTime        - 充电停止时间，以时间戳形式表示。
     * @param stopReasonCode  - 停止原因代码，用于标识停止的具体原因。
     * @param stopReasonText  - 停止原因文本描述，便于理解和记录。
     * @return SyncResult    - 返回一个SyncResult对象，表示结算的结果状态和相关信息。
     */
    private SyncResult chargeFinishWithChargeCard(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {

        // 记录充电卡结算操作的开始，便于后续日志追踪
        LogsUtil.info(TAG, "[%s] - %s:%s 充电卡结算", orderEntity.OrderSN, deviceEntity.deviceCode, orderEntity.port);

        // 开始数据库事务，确保结算过程的原子性
        return ChargeOrderEntity.getInstance().beginTransaction(connection -> {
            // 步骤 1：根据用户选择的充电功率查询对应的收费配置
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, orderEntity.limitChargePower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                // 如果收费标准无效，记录错误日志并返回失败结果
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, settlementPower);
                return new SyncResult(2, "系统错误：收费标准数据不正确");
            }

            // 步骤 2：计算充电卡的消耗时间，基于总充电时间和消耗时间比例
            long chargeCardConsumeTime = Convert.toLong(orderEntity.totalChargeTime * chargeStandardItemEntity.chargeCardConsumeTimeRate);

            BigDecimal totalAmount = BigDecimal.ZERO; // 总费用（电费 + 服务费）
            BigDecimal electricityFeeAmount = BigDecimal.ZERO; // 电费金额
            BigDecimal serviceFeeAmount = BigDecimal.ZERO; // 服务费金额
            BigDecimal electricityFeePrice = BigDecimal.ZERO; // 电费单价（元/度）
            BigDecimal serviceFeePrice = BigDecimal.ZERO; // 服务费单价（元/小时）
            BigDecimal derivedPowerConsumption = BigDecimal.ZERO; // 衍生耗电量，基于峰值功率计算

            // 使用订单中的累计充电时间进行费用计算，避免中途跳闸等情况导致的时间误差
            long actualChargeTime = orderEntity.totalChargeTime;

            // 步骤 3：调用 billing 方法计算费用
            Map<String, BigDecimal> billData = billing(orderEntity, chargeStandardItemEntity, settlementPower, actualChargeTime);
            if (!billData.isEmpty()) {
                // 提取费用计算结果
                totalAmount = MapUtil.getBigDecimal(billData, "totalAmount");
                electricityFeeAmount = MapUtil.getBigDecimal(billData, "electricityFeeAmount");
                serviceFeeAmount = MapUtil.getBigDecimal(billData, "serviceFeeAmount");
                electricityFeePrice = MapUtil.getBigDecimal(billData, "electricityFeePrice");
                serviceFeePrice = MapUtil.getBigDecimal(billData, "serviceFeePrice");
                derivedPowerConsumption = MapUtil.getBigDecimal(billData, "derivedPowerConsumption");

                // region remark - 2025-10-20 回保险费用扣减
                if (orderEntity.safeCharge == 1) {
                    totalAmount = totalAmount.add(BigDecimal.valueOf(orderEntity.safeChargeFee));
                }
                // endregion
            }

            // 步骤 4：构建需要更新到订单的数据
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", 2); // 更新订单状态为已完成
            set_data.put("payment_status", 1); // 更新支付状态为已支付
            set_data.put("electricityFeeAmount", electricityFeeAmount); // 电费金额
            set_data.put("serviceFeeAmount", serviceFeeAmount); // 服务费金额
            set_data.put("electricityFeePrice", electricityFeePrice); // 电费单价
            set_data.put("serviceFeePrice", serviceFeePrice); // 服务费单价
            set_data.put("derivedPowerConsumption", derivedPowerConsumption); // 衍生耗电量
            set_data.put("stopTime", stopTime); // 停止时间
            set_data.put("stopReasonCode", stopReasonCode); // 停止原因代码
            set_data.put("stopReasonText", stopReasonText); // 停止原因文本
            set_data.put("chargeCardConsumeTime", chargeCardConsumeTime); // 充电卡消耗时间
            set_data.put("chargeCardConsumeAmount", totalAmount.setScale(6, RoundingMode.HALF_UP)); // 扣费金额
            set_data.put("chargeCardConsumeTimeRate", chargeStandardItemEntity.chargeCardConsumeTimeRate);

            // 处理充电柜的状态
            if (orderEntity.is_cabinet == 1) {
                set_data.put("door_status", 1); // 如果是充电柜使用，更新门状态为打开
            }

            // 尝试更新订单数据到数据库
            if (orderEntity.update(orderEntity.id, set_data) == 0) {
                // 如果更新失败，则记录错误日志并返回失败结果
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }

            // 返回成功结果，并附带更新的数据
            return new SyncResult(0, "", set_data);
        });
    }

    /**
     * chargeFinishWithIntegral - 使用积分进行结算
     * <p>
     * 该方法实现了基于用户积分的充电费用结算逻辑。
     * 整个结算过程包括以下步骤：
     * 1. 查询并验证收费标准。
     * 2. 计算实际的充电费用（电费 + 服务费）。
     * 3. 扣减用户积分并记录扣费日志。
     * 4. 更新充电订单状态并保存至数据库。
     *
     * @param orderEntity     - 订单实体类，包含订单的详细信息。
     * @param deviceEntity    - 设备实体类，包含充电设备的详细信息。
     * @param settlementPower - 结算使用的功率值（瓦），用于费用计算。
     * @param stopTime        - 充电停止时间，以时间戳形式表示。
     * @param stopReasonCode  - 停止原因编码，用于标识停止的具体原因。
     * @param stopReasonText  - 停止原因文本描述，便于理解和记录。
     * @return SyncResult    - 返回一个SyncResult对象，表示结算的结果状态和相关信息。
     */
    private SyncResult chargeFinishWithIntegral(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {

        // 记录积分结算操作的开始，便于后续日志追踪
        LogsUtil.info(TAG, "[%s] - %s:%s 积分结算", orderEntity.OrderSN, deviceEntity.deviceCode, orderEntity.port);

        // 开始数据库事务，确保结算过程的原子性
        return ChargeOrderEntity.getInstance().beginTransaction(connection -> {

            // 初始化状态和支付标记：积分结算默认视为已支付
            int payment_status = 1; // 支付状态：0=未支付，1=已支付
            int status = 2; // 订单状态：-1=错误，0=待启动，1=充电中，2=已完成
            String status_msg = ""; // 状态消息，用于描述订单状态的具体信息

            // 步骤 1：查询并验证收费标准
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, orderEntity.limitChargePower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                // 如果收费标准无效，则记录错误日志并返回失败结果
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, settlementPower);
                return new SyncResult(2, "系统错误：收费标准数据不正确");
            }

            BigDecimal totalAmount = BigDecimal.ZERO; // 总费用（电费 + 服务费）
            BigDecimal electricityFeeAmount = BigDecimal.ZERO; // 电费金额
            BigDecimal serviceFeeAmount = BigDecimal.ZERO; // 服务费金额
            BigDecimal electricityFeePrice = BigDecimal.ZERO; // 电费单价（元/度）
            BigDecimal serviceFeePrice = BigDecimal.ZERO; // 服务费单价（元/小时）
            BigDecimal derivedPowerConsumption = BigDecimal.ZERO; // 衍生耗电量，基于峰值功率计算

            // 使用订单中的累计充电时间进行费用计算，避免中途跳闸等情况导致的时间误差
            long actualChargeTime = orderEntity.totalChargeTime;

            // 步骤 2：调用 billing 方法计算费用
            Map<String, BigDecimal> billData = billing(orderEntity, chargeStandardItemEntity, settlementPower, actualChargeTime);
            if (!billData.isEmpty()) {
                // 提取费用计算结果
                totalAmount = MapUtil.getBigDecimal(billData, "totalAmount");
                electricityFeeAmount = MapUtil.getBigDecimal(billData, "electricityFeeAmount");
                serviceFeeAmount = MapUtil.getBigDecimal(billData, "serviceFeeAmount");
                electricityFeePrice = MapUtil.getBigDecimal(billData, "electricityFeePrice");
                serviceFeePrice = MapUtil.getBigDecimal(billData, "serviceFeePrice");
                derivedPowerConsumption = MapUtil.getBigDecimal(billData, "derivedPowerConsumption");

                // region remark - 2025-10-20 回保险费用扣减
//                if (orderEntity.safeCharge == 1) {
//                    totalAmount = totalAmount.add(BigDecimal.valueOf(orderEntity.safeChargeFee));
//                }
                // endregion
            }

            // 构建需要更新到订单的数据
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", status); // 更新订单状态为已完成
            set_data.put("status_msg", status_msg); // 更新状态消息
            set_data.put("payment_status", payment_status); // 更新支付状态为已支付
            set_data.put("integralConsumeAmount", totalAmount.setScale(6, RoundingMode.HALF_UP)); // 积分消费金额
            set_data.put("electricityFeeAmount", electricityFeeAmount); // 电费金额
            set_data.put("serviceFeeAmount", serviceFeeAmount); // 服务费金额
            set_data.put("electricityFeePrice", electricityFeePrice); // 电费单价
            set_data.put("serviceFeePrice", serviceFeePrice); // 服务费单价
            set_data.put("derivedPowerConsumption", derivedPowerConsumption); // 衍生耗电量
            set_data.put("stopTime", stopTime); // 停止时间
            set_data.put("stopReasonCode", stopReasonCode); // 停止原因代码
            set_data.put("stopReasonText", stopReasonText); // 停止原因文本

            // 处理充电柜相关状态
            if (orderEntity.is_cabinet == 1) {
                // 如果是充电柜使用，更新门状态为打开
                set_data.put("door_status", 1);
            }

            // 尝试更新订单数据到数据库
            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                // 如果更新失败，则记录错误日志并返回失败结果
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }

            // 返回成功结果，并附带更新的数据
            return new SyncResult(0, "", set_data);
        });
    }

    /**
     * estimateAmountRefundTransaction - 充电预扣费退款方法
     * <p>
     * 此方法用于处理充电预扣费的退款逻辑，确保每笔订单仅退款一次。
     * 通过查询预扣费记录和退款记录，判断是否需要执行退款操作。
     * 执行成功后会更新用户的余额并记录退款日志。
     * <p>
     * 主要步骤包括：
     * 1. 检查订单是否存在预扣费记录。
     * 2. 检查是否已经进行了退款操作。
     * 3. 如果未退款，则执行退款操作，并记录退款日志。
     *
     * @param connection     - 数据库连接，确保操作在同一事务中执行。
     * @param uid            - 用户ID，用于标识进行退款操作的用户。
     * @param estimateAmount - 预扣费金额，需要退还给用户。
     * @param OrderSN        - 订单号，标识具体的充电订单。
     * @return SyncResult    - 返回一个SyncResult对象，表示退款操作的结果状态和相关信息。
     * @throws SQLException - 如果数据库操作出现异常，抛出SQLException。
     */
    private SyncResult estimateAmountRefundTransaction(Connection connection
            , long uid
            , double estimateAmount
            , String OrderSN) throws SQLException {

        // 步骤 1：先检查此订单是否存在预扣费记录
        if (!UserBalanceLogEntity.getInstance()
                .where("type", EBalanceUpdateType.escharge) // 查询类型为预扣费
                .where("OrderSN", OrderSN) // 根据订单号筛选
                .existTransaction(connection)) {
            // 若无预扣费记录，则无需退款，返回成功结果
            return new SyncResult(0, "没有充电预扣费");
        }

        // 步骤 2：检查是否已退款，确保同一订单不重复退款
        Map<String, Object> data = UserBalanceLogEntity.getInstance()
                .field("id") // 仅需查询记录的存在性
                .where("uid", uid) // 根据用户ID筛选
                .where("type", EBalanceUpdateType.escharge_refund) // 查询类型为退款记录
                .where("OrderSN", OrderSN) // 根据订单号筛选
                .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (data != null && !data.isEmpty()) {
            // 如果已经退款，则无需重复操作，返回成功结果
            return new SyncResult(0, "已经退款");
        }

        // 步骤 3：执行退款操作，将预扣费金额退还至用户余额，并记录日志
        return UserSummaryEntity.getInstance().updateBalanceTransaction(connection
                , uid
                , estimateAmount // 退款金额
                , EBalanceUpdateType.escharge_refund // 更新类型为退款
                , "充电预扣费退款" // 备注信息
                , OrderSN // 关联的订单号
                , null // 附加信息为空
                , 0 // 附加标记为0
        );
    }
}