package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.basedata.ChargeTimeItemEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.order.settlement.v2.ChargeSettlementForEBike_20241021;
import com.evcharge.entity.order.settlement.v2.IChargingSettlementV2;
import com.evcharge.entity.user.*;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.sys.SysMessageEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EChargePaymentType;
import com.evcharge.enumdata.ENotifyType;
import com.evcharge.libsdk.wechat.WechatSubscribeTmplSDK;
import com.evcharge.mqtt.XMQTTFactory;
import com.evcharge.service.User.UserSummaryService;
import com.evcharge.service.notify.NotifyService;
import com.evcharge.task.ParkingMonitorApp;
import com.evcharge.utils.EvChargeHelper;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 充电订单;
 *
 * @author : JED
 * @date : 2022-9-29
 */
public class ChargeOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 充电订单号
     */
    public String OrderSN;

    /**
     * 状态,-1=启动错误，0=待启动，1=充电中，2=已完成
     */
    public int status;
    /**
     * 状态错误说明
     */
    public String status_msg;

    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 设备码
     */
    public String deviceCode;
    /**
     * 端口
     */
    public int port;

    /**
     * 支付方式：1=余额，2=充电卡
     */
    public int paymentTypeId;
    /**
     * 支付状态：0=未支付，1=已支付
     */
    public int payment_status;
    /**
     * 收费类型：1=先付后充，2=先充后付
     */
    public int chargeType;
    /**
     * 充电模式：0=计时，1=包月
     */
    public int chargeMode;


    /**
     * 开始充电时间戳
     */
    public long startTime;
    /**
     * 预计结束充电时间戳
     */
    public long endTime;
    /**
     * 设备停止充电时间戳，结算更新
     */
    public long stopTime;

    /**
     * 充电时长,单位：秒
     */
    public long chargeTime;
    /**
     * 累计充电时长，心跳包更新
     */
    public long totalChargeTime;
    /**
     * 耗电量，心跳包更新
     */
    public double powerConsumption;
    /**
     * 当前功率
     */
    public double currentPower;
    /**
     * 显示给用户看的最大功率
     */
    public double maxPower;
    /**
     * 限制充电功率
     */
    public double limitChargePower;

    /**
     * 充电时长ID
     */
    public long chargeTimeItemId;
    /**
     * 充电收费ID
     */
    public long chargeStandardItemId;

    /**
     * 预计金额
     */
    public double estimateAmount;
    /**
     * 累计扣费金额
     */
    public double totalAmount;

    /**
     * 电费=衍生耗电量 * 电费单价 * 充电小时
     */
    public BigDecimal electricityFeeAmount;
    /**
     * 服务费=服务费电价 * 充电小时
     */
    public BigDecimal serviceFeeAmount;
    /**
     * 电费单价(元/度)
     */
    public BigDecimal electricityFeePrice;
    /**
     * 服务费单价(元/小时)
     */
    public BigDecimal serviceFeePrice;
    /**
     * 衍生耗电量,通过公式计算的耗电量
     */
    public BigDecimal derivedPowerConsumption;

    /**
     * 充电卡卡号
     */
    public String cardNumber;
    /**
     * 月卡消耗时间
     */
    public long chargeCardConsumeTime;
    /**
     * 充电卡消耗时间的倍率
     */
    public double chargeCardConsumeTimeRate;
    /**
     * 充电卡消耗的金额（用于统计计费）
     */
    public double chargeCardConsumeAmount;

    /**
     * 预计积分扣费
     */
    public int esIntegral;
    /**
     * 实际积分扣费
     */
    public int integral;
    /**
     * 预计积分消耗的金额（用于统计计费）
     */
    public double esIntegralConsumeAmount;
    /**
     * 实际积分消耗的金额（用于统计计费）
     */
    public double integralConsumeAmount;

    /**
     * 实际充电最大功率，心跳包更新
     */
    public double actualMaxPower;
    /**
     * 当前电压，心跳包更新
     */
    public double voltage;
    /**
     * 当前电流，心跳包更新
     */
    public double current;
    /**
     * 当前环境温度，心跳包更新
     */
    public double temperature;
    /**
     * 当前端口温度，心跳包更新
     */
    public double portTemperature;

    /**
     * 停止原因状态码，结算更新
     */
    public int stopReasonCode;
    /**
     * 停止原因文本，结算更新
     */
    public String stopReasonText;

    /**
     * 充满自停标识，0=否，1=是
     */
    public int chargeAutoStop;
    /**
     * 安全充电保险，0=不启用，1=启用
     */
    public int safeCharge;
    /**
     * 安全充电保险费用
     */
    public double safeChargeFee;

    /**
     * 电表Id
     */
    public long meterId;

    /**
     * 充电过载时重启，0-否，1-是
     */
    public int overloadRestart;
    /**
     * 父级订单号
     */
    public String parentOrderSN;
    /**
     * 充电柜标识：0-否，1-是
     */
    public int is_cabinet;
    /**
     * 充电柜使用，门状态：0-关闭，1-打开，-1-无
     */
    public int door_status;

    /**
     * 是否为测试订单，0=否，1=是
     */
    public int isTest;
    /**
     * 测试ID
     */
    public long testId;
    /**
     * 版本号
     */
    public int version;
    /**
     * 创建时间戳
     */
    public long create_time;


    /**
     * 用户的充电车辆ID
     */
    @Deprecated
    public long userEbikeId;
    /**
     * 充电卡ID（待删除）
     */
    public long chargeCardId;

    /**
     * 2024-01-19 新增折扣金额
     */
    public BigDecimal discountAmount;

    /**
     * 2024-01-19 标记是否残疾用户
     */
    public int is_disabled_user;


    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeOrderEntity getInstance() {
        return new ChargeOrderEntity();
    }

    /**
     * 根据用户的充电习惯获得一个限制充电功率
     * <p>
     * 根据用户最近的10次充电记录获取最大充电功率maxValue，然后分配当前充电功率=maxValue+maxValue*5%，如果不够10次，则直接分配950w
     */
    @Deprecated
    public double getLimitChargePowerWithUserHabit1(long uid, DeviceEntity deviceEntity, int port) {
        //充电功率：默认200w
        double chargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 200);
        //查询当前设备的电表数据
        ElectricityMeterEntity meterEntity = ElectricityMeterEntity.getInstance()
                .cache(String.format("ChargeStation:ElectricMeter:%s:Detilas", deviceEntity.meterId))
                .findEntity(deviceEntity.meterId);
        if (meterEntity == null || meterEntity.id == 0) return chargePower;

        //查询当前插座数据
        DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                .cache(String.format("Device:%s:Port:%s:Socket", deviceEntity.id, port))
                .where("deviceId", deviceEntity.id)
                .where("port", port)
                .findEntity();
        if (deviceSocketEntity == null || deviceSocketEntity.id == 0) return chargePower;

        //默认充电功率为插座的限制充电功率
        chargePower = deviceSocketEntity.limitChargePower;

        //获取用户最近10次充电最大功率
        Map<String, Object> user_data = UserSummaryEntity.getInstance()
                .field("id,lastTimeChargeMaxPower,charging_count")
                .where("uid", uid)
                .order("id DESC")
                .find();
        if (user_data.isEmpty()) return chargePower;
        int charging_count = MapUtil.getInt(user_data, "charging_count");
        //收集用户充电次数少于10次，直接开放默认功率给用户充电
        if (charging_count <= 10) return chargePower;

        double lastTimeChargeMaxPower = MapUtil.getDouble(user_data, "lastTimeChargeMaxPower");
        chargePower = lastTimeChargeMaxPower + lastTimeChargeMaxPower * 0.05;
        return Convert.toDouble(Convert.toInt(chargePower));
    }

    /**
     * 检查最后使用订单
     *
     * @param deviceCode 设备编码
     * @param port       端口
     * @param uid        用户id
     */
    public SyncResult checkLastUse(String deviceCode, int port, long uid) {
        ChargeOrderEntity orderTemp = ChargeOrderEntity.getInstance()
                .where("deviceCode", deviceCode)
                .where("port", port)
                .order("create_time DESC")
                .findEntity();
        if (orderTemp != null && orderTemp.id > 0 && orderTemp.status == 1 && orderTemp.uid == uid) {
            Map<String, Object> cb = new LinkedHashMap<>();
            cb.put("orderSN", orderTemp.OrderSN);
            cb.put("deviceCode", deviceCode);
            cb.put("port", port);
            return new SyncResult(200, "", cb);
        }
        return new SyncResult(1, "");
    }

    /**
     * 根据用户的充电习惯获得一个限制充电功率
     * <p>
     * 检查用户是否添加了车辆信息，如果添加了车辆信息则获取车辆信息中的充电功率来进行充电最大功率限制
     * 如果不存在车辆信息，则默认安全充电功率限制
     */
    public double getLimitChargePowerWithUserHabit2(long uid, long user_ebike_id, DeviceEntity deviceEntity, int port) {
        //充电功率：默认200w
        double chargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 200);
        if (user_ebike_id == 0) return chargePower;

        //查询用户是否存在车辆信息，如果存在车辆信息，则按照车辆信息最大充电功率进行充电限制
        UserEbikeEntity userEbikeEntity = UserEbikeEntity.getInstance()
                .cache(String.format("User:%s:Ebike:%s:Details", uid, user_ebike_id))
                .where("id", user_ebike_id)
                .where("uid", uid)
                .findEntity();
        if (userEbikeEntity == null || userEbikeEntity.id == 0) return chargePower;

        //查询当前插座数据
        DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                .cache(String.format("Device:%s:Port:%s:Socket", deviceEntity.id, port))
                .where("deviceId", deviceEntity.id)
                .where("port", port)
                .findEntity();
        if (deviceSocketEntity == null || deviceSocketEntity.id == 0) return chargePower;

        //检查是否超过插座最大限制充电功率
        if (userEbikeEntity.charger_max_power > deviceSocketEntity.limitChargePower) {
            chargePower = deviceSocketEntity.limitChargePower;
        } else if (userEbikeEntity.charger_max_power > 0) {
            chargePower = userEbikeEntity.charger_max_power;
        }

        return chargePower;
    }

    /**
     * 尝试结单，一般提供给后台使用
     *
     * @param OrderSN        充电订单号
     * @param stopReasonCode 停止代码
     * @param stopReasonText 停止原因
     * @return
     */
    public SyncResult tryChargeFinish(String OrderSN, int stopReasonCode, String stopReasonText) {
        //查找充电订单数据
        ChargeOrderEntity orderEntity = new ChargeOrderEntity()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(3, "订单不存在");
        return tryChargeFinish(orderEntity, stopReasonCode, stopReasonText);
    }

    /**
     * 尝试结单，一般提供给后台使用
     *
     * @param orderEntity    充电订单
     * @param stopReasonCode 停止代码
     * @param stopReasonText 停止原因
     * @return
     */
    public SyncResult tryChargeFinish(@NonNull ChargeOrderEntity orderEntity, int stopReasonCode, String stopReasonText) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请选择订单");
        if (!StringUtils.hasLength(stopReasonText)) return new SyncResult(2, "请输入停止原因");

        //状态,-1=启动错误，0=待启动，1=充电中，2=已完成
        if (orderEntity.status == 2) return new SyncResult(4, "订单已完成，无需结束订单");
        if (orderEntity.status == -1) return new SyncResult(5, "订单已经处理");

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity != null) {
            //尝试下发指令结束订单，这里不一定会成功，如果主机断网或者断电了，就无法使用了
            JSONObject json = new JSONObject();
            json.put("deviceCode", orderEntity.deviceCode);
            json.put("ChargeMode", orderEntity.chargeMode);
            json.put("port", orderEntity.port);//端口
            json.put("OrderSN", OrderSN);
            XMQTTFactory.getInstance().publish(String.format("%s/%s/command/stopCharge"
                            , deviceEntity.appChannelCode
                            , orderEntity.deviceCode)
                    , json.toJSONString());
        }

        // 延迟3秒执行，让客户端等待一下，减少并发，同时优先设备端发送数据上来结单，减少手动结单
//        ThreadPoolManager.sleep(3000);

        LogsUtil.warn(this.getClass().getSimpleName(), "[%s]尝试结束订单", orderEntity.OrderSN);

        long stopTime = orderEntity.startTime;
        double settlementPower = 0;
        ChargeOrderHeartbeatEntity heartbeatEntity = ChargeOrderHeartbeatEntity.getInstance()
                .where("order_id", orderEntity.id)
                .order("create_time DESC")
                .findEntity();
        if (heartbeatEntity == null || heartbeatEntity.id == 0) stopReasonText = "充电启动错误";
        if (heartbeatEntity != null) {
            stopTime = heartbeatEntity.create_time;
            settlementPower = ChargeOrderHeartbeatEntity.getInstance()
                    .where("order_id", orderEntity.id)
                    .max("maxPower");
        }
        return ChargeOrderEntity.getInstance().chargeFinish(OrderSN, settlementPower, stopTime, stopReasonCode, stopReasonText);
    }

    /**
     * 结算充电
     */
    public SyncResult chargeFinish(String OrderSN, int stopReasonCode, String stopReasonText) {
        return chargeFinish(OrderSN, 0, TimeUtil.getTimestamp(), stopReasonCode, stopReasonText);
    }

    /**
     * 结算充电
     */
    public SyncResult chargeFinish(String OrderSN
            , double settlementPower
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        String logMessage = "";
        logMessage += String.format("\033[1;94m 充电结算：OrderSN=%s stopReasonCode=%s stopReasonText=%s \033[0m", OrderSN, stopReasonCode, stopReasonText);
        LogsUtil.info(this.getClass().getSimpleName(), "%s", logMessage);

        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();

        //判断是否已经进行了软结算
        if (orderEntity.status == 2) {
            return new SyncResult(0, "已结算");
        }
        if (orderEntity.stopTime != 0) stopTime = orderEntity.stopTime;
        if (stopTime <= 0) stopTime = TimeUtil.getTimestamp();

        //region 查询设备数据

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) {
            LogsUtil.error(this.getClass().getSimpleName(), "查询设备数据出错,deviceCode=%s", orderEntity.deviceCode);
            return new SyncResult(1, "查询设备数据出错");
        }

        //endregion

        // region 2024-10-21 版本收费模式（废弃逻辑）
        /*
         * 1.	选择档位功率结算时，将峰值功率向下取整，并根据此功率查询收费标准进行结算
         * 2.	自动识别功率，并默认下发1000W
         * 3.	自动识别功率结算时，保留实际峰值功率的小数部分，不取整，并根据此功率查询收费标准进行结算
         * 4.	新增字段保存实际峰值功率。自动识别功率结算时将原值maxPower向上取整（10~20W）
         */
//        // 如果上级提供的结算功率低于真实的结算功率，则取最大值
//        if (settlementPower != 0) settlementPower = Math.max(orderEntity.actualMaxPower, settlementPower);
//        // 如果结算功率为0，则进行自动匹配
//        if (settlementPower == 0) settlementPower = orderEntity.actualMaxPower;
//        if (settlementPower == 0) settlementPower = orderEntity.maxPower; // 避免没有更新到真实的功率数据
//        if (settlementPower == 0) settlementPower = orderEntity.limitChargePower; // 避免没有更新到真实的功率数据
//        if (orderEntity.overloadRestart == 0) {
//            // 选择档位功率结算时，将峰值功率向下取整，并根据此功率查询收费标准进行结算
//            settlementPower = Math.floor(settlementPower);
//        }
//
//        IChargingSettlementV2 settlement = new ChargeSettlementForEBike_20241021();
//        SyncResult r = settlement.chargeFinish(orderEntity, deviceEntity, settlementPower, stopTime, stopReasonCode, stopReasonText);
//        if (r.code == 0) {
//            //新增充电次数和时间：计时直接计算用户选择的时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
//        }
        // endregion

        // region 2024-10-21 版本收费模式(修改)
        /*
         * 1.	选择档位功率结算时，将峰值功率向下取整，并根据此功率查询收费标准进行结算
         * 2.	自动识别功率，不再默认下发1000W，而是从最低功率逐步提升
         * 3.	自动识别功率结算时，保留实际峰值功率的小数部分，不取整，并根据此功率查询收费标准进行结算
         * 4.	新增字段保存实际峰值功率。自动识别功率结算时将原值maxPower向上取整（10~20W）
         */
        // 如果上级提供的结算功率低于真实的结算功率，则取最大值
        if (settlementPower != 0) settlementPower = Math.max(orderEntity.actualMaxPower, settlementPower);
        // 如果结算功率为0，则进行自动匹配
        // 优先对比真是功率和限制功率（取最小值），因为会出现功率过载的情况（用下一个档位进行了结算），所以取最小值
        if (settlementPower == 0) settlementPower = Math.min(orderEntity.actualMaxPower, orderEntity.limitChargePower);
        if (settlementPower == 0) settlementPower = orderEntity.actualMaxPower; // 如果出现功率结算为0的情况下，优先取真实功率
        if (settlementPower == 0) settlementPower = orderEntity.limitChargePower; // 如果出现功率为0的情况下，优先取限制功率
        if (settlementPower == 0) settlementPower = orderEntity.maxPower; // 避免没有更新到真实的功率数据
        if (orderEntity.overloadRestart == 0) {
            // 选择档位功率结算时，将峰值功率向下取整，并根据此功率查询收费标准进行结算
            settlementPower = Math.floor(settlementPower);
        }

        IChargingSettlementV2 settlement = new ChargeSettlementForEBike_20241021();
        SyncResult r = settlement.chargeFinish(orderEntity, deviceEntity, settlementPower, stopTime, stopReasonCode, stopReasonText);
        if (r.code == 0) {
            //新增充电次数和时间：计时直接计算用户选择的时长
            UserSummaryService.asyncUpdateUserChargeDataTask(orderEntity);
        }
        // endregion

        //如果用户是会员并且使用了9分钱 则需要将9分钱退款 无需后付费
//        if (orderEntity.safeCharge == 1) {
//            UserMemberService userMemberService = new UserMemberService();
//            long uid = orderEntity.uid;
//            if (userMemberService.checkUserIsMember(uid)) {
//                UserSummaryEntity.getInstance().updateBalance(uid
//                        , orderEntity.safeChargeFee
//                        , EBalanceUpdateType.safe_charge_fee_refund);
//            }
//        }

        // 根据扣费模式进行结算 修改日志：2024-10-21 使用新的计费方式：电费+服务费
//        SyncResult r;
//        EChargePaymentType ePaymentType = EChargePaymentType.valueOf(orderEntity.paymentTypeId);
//        switch (ePaymentType) {
//            case Balance:
//                r = chargeFinishWithBalance(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
//                break;
//            case ChargeCard:
//                r = chargeFinishWithChargeCard(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
//                break;
//            case Integral:
//                r = chargeFinishWithIntegral(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
//                break;
//            default:
//                return new SyncResult(11, "无效扣费方式");
//        }

        //重新查询一系订单信息

//        orderEntity = ChargeOrderEntity.getInstance()
//                .where("OrderSN", OrderSN)
//                .findEntity();

        Map<String, Object> cbData = (Map<String, Object>) r.data;
        double totalAmount = MapUtil.getDouble(cbData, "totalAmount");

        boolean sendWechatSubNotify = true;

        //region 当出现充电过载的时候检查订单是否授权了自动重启
        if (orderEntity.overloadRestart == 1) {
            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    SyncResult restartChargeResult = restartCharging(orderEntity);
                    if (restartChargeResult.code != 0) {
                        LogsUtil.warn(this.getClass().getSimpleName(), "充电重启 %s - 无法重启，%s", orderEntity.OrderSN, restartChargeResult.msg);

                        String title = "无法重启充电通知";
                        String message = "亲爱的用户，系统无法为您重启充电服务，原因：" + restartChargeResult.msg;
                        SysMessageEntity.getInstance().sendSysNotice(orderEntity.uid, title, message);
                    } else sendWechatSubNotify = false; // 不进行发送通知
                    break;
            }
        }
        //endregion

        //根据充电订单进行分润
//        int isShareProfit = SysGlobalConfigEntity.getInt("ChargeStation:ShareProfit", 1);
//        if (orderEntity.paymentTypeId == 1 && isShareProfit == 1) {
//            ChargeStationShareProfitEntity.getInstance().threadShareProfitCheckWithChargeOrder(orderEntity);
//        }

        //占用费收费监控
//        ParkingMonitorApp.add(orderEntity);

        //region 微信充电完成通知
        if (sendWechatSubNotify) {
            try {
                String PortText = "未知";
                DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode, orderEntity.port);
                if (deviceSocketEntity != null && deviceSocketEntity.id > 0) {
                    PortText = String.valueOf(deviceSocketEntity.index);

                    deviceSocketEntity.where("deviceId", deviceEntity.id)
                            .where("port", orderEntity.port)
                            .update(new HashMap<>() {{
                                put("usePower", 0.0);
                            }});
                }

                ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(String.format("%s", orderEntity.CSId));
                String ChargeStandardName = "";
                if (chargeStationEntity != null) {
                    ChargeStandardName = chargeStationEntity.name;
                }

                String ChargeTimeText = TimeUtil.convertToFormatTime(orderEntity.chargeTime);
                if (orderEntity.paymentTypeId == EChargePaymentType.ChargeCard.index) {
                    ChargeTimeText = TimeUtil.convertToFormatTime(orderEntity.totalChargeTime);
                }

                Map<String, Object> noticeData = new HashMap<>();
                noticeData.put("OrderSN", OrderSN);
                noticeData.put("ChargeStandardName", ChargeStandardName);
                noticeData.put("Port", PortText);
                noticeData.put("ChargeTime", ChargeTimeText);
                //2022/11/2 修复充电完成的微信通知，显示充电时间的小数点N位
                noticeData.put("totalAmount", String.format("%.2f", totalAmount));
                //2022/11/2 修改通知模板，添加停止原因
                noticeData.put("stopReasonText", stopReasonText);
                wechatChargeDoneNotice(orderEntity.uid, noticeData);
            } catch (Exception e) {
                LogsUtil.error(e, this.getClass().getSimpleName(), "发送充电完成通知发生错误");
            }
        }
        //endregion

        // region 2025-04-09 农业银行活动短信通知，临时使用

        ThreadUtil.getInstance().execute("农业银行活动短信通知", () -> {
            long uid = orderEntity.uid;

            // 限制30天推送一次
            int abcBankSMSNotify = DataService.getMainCache().getInt(String.format("Activity:ABCBank:SMS:%s", uid), -1);
            if (abcBankSMSNotify == 1) return;

            // 查询用户信息
            UserEntity userEntity = UserEntity.getInstance().findUserByUid(uid);
            if (userEntity == null) {
                LogsUtil.warn("农业银行活动SMS推送", "无法找到用户信息 uid=%s", uid);
                return;
            }
            if (StringUtil.isEmpty(userEntity.phone)) {
                LogsUtil.warn("农业银行活动SMS推送", "用户手机号码为空值 uid=%s", uid);
                return;
            }

            // 发送短信
            JSONObject notifyTransData = new JSONObject();
            notifyTransData.put("accept_list", userEntity.phone);
            notifyTransData.put("platform_code", "genkigo");
            notifyTransData.put("organize_code", "genkigo");
            NotifyService.getInstance().asyncPush(String.format("%s", uid)
                    , "CHARGE.END.ACTIVITY.PUSH"
                    , ENotifyType.SMS
                    , notifyTransData
            );

            DataService.getMainCache().set(String.format("Activity:ABCBank:SMS:%s", uid), 1, ECacheTime.MONTH);
        });

        // endregion

        //发送用户站内通知
        SysMessageEntity.getInstance().sendChargeNoticeToUserWithStopReasonCode(stopReasonCode, orderEntity);
        return r;
    }
//
//    /**
//     * 使用余额结算
//     *
//     * @param orderEntity    订单实体类
//     * @param deviceEntity   设备实体类
//     * @param stopReasonCode 停止原因编码
//     * @param stopReasonText 停止原因文本
//     */
//    private SyncResult chargeFinishWithBalance(ChargeOrderEntity orderEntity
//            , DeviceEntity deviceEntity
//            , long stopTime
//            , int stopReasonCode
//            , String stopReasonText) {
//        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
//            double totalAmount = 0.0;
//            int payment_status = 0;//支付状态：0=未支付，1=已支付
//            int status = 2;//状态,-1=错误，0=待启动，1=充电中，2=已完成
//            String status_msg = "";
//
//            //根据用户选择的充电功率进行查询收费配置
//            double chargePower = orderEntity.limitChargePower;
//            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
//                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
//            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
//                return new SyncResult(2, "系统错误：收费标准数据不正确");
//            }
//
//            /*
//             * 2023-11-08 上线的收费模式
//             * 收费标准：每30分钟收一次费用，不满30分钟，按30分钟收费。
//             * 特殊情况：
//             * 1、开始充电后10分钟停止充电免收费用。
//             * 2、充电时长超过30分钟的整数倍的余数部分，如小于等于10分钟，则按10分钟收费。
//             */
//            totalAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);
//
//            UserSummaryEntity userSummaryEntity = new UserSummaryEntity();
//
//            //region 充电预扣费退款
//            SyncResult esChargeRefundRefundResult = esChargeRefundTransaction(connection
//                    , orderEntity.uid
//                    , orderEntity.estimateAmount
//                    , orderEntity.OrderSN);
//            //记录一下日志
//            if (esChargeRefundRefundResult.code != 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：返回用户充电预扣费失败 OrderSN=%s DeviceCode=%s msg=%s", orderEntity.OrderSN, deviceEntity.deviceCode, esChargeRefundRefundResult.msg);
//            }
//            //endregion
//
//            //region 进行扣费操作
//
//            //如果扣费金额为0可能是免结算时间内停止充电
//            if (totalAmount == 0) {
//                int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
//                status = -1;
//                status_msg = String.format("在%s秒内进行结算无需扣款", startChargeFreeTime);
//                payment_status = 0;
//                totalAmount = 0.0;
//            } else {
//                //检查此笔充电订单是否已经支付了
//                if (orderEntity.payment_status == 1) {
//                    LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：此笔充电订单用户已经支付过了，发生重复结算情况 OrderSN=%s DeviceCode=%s ORDER=%s", orderEntity.OrderSN, deviceEntity.deviceCode, JSONObject.toJSONString(orderEntity));
//                } else {
//                    //检查用户是否足够余额
//                    double balance = userSummaryEntity.getBalanceDoubleWithUid(orderEntity.uid);
//                    if (balance < totalAmount) {
//                        payment_status = 0;//未支付
//                        LogsUtil.error(this.getClass().getSimpleName(), "[%s][%s] - %s 充电结算失败：用户余额不足，当前余额 %s", orderEntity.OrderSN, deviceEntity.deviceCode, uid, balance);
//                    } else {
//                        //检查是否已经扣费了
//                        if (!UserBalanceLogEntity.getInstance()
//                                .where("type", EBalanceUpdateType.charge)
//                                .where("orderSN", orderEntity.OrderSN)
//                                .existTransaction(connection)) {
//                            SyncResult payResult = userSummaryEntity.updateBalanceTransaction(connection
//                                    , orderEntity.uid
//                                    , -totalAmount
//                                    , EBalanceUpdateType.charge
//                                    , "充电扣费"
//                                    , orderEntity.OrderSN
//                            );
//                            if (payResult.code != 0) {
//                                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：用户扣款失败 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
//                            } else payment_status = 1;//已支付
//                        } else payment_status = 1;//已支付
//                    }
//                }
//            }
//            //endregion
//
//            Map<String, Object> set_data = new LinkedHashMap<>();
//            set_data.put("status", status);//状态,-1=错误，0=待启动，1=充电中，2=已完成
//            set_data.put("status_msg", status_msg);
//            set_data.put("payment_status", payment_status);//支付状态：0=未支付，1=已支付
//            set_data.put("totalAmount", totalAmount);
//            set_data.put("stopTime", stopTime);
//            set_data.put("stopReasonCode", stopReasonCode);
//            set_data.put("stopReasonText", stopReasonText);
//            //充电柜标识：0-否，1-是
//            if (orderEntity.is_cabinet == 1) {
//                //充电柜使用，门状态：0-关闭，1-打开，-1-无
//                set_data.put("door_status", 1);
//            }
//
//            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
//                return new SyncResult(1, "结算失败");
//            }
//            return new SyncResult(0, "");
//        });
//        if (r.code == 0) {
//            //新增充电次数和时间：计时直接计算用户选择的时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
//        }
//        return r;
//    }
//
//    /**
//     * 使用充电卡结算
//     *
//     * @param orderEntity    充电订单详情
//     * @param deviceEntity   充电设备实体类
//     * @param stopReasonCode 停止充电代码
//     * @param stopReasonText 停止充电原因文本
//     */
//    private SyncResult chargeFinishWithChargeCard(ChargeOrderEntity orderEntity
//            , DeviceEntity deviceEntity
//            , long stopTime
//            , int stopReasonCode
//            , String stopReasonText) {
//        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
//            //根据充电功率计算充电卡的消耗倍数时间
//            long chargeCardConsumeTime = Convert.toLong(orderEntity.totalChargeTime * orderEntity.chargeCardConsumeTimeRate);
//            double chargeCardConsumeAmount = 0.0;
//
//            //region 计算充电卡对应消耗的金额
//
//            //根据用户选择的充电功率进行查询收费配置
//            double chargePower = orderEntity.limitChargePower;
//            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
//                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
//            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
//            } else {
//                /*
//                 * 2023-11-08 上线的收费模式
//                 * 收费标准：每30分钟收一次费用，不满30分钟，按30分钟收费。
//                 * 特殊情况：
//                 * 1、开始充电后10分钟停止充电免收费用。
//                 * 2、充电时长超过30分钟的整数倍的余数部分，如小于等于10分钟，则按10分钟收费。
//                 */
//                chargeCardConsumeAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);
//            }
//            //endregion
//
//            Map<String, Object> set_data = new LinkedHashMap<>();
//            set_data.put("status", 2);//状态,-1=错误，0=待启动，1=充电中，2=已完成
//            set_data.put("payment_status", 1);//支付状态：0=未支付，1=已支付
//            set_data.put("stopTime", stopTime);
//            set_data.put("stopReasonCode", stopReasonCode);
//            set_data.put("stopReasonText", stopReasonText);
//            set_data.put("chargeCardConsumeTime", chargeCardConsumeTime);
//            set_data.put("chargeCardConsumeAmount", chargeCardConsumeAmount);
//            //充电柜标识：0-否，1-是
//            if (orderEntity.is_cabinet == 1) {
//                //充电柜使用，门状态：0-关闭，1-打开，-1-无
//                set_data.put("door_status", 1);
//            }
//            if (orderEntity.update(orderEntity.id, set_data) == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
//                return new SyncResult(1, "结算失败");
//            }
//            return new SyncResult(0, "");
//        });
//        if (r.code == 0) {
//            //新增充电次数和时间：月卡计算实时充电时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
//        }
//        return r;
//    }
//
//    /**
//     * 使用积分结算
//     *
//     * @param orderEntity    订单实体类
//     * @param deviceEntity   设备实体类
//     * @param stopReasonCode 停止原因编码
//     * @param stopReasonText 停止原因文本
//     */
//    private SyncResult chargeFinishWithIntegral(ChargeOrderEntity orderEntity
//            , DeviceEntity deviceEntity
//            , long stopTime
//            , int stopReasonCode
//            , String stopReasonText) {
//        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
//            double totalAmount = 0.0;
//            int payment_status = 1;//支付状态：0=未支付，1=已支付
//            int status = 2;//状态,-1=错误，0=待启动，1=充电中，2=已完成
//            String status_msg = "";
//
//            //根据用户选择的充电功率进行查询收费配置
//            double chargePower = orderEntity.limitChargePower;
//            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
//                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
//            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
//                return new SyncResult(2, "系统错误：收费标准数据不正确");
//            }
//
//            //计算一下实际扣费是多少，用于统计参考
//            totalAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);
//
//            //region 进行扣费操作
//
//            //如果扣费金额为0可能是免结算时间内停止充电
////            if (totalAmount == 0) {
////                int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
////                status = -1;
////                status_msg = String.format("在%s秒内进行结算无需扣款", startChargeFreeTime);
////                payment_status = 0;
////                totalAmount = 0.0;
////            }
//            //endregion
//
//            Map<String, Object> set_data = new LinkedHashMap<>();
//            set_data.put("status", status);//状态,-1=错误，0=待启动，1=充电中，2=已完成
//            set_data.put("status_msg", status_msg);
//            set_data.put("payment_status", payment_status);//支付状态：0=未支付，1=已支付
//            set_data.put("integralConsumeAmount", totalAmount);
//            set_data.put("stopTime", stopTime);
//            set_data.put("stopReasonCode", stopReasonCode);
//            set_data.put("stopReasonText", stopReasonText);
//            //充电柜标识：0-否，1-是
//            if (orderEntity.is_cabinet == 1) {
//                //充电柜使用，门状态：0-关闭，1-打开，-1-无
//                set_data.put("door_status", 1);
//            }
//
//            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
//                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
//                return new SyncResult(1, "结算失败");
//            }
//            return new SyncResult(0, "");
//        });
//        if (r.code == 0) {
//            //新增充电次数和时间：计时直接计算用户选择的时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
//        }
//        return r;
//    }

    /**
     * 重启充电
     */
    private SyncResult restartCharging(ChargeOrderEntity orderEntity) {
        LogsUtil.info(this.getClass().getSimpleName(), "[充电订单重启] - %s - 当前限制最大功率：%s - 继承订单号：", orderEntity.OrderSN, orderEntity.limitChargePower, orderEntity.OrderSN);

        double limitChargePower = Math.max(orderEntity.maxPower, orderEntity.limitChargePower);

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        //根据 limitChargePower 查询下一档充电功率
        ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                .where("configId", deviceEntity.chargeStandardConfigId)
                .where("maxPower", ">", limitChargePower)
                .order("maxPower ASC")
                .findEntity();
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            //查询不到有可能是最大功率的档位了
            return new SyncResult(1, "用户充电器的功率过大，无法自动换挡");
        }

        // remark - 2025-09-28 优化阶梯式充电：自动计算或重新选择充电时间
        long chargeTimeItemId = orderEntity.chargeTimeItemId;
        if (chargeTimeItemId > 0) {
            // chargeTimeItemId = -1 一般情况下都是使用了充电卡充电
            chargeTimeItemId = ChargeTimeItemEntity.getInstance().selectChargeTimeItemNearestOnAutoRestart(orderEntity.chargeTimeItemId, orderEntity.totalChargeTime, orderEntity.chargeTimeItemId);
            if (chargeTimeItemId == 0) return new SyncResult(111, "无需再重启充电了");
        }

        EvChargeHelper helper = EvChargeHelper.getInstance()
                .setUid(orderEntity.uid)
                .setDeviceCode(orderEntity.deviceCode)
                .setPort(orderEntity.port)
                .setChargeTimeItemId(chargeTimeItemId)
                .setSafeCharge(orderEntity.safeCharge == 1)
                .setPaymentType(EChargePaymentType.valueOf(orderEntity.paymentTypeId))
                .setLimitChargePower(chargeStandardItemEntity.maxPower)
                .setOverloadRestart(orderEntity.overloadRestart == 1)
                .setParentOrderSN(orderEntity.OrderSN)
                .setIsDisabledUser(orderEntity.is_disabled_user);//2024-01-19 新增残疾人用户设置

        if (orderEntity.paymentTypeId == EChargePaymentType.ChargeCard.index) {
            helper.setCardNumber(orderEntity.cardNumber);
        }
        return helper.start();
    }

    /**
     * 微信订阅充电完成通知
     */
    public void wechatChargeDoneNotice(long uid, Map<String, Object> data) {
        String templateId = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:TemplateId");

        Map<String, Object> msgObj;

        //character_string1={OrderSN}&thing2={ChargeStandardName}&amount6={totalAmount}&thing12={ChargeTime}&thing9={stopReasonText}
        String templateData = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:TemplateData");
        try {
            //region 替换值data的值
//            Iterator it = data.keySet().iterator();
//            while (it.hasNext()) {
//                String key = (String) it.next();
//                String value = MapUtil.getString(data, key);
//                templateData = templateData.replace(String.format("{%s}", key), value);
//            }
//            String[] temp = templateData.split("&");
//            for (String str : temp) {
//                String[] s = str.split("=");
//                String key = s[0];
//                Object value = s[1];
//                msgObj.put(key, value);
//            }
            //endregion

            //获得openId
            Map<String, Object> wechatUser = UserEntity.getInstance().findWxUserByID(uid);
            String openId = MapUtil.getString(wechatUser, "open_id");

            //获得跳转路径
            String jumpUrl = SysGlobalConfigEntity.getString("Wechat:App:ChargeDoneNotice:JumpPath");
            jumpUrl = jumpUrl.replace("{OrderSN}", MapUtil.getString(data, "OrderSN"));

            //发送通知
            WechatSubscribeTmplSDK wechatSubscribeTmplSDK = new WechatSubscribeTmplSDK();
            msgObj = wechatSubscribeTmplSDK.createTemplateData(templateData, data);

            SyncResult r = wechatSubscribeTmplSDK.sendSubscribeMessage(openId, jumpUrl, templateId, msgObj);
            if (r.code != 0) {
                LogsUtil.error("微信订阅消息", "充电完成通知，发生错误，原因：%s", r.msg);
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getSimpleName(), "微信订阅充电完成通知 - 发生错误 templateId：%s templateData：%s", templateId, templateData);
        }
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     */
    public SyncResult esChargeRefund(long uid, double estimateAmount, String OrderSN) {
        return beginTransaction(connection -> esChargeRefundTransaction(connection, uid, estimateAmount, OrderSN));
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param connection     事务
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     */
    public SyncResult esChargeRefundTransaction(Connection connection, long uid, double estimateAmount, String OrderSN) throws SQLException {
        return esChargeRefundTransaction(connection, uid, estimateAmount, OrderSN, 0);
    }

    /**
     * 充电预扣费退款，会自动检查此订单是否已经退款了
     *
     * @param connection     事务
     * @param uid            用户ID
     * @param estimateAmount 预扣费金额
     * @param OrderSN        订单号
     * @param testId         测试ID
     */
    public SyncResult esChargeRefundTransaction(Connection connection, long uid, double estimateAmount, String OrderSN, long testId) throws SQLException {
        //先检查是否此笔订单是否已预扣费
        if (!UserBalanceLogEntity.getInstance()
                .where("type", EBalanceUpdateType.escharge)
                .where("OrderSN", OrderSN)
                .existTransaction(connection)) {
            return new SyncResult(0, "没有充电预扣费");
        }

        Map<String, Object> data = UserBalanceLogEntity.getInstance()
                .field("id")
                .where("uid", uid)
                .where("type", EBalanceUpdateType.escharge_refund)
                .where("OrderSN", OrderSN)
                .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (data != null && !data.isEmpty()) return new SyncResult(0, "已经退款");

        return UserSummaryEntity.getInstance().updateBalanceTransaction(connection
                , uid
                , estimateAmount
                , EBalanceUpdateType.escharge_refund
                , "充电预扣费退款"
                , OrderSN
                , null
                , testId
        );
    }

    /**
     * 修复充电卡消耗的对应金额
     */
    public static long repairChargeCardConsumeAmount(long id) {
        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("id", ">", id)
                .where("status", 2)//状态,-1=错误，0=待启动，1=充电中，2=已完成
                .where("paymentTypeId", 2) //支付方式：1=余额，2=充电卡
                .where("isTest", 0)
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) {
            LogsUtil.info(getInstance().getClass().getSimpleName(), "订单修复 - 修复完毕");
            return -1;
        }

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) return orderEntity.id;

        //根据充电功率计算充电卡的消耗倍数时间
        long chargeCardConsumeTime = Convert.toLong(orderEntity.totalChargeTime * orderEntity.chargeCardConsumeTimeRate);
        double chargeCardConsumeAmount = 0.0;

        //region 计算充电卡对应消耗的金额

        double chargePower = orderEntity.limitChargePower;
        if (chargePower == 0) chargePower = orderEntity.maxPower;

        long chargeStandardConfigId = deviceEntity.chargeStandardConfigId;
        //订单的停止时间如果小于：2023-09-07 10:45:00 的话，证明收费标准配置是 4 不然则按照最新设备收费标准
        if (orderEntity.stopTime <= 1694054700000L) {
            chargeStandardConfigId = 2;
        }

        //根据 功率 和 设备 查找对应的收费档位
        ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                .getItemWithConfig(chargeStandardConfigId, chargePower);
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            LogsUtil.warn(getInstance().getClass().getSimpleName(), "订单修复 - 系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
        } else {
            long chargeTimeDivision = TimeUtil.division(chargeCardConsumeTime, chargeStandardItemEntity.billingInterval);
            chargeCardConsumeAmount = chargeTimeDivision * chargeStandardItemEntity.billingPrice;
        }

        //endregion

        Map<String, Object> set_data = new LinkedHashMap<>();
        set_data.put("chargeCardConsumeAmount", chargeCardConsumeAmount);

        if (orderEntity.update(orderEntity.id, set_data) > 0) {
            LogsUtil.info(getInstance().getClass().getSimpleName(), "订单修复 - id:%s [%s]已修复", orderEntity.id, orderEntity.OrderSN);
        } else {
            LogsUtil.info(getInstance().getClass().getSimpleName(), "充电订单修复 - id:%s [%s]修复失败", orderEntity.id, orderEntity.OrderSN);
        }
        return orderEntity.id;
    }

    /**
     * 调整功率
     * 2024-10-21 版本的收费，为了减少客诉量，调整显示给用户的功率
     *
     * @param power
     * @param powerRanges
     * @return
     */
    public static double adjustPower_20241021(double power, double[][] powerRanges) {
        // 定义功率档位
//        double[][] powerRanges = {
//                {0, 210},   // 档位1: 0W~210W
//                {210, 300}, // 档位2: 210W~300W
//                {300, 500}, // 档位3: 300W~500W
//                {500, 1000} // 档位4: 500W~1000W
//        };
        // 定义功率调整步长
        double dist = 15;
        double last_max = 0.0;
        // 遍历功率档位，找到对应的档位
        for (double[] range : powerRanges) {
            double min = range[0];
            double max = range[1];
            // 功率在当前档位范围内，直接返回，不做调整
            if (power > min && power <= max) {
                if (power - last_max > dist) {
                    return power;
                }
                return power + common.randomInt(15, 20);
            }
            last_max = max;
        }
        // 如果功率超出最高档位，直接返回原始功率
        return power;
    }

    // region 2024-10-21 以前的结算版本

    /**
     * 结算充电
     */
    public SyncResult chargeFinish_20241020(String OrderSN, int stopReasonCode, String stopReasonText) {
        return chargeFinish_20241020(OrderSN, TimeUtil.getTimestamp(), stopReasonCode, stopReasonText);
    }

    /**
     * 结算充电
     */
    public SyncResult chargeFinish_20241020(String OrderSN
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        String logMessage = "";
        logMessage += String.format("\033[1;94m 充电结算：OrderSN=%s stopReasonCode=%s stopReasonText=%s \033[0m", OrderSN, stopReasonCode, stopReasonText);
        LogsUtil.info(this.getClass().getSimpleName(), "%s", logMessage);

        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();

        //判断是否已经进行了软结算
        if (orderEntity.status == 2) {
            return new SyncResult(0, "已结算");
        }

        if (orderEntity.stopTime != 0) stopTime = orderEntity.stopTime;
        if (stopTime <= 0) stopTime = TimeUtil.getTimestamp();

        //region 查询设备数据

        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(orderEntity.deviceCode);
        if (deviceEntity == null || deviceEntity.id == 0) {
            LogsUtil.error(this.getClass().getSimpleName(), "查询设备数据出错,deviceCode=%s", orderEntity.deviceCode);
            return new SyncResult(1, "查询设备数据出错");
        }

        //endregion

        SyncResult r;
        //根据扣费模式进行结算
        EChargePaymentType ePaymentType = EChargePaymentType.valueOf(orderEntity.paymentTypeId);
        switch (ePaymentType) {
            case Balance:
                r = chargeFinishWithBalance_20241020(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
                break;
            case ChargeCard:
                r = chargeFinishWithChargeCard_20241020(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
                break;
            case Integral:
                r = chargeFinishWithIntegral_20241020(orderEntity, deviceEntity, stopTime, stopReasonCode, stopReasonText);
                break;
            default:
                return new SyncResult(11, "无效扣费方式");
        }

        //重新查询一系订单信息
        orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();

        //region 当出现充电过载的时候检查订单是否授权了自动重启

        if (orderEntity.overloadRestart == 1) {
            switch (stopReasonCode) {
                case 6://负载过大
                case 8://动态过载
                    SyncResult restartChargeResult = restartCharging(orderEntity);
                    if (restartChargeResult.code != 0) {
                        LogsUtil.warn(this.getClass().getSimpleName(), "充电重启 %s - 无法重启，%s", orderEntity.OrderSN, restartChargeResult.msg);

                        String title = "无法重启充电通知";
                        String message = "亲爱的用户，系统无法为您重启充电服务，原因：" + restartChargeResult.msg;
                        SysMessageEntity.getInstance().sendSysNotice(orderEntity.uid, title, message);
                    }
                    break;
            }
        }

        //endregion

        //根据充电订单进行分润
        int isShareProfit = SysGlobalConfigEntity.getInt("ChargeStation:ShareProfit", 1);
        if (orderEntity.paymentTypeId == 1 && isShareProfit == 1) {
            ChargeStationShareProfitEntity.getInstance().threadShareProfitCheckWithChargeOrder(orderEntity);
        }

        //占用费收费监控
        ParkingMonitorApp.add(orderEntity);

        //region 微信充电完成通知

        try {
            String PortText = "未知";
            DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance()
                    .cache(String.format("Device:%s:Port:%s:Socket", deviceEntity.id, orderEntity.port))
                    .where("deviceId", deviceEntity.id)
                    .where("port", orderEntity.port)
                    .findEntity();
            if (deviceSocketEntity != null && deviceSocketEntity.id > 0) {
                PortText = String.valueOf(deviceSocketEntity.index);

                deviceSocketEntity.where("deviceId", deviceEntity.id)
                        .where("port", orderEntity.port)
                        .update(new HashMap<>() {{
                            put("usePower", 0.0);
                        }});
            }

//            String ChargeStandardName = ChargeStationEntity.getInstance()
//                    .getChargeStandardNameWithDeviceCode(orderEntity.deviceCode);
            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance()
                    .where("id", orderEntity.CSId)
                    .findEntity();
            String ChargeStandardName = "";
            if (chargeStationEntity != null) {
                ChargeStandardName = chargeStationEntity.name;
            }

            String ChargeTimeText = TimeUtil.convertToFormatTime(orderEntity.chargeTime);
            switch (ePaymentType) {
                case Balance:
                    break;
                case ChargeCard:
                    ChargeTimeText = TimeUtil.convertToFormatTime(orderEntity.totalChargeTime);
                    break;
            }

            Map<String, Object> noticeData = new HashMap<>();
            noticeData.put("OrderSN", OrderSN);
            noticeData.put("ChargeStandardName", ChargeStandardName);
            noticeData.put("Port", PortText);
            noticeData.put("ChargeTime", ChargeTimeText);
            //2022/11/2 修复充电完成的微信通知，显示充电时间的小数点N位
            noticeData.put("totalAmount", String.format("%.2f", orderEntity.totalAmount));
            //2022/11/2 修改通知模板，添加停止原因
            noticeData.put("stopReasonText", stopReasonText);
            System.out.println(noticeData);
            wechatChargeDoneNotice(orderEntity.uid, noticeData);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "发送充电完成通知发生错误");
        }

        //endregion

        //发送用户站内通知
        SysMessageEntity.getInstance().sendChargeNoticeToUserWithStopReasonCode(stopReasonCode, orderEntity);

        //发送管理员Email通知
//        SysEmailEntity.getInstance().sendWarnToAdminWithStopReasonCode(stopReasonCode, orderEntity);

        return r;
    }

    /**
     * 使用余额结算
     *
     * @param orderEntity    订单实体类
     * @param deviceEntity   设备实体类
     * @param stopReasonCode 停止原因编码
     * @param stopReasonText 停止原因文本
     */
    private SyncResult chargeFinishWithBalance_20241020(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
            double totalAmount = 0.0;
            int payment_status = 0;//支付状态：0=未支付，1=已支付
            int status = 2;//状态,-1=错误，0=待启动，1=充电中，2=已完成
            String status_msg = "";

            //根据用户选择的充电功率进行查询收费配置
            double chargePower = orderEntity.limitChargePower;
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
                return new SyncResult(2, "系统错误：收费标准数据不正确");
            }

            /*
             * 2023-11-08 上线的收费模式
             * 收费标准：每30分钟收一次费用，不满30分钟，按30分钟收费。
             * 特殊情况：
             * 1、开始充电后10分钟停止充电免收费用。
             * 2、充电时长超过30分钟的整数倍的余数部分，如小于等于10分钟，则按10分钟收费。
             */
            totalAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);

            UserSummaryEntity userSummaryEntity = new UserSummaryEntity();

            //region 充电预扣费退款
            SyncResult esChargeRefundRefundResult = esChargeRefundTransaction(connection
                    , orderEntity.uid
                    , orderEntity.estimateAmount
                    , orderEntity.OrderSN);
            //记录一下日志
            if (esChargeRefundRefundResult.code != 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：返回用户充电预扣费失败 OrderSN=%s DeviceCode=%s msg=%s", orderEntity.OrderSN, deviceEntity.deviceCode, esChargeRefundRefundResult.msg);
            }
            //endregion

            //region 进行扣费操作

            //如果扣费金额为0可能是免结算时间内停止充电
            if (totalAmount == 0) {
                int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
                status = -1;
                status_msg = String.format("在%s秒内进行结算无需扣款", startChargeFreeTime);
                payment_status = 0;
                totalAmount = 0.0;
            } else {
                //检查此笔充电订单是否已经支付了
                if (orderEntity.payment_status == 1) {
                    LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：此笔充电订单用户已经支付过了，发生重复结算情况 OrderSN=%s DeviceCode=%s ORDER=%s", orderEntity.OrderSN, deviceEntity.deviceCode, JSONObject.toJSONString(orderEntity));
                } else {
                    //检查用户是否足够余额
                    double balance = userSummaryEntity.getBalanceDoubleWithUid(orderEntity.uid);
                    if (balance < totalAmount) {
                        payment_status = 0;//未支付
                        LogsUtil.error(this.getClass().getSimpleName(), "[%s][%s] - %s 充电结算失败：用户余额不足，当前余额 %s", orderEntity.OrderSN, deviceEntity.deviceCode, uid, balance);
                    } else {
                        //检查是否已经扣费了
                        if (!UserBalanceLogEntity.getInstance()
                                .where("type", EBalanceUpdateType.charge)
                                .where("orderSN", orderEntity.OrderSN)
                                .existTransaction(connection)) {
                            SyncResult payResult = userSummaryEntity.updateBalanceTransaction(connection
                                    , orderEntity.uid
                                    , -totalAmount
                                    , EBalanceUpdateType.charge
                                    , "充电扣费"
                                    , orderEntity.OrderSN
                            );
                            if (payResult.code != 0) {
                                LogsUtil.error(this.getClass().getSimpleName(), "充电结算失败：用户扣款失败 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                            } else payment_status = 1;//已支付
                        } else payment_status = 1;//已支付
                    }
                }
            }
            //endregion

            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", status);//状态,-1=错误，0=待启动，1=充电中，2=已完成
            set_data.put("status_msg", status_msg);
            set_data.put("payment_status", payment_status);//支付状态：0=未支付，1=已支付
            set_data.put("totalAmount", totalAmount);
            set_data.put("stopTime", stopTime);
            set_data.put("stopReasonCode", stopReasonCode);
            set_data.put("stopReasonText", stopReasonText);
            //充电柜标识：0-否，1-是
            if (orderEntity.is_cabinet == 1) {
                //充电柜使用，门状态：0-关闭，1-打开，-1-无
                set_data.put("door_status", 1);
            }

            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }
            return new SyncResult(0, "");
        });
        if (r.code == 0) {
            //新增充电次数和时间：计时直接计算用户选择的时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
            UserSummaryService.asyncUpdateUserChargeDataTask(orderEntity);
        }
        return r;
    }

    /**
     * 使用充电卡结算
     *
     * @param orderEntity    充电订单详情
     * @param deviceEntity   充电设备实体类
     * @param stopReasonCode 停止充电代码
     * @param stopReasonText 停止充电原因文本
     */
    private SyncResult chargeFinishWithChargeCard_20241020(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
            //根据充电功率计算充电卡的消耗倍数时间
            long chargeCardConsumeTime = Convert.toLong(orderEntity.totalChargeTime * orderEntity.chargeCardConsumeTimeRate);
            double chargeCardConsumeAmount = 0.0;

            //region 计算充电卡对应消耗的金额

            //根据用户选择的充电功率进行查询收费配置
            double chargePower = orderEntity.limitChargePower;
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
            } else {
                /*
                 * 2023-11-08 上线的收费模式
                 * 收费标准：每30分钟收一次费用，不满30分钟，按30分钟收费。
                 * 特殊情况：
                 * 1、开始充电后10分钟停止充电免收费用。
                 * 2、充电时长超过30分钟的整数倍的余数部分，如小于等于10分钟，则按10分钟收费。
                 */
                chargeCardConsumeAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);
            }
            //endregion

            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", 2);//状态,-1=错误，0=待启动，1=充电中，2=已完成
            set_data.put("payment_status", 1);//支付状态：0=未支付，1=已支付
            set_data.put("stopTime", stopTime);
            set_data.put("stopReasonCode", stopReasonCode);
            set_data.put("stopReasonText", stopReasonText);
            set_data.put("chargeCardConsumeTime", chargeCardConsumeTime);
            set_data.put("chargeCardConsumeAmount", chargeCardConsumeAmount);
            //充电柜标识：0-否，1-是
            if (orderEntity.is_cabinet == 1) {
                //充电柜使用，门状态：0-关闭，1-打开，-1-无
                set_data.put("door_status", 1);
            }
            if (orderEntity.update(orderEntity.id, set_data) == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }
            return new SyncResult(0, "");
        });
        if (r.code == 0) {
            //新增充电次数和时间：月卡计算实时充电时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
            UserSummaryService.asyncUpdateUserChargeDataTask(orderEntity);
        }
        return r;
    }

    /**
     * 使用积分结算
     *
     * @param orderEntity    订单实体类
     * @param deviceEntity   设备实体类
     * @param stopReasonCode 停止原因编码
     * @param stopReasonText 停止原因文本
     */
    private SyncResult chargeFinishWithIntegral_20241020(ChargeOrderEntity orderEntity
            , DeviceEntity deviceEntity
            , long stopTime
            , int stopReasonCode
            , String stopReasonText) {
        SyncResult r = ChargeOrderEntity.getInstance().beginTransaction(connection -> {
            double totalAmount = 0.0;
            int payment_status = 1;//支付状态：0=未支付，1=已支付
            int status = 2;//状态,-1=错误，0=待启动，1=充电中，2=已完成
            String status_msg = "";

            //根据用户选择的充电功率进行查询收费配置
            double chargePower = orderEntity.limitChargePower;
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance()
                    .getItemWithConfig(deviceEntity.chargeStandardConfigId, chargePower);
            if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "系统错误：收费标准数据不正确,configId=%s 充电功率=%s", deviceEntity.chargeStandardConfigId, chargePower);
                return new SyncResult(2, "系统错误：收费标准数据不正确");
            }

            //计算一下实际扣费是多少，用于统计参考
            totalAmount = EvChargeHelper.getInstance().billing_20231211(orderEntity, chargeStandardItemEntity, stopTime);

            //region 进行扣费操作

            //如果扣费金额为0可能是免结算时间内停止充电
//            if (totalAmount == 0) {
//                int startChargeFreeTime = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);
//                status = -1;
//                status_msg = String.format("在%s秒内进行结算无需扣款", startChargeFreeTime);
//                payment_status = 0;
//                totalAmount = 0.0;
//            }
            //endregion

            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("status", status);//状态,-1=错误，0=待启动，1=充电中，2=已完成
            set_data.put("status_msg", status_msg);
            set_data.put("payment_status", payment_status);//支付状态：0=未支付，1=已支付
            set_data.put("integralConsumeAmount", totalAmount);
            set_data.put("stopTime", stopTime);
            set_data.put("stopReasonCode", stopReasonCode);
            set_data.put("stopReasonText", stopReasonText);
            //充电柜标识：0-否，1-是
            if (orderEntity.is_cabinet == 1) {
                //充电柜使用，门状态：0-关闭，1-打开，-1-无
                set_data.put("door_status", 1);
            }

            if (orderEntity.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                LogsUtil.error(this.getClass().getSimpleName(), "充电结算错误：更新充电订单不成功 OrderSN=%s DeviceCode=%s", orderEntity.OrderSN, deviceEntity.deviceCode);
                return new SyncResult(1, "结算失败");
            }
            return new SyncResult(0, "");
        });
        if (r.code == 0) {
            //新增充电次数和时间：计时直接计算用户选择的时长
//            updateUserSummarySomeData(orderEntity.uid, orderEntity);
            UserSummaryService.asyncUpdateUserChargeDataTask(orderEntity);
        }
        return r;
    }

    // endregion
}
