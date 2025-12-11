package com.evcharge.utils;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.basedata.ChargeStandardItemEntity;
import com.evcharge.entity.basedata.ChargeTimeItemEntity;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.order.settlement.v2.ChargeSettlementForEBike_20241021;
import com.evcharge.entity.order.settlement.v2.IChargingSettlementV2;
import com.evcharge.entity.station.*;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.PrivateChargeStationUserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.entity.user.coupon.UserPlatformCouponEntity;
import com.evcharge.entity.user.coupon.UserThirdPartyCouponEntity;
import com.evcharge.entity.user.member.UserMemberEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EChargeMode;
import com.evcharge.enumdata.EChargeType;
import com.evcharge.enumdata.EChargePaymentType;
import com.evcharge.mqtt.XMQTTFactory;
import com.evcharge.service.User.UserMemberService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 电动车充电助手
 */
public class EvChargeHelper {

    //region 属性
    private final static String TAG = "电动自行车充电";

    /**
     * 用户ID
     */
    private long uid = 0;
    /**
     * 充电的设备
     */
    private String deviceCode;
    /**
     * 充电的端口
     */
    private int port = -1;
    /**
     * 充电时间项目Id
     */
    private long chargeTimeItemId = -1;
    private long chargeStandardItemId = -1;
    /**
     * 是否启动安全充电
     */
    private boolean safeCharge = false;
    /**
     * 限制最大充电功率，取值范围大于0或小于充电端口限制的最大功率（系统会进行检查并且限制）
     */
    private double limitChargePower = 210;
    /**
     * 检查是否有充电中的订单
     */
    private boolean checkChargingOrder = true;

    /**
     * 充电过载时重启
     */
    private boolean overloadRestart = false;

    /**
     * 父级订单号
     */
    private String parentOrderSN = "";

    /**
     * 支付类型
     */
    private EChargePaymentType paymentType;
    /**
     * 充电卡
     */
    private String cardNumber;

    /**
     * 2024-01-24新增
     * 折扣金额
     */
    private double discountAmount;

    /**
     * 2024-01-24新增
     * 是否残疾人
     */
    private int is_disabled_user;

    /**
     * 2024-08-16 新增
     */
    private long coupon_id;
    private final static IChargingSettlementV2 mChargingSettlementV2 = new ChargeSettlementForEBike_20241021();

    //endregion

    //region Builder

    /**
     * 用户ID
     */
    public EvChargeHelper setUid(long uid) {
        this.uid = uid;
        return this;
    }

    /**
     * 充电的设备
     */
    public EvChargeHelper setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
        return this;
    }

    /**
     * 充电的端口
     */
    public EvChargeHelper setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * 充电时间项目Id
     */
    public EvChargeHelper setChargeTimeItemId(long chargeTimeItemId) {
        this.chargeTimeItemId = chargeTimeItemId;
        return this;
    }

    /**
     * 充电收费标准项Id
     */
    public EvChargeHelper setChargeStandardItemId(long chargeStandardItemId) {
        this.chargeStandardItemId = chargeStandardItemId;
        return this;
    }

    /**
     * 是否启动安全充电
     */
    public EvChargeHelper setSafeCharge(boolean safeCharge) {
        this.safeCharge = safeCharge;
        return this;
    }

    /**
     * 限制最大充电功率，取值范围大于0或小于充电端口限制的最大功率（系统会进行检查并且限制）
     */
    public EvChargeHelper setLimitChargePower(double limitChargePower) {
        this.limitChargePower = limitChargePower;
        return this;
    }

    /**
     * 检查是否有充电中的订单
     */
    public EvChargeHelper setCheckChargingOrder(boolean isCheck) {
        this.checkChargingOrder = isCheck;
        return this;
    }

    /**
     * 充电过载时重启
     */
    public EvChargeHelper setOverloadRestart(boolean overloadRestart) {
        this.overloadRestart = overloadRestart;
        return this;
    }

    /**
     * 父级订单号
     */
    public EvChargeHelper setParentOrderSN(String parentOrderSN) {
        this.parentOrderSN = parentOrderSN;
        return this;
    }

    /**
     * 支付方式
     */
    public EvChargeHelper setPaymentType(EChargePaymentType ePaymentType) {
        this.paymentType = ePaymentType;
        return this;
    }

    /**
     * 充电卡卡号
     */
    public EvChargeHelper setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
        return this;
    }

    /**
     * 2024-01-24新增
     *
     * @param isDisabledUser
     * @return
     */
    public EvChargeHelper setIsDisabledUser(int isDisabledUser) {
        this.is_disabled_user = isDisabledUser;
        return this;
    }

    /**
     * 2024-08-16新增
     *
     * @param couponId 优惠券id
     * @return
     */
    public EvChargeHelper setCouponId(long couponId) {
        this.coupon_id = couponId;
        return this;
    }
    //endregion

    public static EvChargeHelper getInstance() {
        return new EvChargeHelper();
    }

    /**
     * 开始充电
     *
     * @return
     */
    public SyncResult start() {
        //region 检查传入参数
        if (!StringUtils.hasLength(this.deviceCode)) return new SyncResult(2, "请选择正确的插座");
        if (this.port == -1) return new SyncResult(2, "请选择正确的插座端口");
        if (paymentType != EChargePaymentType.ChargeCard && chargeTimeItemId == -1) {
            return new SyncResult(2, "请选择充电时长");
        }
        //endregion

        //查询设备
        DeviceEntity deviceEntity = DeviceEntity.getInstance().getWithDeviceCode(this.deviceCode, false);
        if (deviceEntity == null || deviceEntity.id == 0) {
            LogsUtil.error("", "无效的充电设备，请检设备是否注册了，deviceCode=%s port=%s", this.deviceCode, this.port);
            return new SyncResult(101, "无效的充电设备");
        }
        if ("".equals(deviceEntity.CSId) || "0".equals(deviceEntity.CSId)) {
            return new SyncResult(201, "充电桩即将上线，敬请期待！");
        }

        //查询设备端口
        DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance().getWithDeviceCode(this.deviceCode, port);
        if (deviceSocketEntity == null || deviceSocketEntity.id == 0) {
            LogsUtil.error("", "无效的充电端口，请检设备插座数据是否添加了，deviceCode=%s port=%s", this.deviceCode, this.port);
            return new SyncResult(102, "无效的充电端口");
        }

        //region 检测设备是否是当前平台的设备

        String platform_code = SysGlobalConfigEntity.getString("System:EvPlatform:Code");
        if (StringUtils.hasLength(deviceEntity.platform_code) && !platform_code.equalsIgnoreCase(deviceEntity.platform_code)) {
            return new SyncResult(20, "此充电桩不能在该平台使用");
        }

        //endregion

        //region 限制用户同时充电订单
        if (this.checkChargingOrder) {
            Map<String, Object> co_data = ChargeOrderEntity.getInstance().field("OrderSN").where("uid", uid).where("status", 1).order("id DESC").find();
            if (co_data != null && !co_data.isEmpty()) {
                return new SyncResult(300, "您目前有一笔订单正在充电中，请先完成充电再发起新的充电", co_data);
            }
        }
        //endregion

        //region 检查占用费收费是否有没有结算

        Map<String, Object> pk_data = ParkingOrderEntity.getInstance().field("OrderSN").where("uid", uid).where("payment_status", 0).order("id DESC").find();
        if (!pk_data.isEmpty()) return new SyncResult(310, "您有占用还没缴费，请先缴费", pk_data);

        //endregion

        ChargeTimeItemEntity chargeTimeItemEntity = null;
        if (this.chargeTimeItemId > 0) {
            chargeTimeItemEntity = ChargeTimeItemEntity.getInstance().getItemWithId(this.chargeTimeItemId);
            if (chargeTimeItemEntity == null || chargeTimeItemEntity.id == 0) {
                return new SyncResult(2, "请正确选择充电时长");
            }
        }

        //region 兼容：原设计没有chargeStandardItemId，现在可以通过功率判断并设置好
        if (this.chargeStandardItemId == -1) {
            ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance().getItemWithConfig(deviceEntity.chargeStandardConfigId, this.limitChargePower);
            if (chargeStandardItemEntity != null) this.chargeStandardItemId = chargeStandardItemEntity.id;
        }
        //endregion

        //region 检查状态：-1=下发命令检查中,0=空闲，1=充电中，2=有插座但未启动充电，3=已充满电，4=故障，5=浮充
        boolean EnableCheckPort = SysGlobalConfigEntity.getBool("Enable:CheckPort", false);
        if (EnableCheckPort) {
            int status = DataService.getMainCache().getInt(String.format("Device:%s:Port:%s:status", deviceCode, port), -1);
            if (status == -1) {
                deviceSocketEntity = DeviceSocketEntity.getInstance().where("deviceId", deviceEntity.id).where("port", this.port).findEntity();
                if (deviceSocketEntity == null || deviceSocketEntity.id == 0) {
                    LogsUtil.error("", "无效的充电端口，请检车设备插座数据是否添加了，deviceCode=%s port=%s", this.deviceCode, this.port);
                    return new SyncResult(102, "无效的充电端口");
                }
                status = deviceSocketEntity.status;
            }
            switch (status) {
                case 0://空闲状态
                    return new SyncResult(613, "请插入充电器");
                case 1://充电中
                    return new SyncResult(614, "此设备正在充电中");
                case 2://有插座但未启动充电
                    break;
                case 3://已充满电
                case 5://浮充
                    return new SyncResult(615, "此设备正在使用中，请更换其他设备");
                case 4://故障
                    return new SyncResult(616, "此插座发生故障，请使用其他的插座充电");
                default:
                    return new SyncResult(617, "暂时不允许充电");
            }
        }
        //endregion

        //region 检查此端口是否有充电订单正在进行中
        if (ChargeOrderEntity.getInstance().where("deviceCode", deviceCode).where("port", port).where("status", 1).exist()) {
            return new SyncResult(614, "此设备正在使用中...");
        }
        //endregion

        int ChargeMode;//默认计时

        ChargeOrderEntity orderEntity = new ChargeOrderEntity();
        ChargeOrderExDataEntity orderExDataEntity = new ChargeOrderExDataEntity();

        orderEntity.uid = uid;
        orderEntity.status = 0;//状态,0=待启动，1=充电中，2=已完成
        orderEntity.deviceCode = this.deviceCode;
        orderEntity.port = port;
        orderEntity.CSId = deviceEntity.CSId;

        //查询充电桩信息
        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(this.deviceCode);
        if (chargeStationEntity == null) return new SyncResult(201, "充电桩即将上线，敬请期待！");

        //检查是否访问受限制的，如果是则需要授权的用户才能使用
        if (chargeStationEntity.is_restricted == 1) {
            if (!PrivateChargeStationUserEntity.getInstance().checkAuth(chargeStationEntity.id, uid)) {
                return new SyncResult(620, "本充电桩不对外开放使用，请查看[元气充电]小程序地图附近站点，敬请谅解！");
            }
        }

        long nowTime = TimeUtil.getTimestamp();
        double balance = 0.0;//计时的时候，用户的余额
        long chargeTime = 0;//充电时长，单位秒
        double estimateAmount = 0.0;//预计金额
        int esIntegral = 0;//预计积分金额
        double esIntegralConsumeAmount = 0;//预计积分消耗的金额（用于统计计费）

        long expireTimestamp = 0;//包月时的过期时间
        long chargeCardId = 0;//充电卡ID
        boolean chargeAutoStop = false;//是否充满自停
        double safeChargeFee = 0.0;//安心充电费用
        //2024-01-19 新增折扣金额字段
        double discountPrice = 0;
        //region 根据用户习惯获得一个充电功率限制值
        // 2023-5-22 新增功能：由前端用户选择限制功率充电，当 user_ebike_id == 0 则以 limitChargePower 作为最大充电功率进行充电
//        if (userEbikeId != 0) {
//            limitChargePower = orderEntity.getLimitChargePowerWithUserHabit2(uid, userEbikeId, deviceEntity, port);
//        }

        limitChargePower = Math.abs(limitChargePower);
        if (limitChargePower == 0) {
            limitChargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 210);
        }
        //限制功率不能大于端口设定的功率
        if (limitChargePower > deviceSocketEntity.limitChargePower) {
            limitChargePower = deviceSocketEntity.limitChargePower;
        }
//        //使用阶梯式充电时，下发的限制功率默认安全充电功率210W开始
//        if (overloadRestart) limitChargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 210);

        //endregion

        //region 查询收费标准
        if (deviceEntity.chargeStandardConfigId == 0) return new SyncResult(630, "设备收费标准配置错误");
        ChargeStandardItemEntity chargeStandardItemEntity = ChargeStandardItemEntity.getInstance().getItemWithConfig(deviceEntity.chargeStandardConfigId, limitChargePower);
        if (chargeStandardItemEntity == null || chargeStandardItemEntity.id == 0) {
            return new SyncResult(631, "系统错误：收费标准数据不正确");
        }
        if (overloadRestart) limitChargePower = chargeStandardItemEntity.maxPower;
        //endregion

        long userMemberVIPLevelId = 0;
        // region remark - 读取用户VIP等级
        UserMemberService userMemberService = new UserMemberService();
        if (userMemberService.checkUserIsMember(uid)) {
            UserMemberEntity userMemberEntity = userMemberService.getUserMember(this.uid);
            if (userMemberEntity != null) userMemberVIPLevelId = userMemberEntity.level_id;
        }
        // endregion

        /*
         * 订单号（32位纯数字）
         * 订单号生产规则参考：
         * 前面14位是年月日时分秒
         * 中间8位是随机数
         * 后面10位是10进制设备号+端口号 如：
         * 20200615113914002978611105276501
         */
        String OrderSN = String.format("%s%s%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomInt(10000000, 99999999), common.randomInt(10000000, 99999999), common.randomInt(10, 99));

        orderEntity.paymentTypeId = paymentType.index;

        //region 使用余额充电
        if (paymentType == EChargePaymentType.Balance) {
            ChargeMode = EChargeMode.HOUR;//默认计时
            //判断是否充满自停
            if (chargeTimeItemEntity != null && chargeTimeItemEntity.id > 0) {
                chargeTime = chargeTimeItemEntity.chargeTime;
                if (chargeTimeItemEntity.chargeAutoStop == 1) chargeAutoStop = true;
            }
            // 2025-04-10 新增逻辑，主要用于实体卡刷卡时调用余额充电时设定充满自停
            if (chargeTimeItemEntity == null && chargeTimeItemId == 0) {
                chargeTime = 28800; // 按照8小时下发指令
                chargeAutoStop = true;
            }

            // 总计费
            double totalAmount = 0.0;
            //region 2024-10-21 以前的预扣费版本：检查用户余额是否足够：预判 ，充电前预扣费主要是担心用户余额不够结算，同时担心用户帮多台车充电时不够费用
//            double totalAmount = 0.0;
//            //根据设备绑定的充电收费标准和限制充电功率来确定预扣费用：一般情况如果用户不添加车辆信息limitChargePower=210来限制充电
//            totalAmount = TimeUtil.convertToHourFull(chargeTime) * chargeStandardItemEntity.price; //chargeTime单位秒，需要转成小时
            // endregion

            // region 2024-10-21 收费版本：预计扣费

            totalAmount = EvChargeHelper.estimateBillingAmount(chargeStandardItemEntity, chargeTime);

            // endregion

            //region 2024-01-19新增功能 判断用户是否残疾人，给予对应折扣
//            if (this.is_disabled_user == 1) {
//                //判断用户等级 //判断折扣率
//                UserEntity userEntity = UserEntity.getInstance().findUserByUid(uid);
//                DisabledUsersEntity disabledUsersEntity = DisabledUsersEntity.getInstance().getInfoByInfo(userEntity.phone);
//                if (disabledUsersEntity != null) {
//                    DisabledLevelEntity disabledLevelEntity = DisabledLevelEntity.getInstance().getInfo(disabledUsersEntity.level_id);
//                    discountPrice = totalAmount * disabledLevelEntity.charge_discount_rate;
//                    totalAmount = totalAmount - discountPrice;
//                }
//            }
            //endregion

            // region 2024-08-16新增优惠券功能，目前支持满减券
            // 获取优惠券信息 判断最终费用
            if (this.coupon_id > 0) {
                Map<String, Object> couponInfo = UserPlatformCouponEntity.getInstance().getCouponInfoById(this.uid, this.coupon_id);
                if (couponInfo == null) {
                    return new SyncResult(1, "优惠券信息不存在");
                }
                double amount = MapUtil.getDouble(couponInfo, "amount");
                double amountFactor = MapUtil.getDouble(couponInfo, "discount_factor");
                double amountUseStint = MapUtil.getDouble(couponInfo, "amount_use_stint");
                if (amountUseStint == 1) {
                    if (totalAmount < amountFactor) {
                        return new SyncResult(1, String.format("该优惠券需满%s元使用，当前总费用为%s元。", amountFactor, totalAmount));
                    }
                    discountPrice = amount;
                    totalAmount = totalAmount - amount;
                } else {
                    discountPrice = amount;
                    totalAmount = totalAmount - amount;
                    if (totalAmount < 0) {
                        totalAmount = 0;
                    }
                }
                System.out.printf("使用优惠券充电,充电订单:%s,优惠券ID:%s,优惠金额:%s,充电金额:%s%n", OrderSN, this.coupon_id, discountPrice, totalAmount);
                UserPlatformCouponEntity.getInstance().consume(this.coupon_id, OrderSN);
                //2024-10-10 新增核销
                UserThirdPartyCouponEntity.getInstance().yiDongWriteOffCouponById(this.coupon_id);
            }
            //endregion

            // region remark - 安心充服务逻辑（0=不启用，1=启用）
            if (this.safeCharge && deviceEntity.safeCharge == 1) {
                if (userMemberVIPLevelId > 0) {
                    // 2025-10-22：只要用户是VIP，不管使用余额扣费还是充电卡，默认启动安心充服务并且不进行扣费
                    this.safeCharge = true;
                    safeChargeFee = 0;
                } else {
                    // 默认进行扣费（若存在父单则进一步判断父链是否已扣过）
                    boolean shouldDeductSafeCharge = true;
                    if (StringUtil.isNotEmpty(this.parentOrderSN)) {
                        shouldDeductSafeCharge = shouldDeductSafeChargeFee(this.parentOrderSN);
                    }

                    if (shouldDeductSafeCharge) {
                        final double fee = deviceEntity.safeChargeFee;
                        if (fee > 0) {
                            safeChargeFee = fee;
                            totalAmount = totalAmount + fee;
                        }
                    }
                }
            }
            // endregion

            estimateAmount = totalAmount;

            balance = UserSummaryEntity.getInstance().getBalanceDoubleWithUid(uid);
            if (balance < estimateAmount) return new SyncResult(632, "余额不足");
        }
        //endregion

        //region 使用充电卡充电
        else if (paymentType == EChargePaymentType.ChargeCard) {
            ChargeMode = EChargeMode.MONTH;//包月
            chargeAutoStop = true;

            //region 判断用户的月卡是否适用
            //查询充电卡信息
            UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance().getCardWithNumber(cardNumber);
            if (userChargeCardEntity == null || userChargeCardEntity.id == 0)
                return new SyncResult(203, "请选择充电卡");
            //判断充电卡是否属于用户的
            if (userChargeCardEntity.uid != uid) return new SyncResult(204, "此充电卡不属于您");

            if (userChargeCardEntity.start_time > nowTime || userChargeCardEntity.end_time < nowTime) {
                return new SyncResult(205, "已失效");
            }
            //检查充电在此充电桩是否可用
            SyncResult cardCheckResult = userChargeCardEntity.checkChargeCardForStation(chargeStationEntity, userChargeCardEntity);
            if (cardCheckResult.code != 0) {
                LogsUtil.warn(TAG, "[%s - %s] - 充电卡不可用：%s", userChargeCardEntity.uid, userChargeCardEntity.cardNumber, cardCheckResult.msg);
                return new SyncResult(206, "充电卡不可用");
            }

            //实际剩余充电时间
            long chargeTimeBalance = userChargeCardEntity.getTodayCardTimeBalance(userChargeCardEntity, true);
            //根据收费标准和功率计算出相应消耗倍率的充电时间
            long todayChargeTimeBalance = Convert.toLong(chargeTimeBalance / chargeStandardItemEntity.chargeCardConsumeTimeRate);
            if (todayChargeTimeBalance <= 0) return new SyncResult(622, "您今天的充电时长已达最大值，无法启动充电");
            //计算用户今日还有多少充电时长，单位秒
            chargeTime = todayChargeTimeBalance;

            //设置用户的到期时间
            expireTimestamp = userChargeCardEntity.end_time;

            //region 允许用户充电卡跨天充电
            long todayEndTime = TimeUtil.getTime24();
            orderEntity.endTime = nowTime + chargeTime * ECacheTime.SECOND;//转化成毫秒级
            //判断预设充电结束时间是否大于等于今天结束时间
            if (orderEntity.endTime >= todayEndTime) {
                //预设充电结束时间跨天了，允许用户消耗第二天的充电卡时间
                ChargeCardConfigEntity chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithCode(userChargeCardEntity.spu_code);
                // TODO 兼容旧卡，添加充电卡时可能还没使用spu_code，需要统一时间来修改
                if (chargeCardConfigEntity == null || chargeCardConfigEntity.id == 0) {
                    chargeCardConfigEntity = ChargeCardConfigEntity.getInstance().getConfigWithId(userChargeCardEntity.cardConfigId);
                }
                long tomorrowChargeTimeBalance = Convert.toLong(chargeCardConfigEntity.dailyChargeTime / chargeStandardItemEntity.chargeCardConsumeTimeRate);

                //重新计算充电时间：今日结束时间 - 现在时 + 明天的可用充电卡时间
                chargeTime = (todayEndTime - nowTime) / ECacheTime.SECOND; //转化为单位秒
                chargeTime += tomorrowChargeTimeBalance;
                orderEntity.endTime = nowTime + chargeTime * ECacheTime.SECOND;//转化成毫秒级
            }
            //endregion

            //检查预设充电结束时间是否小于用户充电卡到期时间
            if (orderEntity.endTime >= userChargeCardEntity.end_time) {
                //表示用户的充电卡到期了
                chargeTime = (userChargeCardEntity.end_time - nowTime) / ECacheTime.SECOND; //转化为单位秒
            }

            orderEntity.cardNumber = cardNumber;


            //endregion

            // region remark - 安心充服务逻辑（0=不启用，1=启用）
            if (this.safeCharge && deviceEntity.safeCharge == 1) {
                if (userMemberVIPLevelId > 0) {
                    // 2025-10-22：只要用户是VIP，不管使用余额扣费还是充电卡，默认启动安心充服务并且不进行扣费
                    this.safeCharge = true;
                    safeChargeFee = 0;
                } else {
                    // 默认进行扣费（若存在父单则进一步判断父链是否已扣过）
                    boolean shouldDeductSafeCharge = true;
                    if (StringUtil.isNotEmpty(this.parentOrderSN)) {
                        shouldDeductSafeCharge = shouldDeductSafeChargeFee(this.parentOrderSN);
                    }

                    // 若判定需要扣费，则执行余额校验
                    if (shouldDeductSafeCharge) {
                        safeChargeFee = deviceEntity.safeChargeFee;
                        balance = UserSummaryEntity.getInstance().getBalanceDoubleWithUid(uid);

                        if (balance < safeChargeFee) return new SyncResult(642, "余额不足，无法购买安心充服务");
                    }
                }
            }
            // endregion
        }
        //endregion

        //region 使用积分充电
        else if (paymentType == EChargePaymentType.Integral) {
            ChargeMode = EChargeMode.HOUR;//默认计时
            //判断是否充满自停
            if (chargeTimeItemEntity != null && chargeTimeItemEntity.id > 0) {
                chargeTime = chargeTimeItemEntity.chargeTime;
                if (chargeTimeItemEntity.chargeAutoStop == 1) chargeAutoStop = true;
            }

            double totalAmount = 0.0;
            //根据设备绑定的充电收费标准和限制充电功率来确定预扣费用：一般情况如果用户不添加车辆信息limitChargePower=210来限制充电
            //chargeTime单位秒，需要转成小时
            totalAmount = TimeUtil.convertToHourFull(chargeTime) * chargeStandardItemEntity.price;
            esIntegralConsumeAmount = totalAmount;
            // 计算预计积分扣费,人民币:积分 = 1:10
            esIntegral = Math.round(Convert.toFloat(totalAmount * 10));

            // 检查用户积分是否足够
            int integralBalance = UserSummaryEntity.getInstance().getIntegralWithUid(this.uid);
            if (integralBalance < esIntegral) return new SyncResult(633, "积分不足");
        }
        //endregion

        else return new SyncResult(640, "无效扣费方式");

        orderEntity.OrderSN = OrderSN;
        orderEntity.chargeType = EChargeType.PaymentBeforeUse;//先付后充
        orderEntity.chargeMode = ChargeMode;
        orderEntity.chargeTime = chargeTime;
        orderEntity.chargeCardId = chargeCardId;
        orderEntity.chargeTimeItemId = chargeTimeItemId;
        orderEntity.chargeStandardItemId = chargeStandardItemId;
        orderEntity.electricityFeePrice = chargeStandardItemEntity.electricityFeePrice; // 电费单价
        orderEntity.serviceFeePrice = chargeStandardItemEntity.serviceFeePrice; //服务费单价
        orderEntity.chargeAutoStop = chargeAutoStop ? 1 : 0;//充满自停标识，0=否，1=是

        //安全充电保险，0=不启用，1=启用
        if ((this.safeCharge || userMemberVIPLevelId > 0) && deviceEntity.safeCharge == 1) {
            orderEntity.safeCharge = 1;
            orderEntity.safeChargeFee = safeChargeFee;

            orderExDataEntity.is_safe_charge = 1;
            orderExDataEntity.safe_charge_fee = BigDecimal.valueOf(safeChargeFee);
        }


        if (limitChargePower <= 0) limitChargePower = 210;

        //预计扣费，充电回调后开始扣除费用
        orderEntity.estimateAmount = estimateAmount;
        //实际扣费，结算时进行计算
        orderEntity.totalAmount = 0;

        //预计积分扣费，充电回调后开始扣除费用
        orderEntity.esIntegral = esIntegral;
        orderEntity.esIntegralConsumeAmount = esIntegralConsumeAmount;
        //实际积分扣费，结算时进行计算
        orderEntity.integral = 0;

        orderEntity.limitChargePower = this.limitChargePower; //记录此次充电的最大功率
        orderEntity.startTime = nowTime;
        orderEntity.endTime = nowTime + chargeTime * ECacheTime.SECOND;

        orderEntity.meterId = deviceEntity.meterId;
        orderEntity.chargeCardConsumeTimeRate = chargeStandardItemEntity.chargeCardConsumeTimeRate;
        orderEntity.create_time = nowTime;
        orderEntity.version = 2;
        orderEntity.overloadRestart = this.overloadRestart ? 1 : 0;
        orderEntity.parentOrderSN = this.parentOrderSN;
//        orderEntity.userEbikeId = this.userEbikeId;//用户需要充电的车辆信息(代删除)

        //判断是否为充电柜
        if (deviceEntity.is_cabinet == 1) {
            orderEntity.is_cabinet = deviceEntity.is_cabinet;
            orderEntity.door_status = 0;
        }
        //判断其他类型

        //region 2024-01-19 写入折扣金额和标记是否残疾人字段
        orderEntity.discountAmount = BigDecimal.valueOf(discountPrice);
        orderEntity.is_disabled_user = this.is_disabled_user;
        //endregion
        orderEntity.id = orderEntity.insert();
        if (orderEntity.id <= 0) return new SyncResult(10, "创建充电订单失败");

        // region remark - 订单扩展数据
        orderExDataEntity.orderSN = OrderSN;
        orderExDataEntity.uid = uid;
        orderExDataEntity.user_member_level_id = userMemberVIPLevelId;
        orderExDataEntity.create_time = nowTime;
        orderExDataEntity.update_time = nowTime;
        orderExDataEntity.insert();
        // endregion

        // region 2024-10-21 收费版本：(废除) 使用余额充电并且自动识别功率时 下发1000W
//        if (this.overloadRestart && ChargeMode == EChargeMode.HOUR) {
//            this.limitChargePower = SysGlobalConfigEntity.getDouble("Default:ChargeMaxPower", 1000);
//        }
        // endregion

        //region 利用MOTT下发命令
        JSONObject json = new JSONObject();
        json.put("deviceCode", this.deviceCode);
        json.put("ChargeMode", ChargeMode);//充电模式
        json.put("port", this.port);//端口
        json.put("OrderSN", OrderSN);
        json.put("expireTimestamp", expireTimestamp);//包月才有用

        json.put("moneyBalance", balance);//计时才有用
        json.put("chargeAutoStop", chargeAutoStop ? 1 : 0);//充满自停标识，0=否，1=是
        json.put("chargeTime", orderEntity.chargeTime); //设备限制最大时长为18小时,单位：秒
        json.put("maxPower", this.limitChargePower);//最大充电功率

        /*
         *  关于内部程序通信的主题定义：
         *  订阅（平台-->推送-->中转站）：{应用通道}/{设备编号}/command/业务逻辑函数名
         *  推送（中转站-->推送-->平台）：{平台代码}/{应用通道}/{设备编号}/业务逻辑函数名
         */
        XMQTTFactory.getInstance().publish(String.format("%s/%s/command/startCharge", deviceEntity.appChannelCode, deviceEntity.deviceCode), json, 1);

        //endregion

        Map<String, Object> cbdata = new LinkedHashMap<>();
        cbdata.put("orderSN", orderEntity.OrderSN);
        cbdata.put("deviceCode", deviceCode);
        cbdata.put("port", port);

        return new SyncResult(0, "", cbdata);
    }

    /**
     * 检查父链是否已经发生过“安心充”扣费（性能优化版）。
     * <p>
     * 业务规则：
     * 1. 若父链中任意订单满足以下条件：
     * - safeCharge == 1（开启安心充）
     * - status == 2（已完成）
     * - safeChargeFee > 0（已发生扣费）
     * 则视为该链已扣过安心充费用，当前订单无需再次扣费，函数返回 false。
     * <p>
     * 2. 若遍历完整条父链未发现上述情况，则说明尚未扣过安心充费用，
     * 函数返回 true（表示可以扣费；调用方应再结合当前订单是否开启安心充判断）。
     * <p>
     * 参数说明：
     *
     * @param startOrderSN 从该父级订单号开始向上回溯检测
     *                     <p>
     *                     返回值说明：
     * @return true  表示父链尚未扣过安心充（可扣费）
     *         false 表示父链中已存在已扣费的安心充订单（不要再扣）
     * <p>
     * 其他说明：
     * - 最大遍历层数 MAX_HOPS = 5，用于防止异常循环或过深链路。
     * - 检测过程中如遇数据缺失、异常、环路等情况，均按保守策略返回 false。
     * - 查询仅选取必要字段 (OrderSN, safeCharge, safeChargeFee, status, parentOrderSN) 以提升性能。
     */
    private static boolean shouldDeductSafeChargeFee(String startOrderSN) {
        if (StringUtil.isEmpty(startOrderSN)) return false;

        // ===============================
        // 初始化保护与环境配置
        // ===============================
        final int MAX_HOPS = 5; // 最大检测层数，防止异常链路过深
        final java.util.HashSet<String> seen = new java.util.HashSet<>(16); // 用于检测环路，防止死循环
        final ChargeOrderEntity dao = ChargeOrderEntity.getInstance(); // 预先获取 DAO 实例，避免循环内重复初始化

        String currentSN = startOrderSN; // 当前正在检测的订单号
        int hops = 0;                    // 当前递进层级（防止无限追溯）

        try {
            // ===============================
            // 从起始订单开始，逐级向上回溯父级订单
            // ===============================
            while (StringUtil.isNotEmpty(currentSN)) {

                // ---- 1. 检查是否出现环路（例如数据链错误导致父子互指） ----
                if (!seen.add(currentSN)) {
                    LogsUtil.warn(TAG, "安心充扣费检查-检测到环: startSN=%s, currentSN=%s, hops=%d", startOrderSN, currentSN, hops);
                    return false; // 发现循环，保守处理：不扣费
                }

                // ---- 2. 检查是否超过最大回溯层数 ----
                if (++hops > MAX_HOPS) {
                    LogsUtil.warn(TAG, "安心充扣费检查-父链过深中止: startSN=%s, lastSN=%s, hops=%d", startOrderSN, currentSN, hops);
                    return false; // 超出上限，保守处理：不扣费
                }

                // ---- 3. 查询当前父级订单的必要字段（仅取关键字段以提高性能） ----
                Map<String, Object> row = dao.field("OrderSN,safeCharge,safeChargeFee,status,parentOrderSN").where("OrderSN", currentSN).find();

                // ---- 4. 若未查到父级订单，说明链条中断，直接中止检测 ----
                if (row == null || row.isEmpty()) {
                    LogsUtil.warn(TAG, "安心充扣费检查-父单不存在: startSN=%s, missingSN=%s, hops=%d", startOrderSN, currentSN, hops);
                    return false; // 保守处理：不扣费
                }

                // ---- 5. 读取核心字段 ----
                int safeCharge = MapUtil.getInt(row, "safeCharge");        // 是否开启安心充
                double safeChargeFee = MapUtil.getDouble(row, "safeChargeFee"); // 扣费金额
                int status = MapUtil.getInt(row, "status");                // 订单状态（2=已完成）

                // ---- 6. 业务判断：是否已存在扣费记录 ----
                // 满足 (safeCharge==1 && status==2 && safeChargeFee>0) 则说明该父单已扣过安心充
                // 一旦命中即停止遍历并返回 false（无需再扣）
                if (safeCharge == 1 && status == 2 && safeChargeFee > 0) {
                    return false;
                }

                // ---- 7. 未命中则继续向上追溯父级订单 ----
                currentSN = MapUtil.getString(row, "parentOrderSN");
            }

        } catch (Exception e) {
            // ===============================
            // 异常处理：任何异常视为不安全，保守返回 false
            // ===============================
            LogsUtil.error(e, TAG, "安心充扣费检查-异常: startSN=%s, currentSN=%s, hops=%d, ex=%s", startOrderSN, currentSN, hops, e.toString());
            return false;
        }

        // ===============================
        // 若能完整走到链顶且未发现扣费记录，则认为可扣费
        // ===============================
        return true;
    }

    /**
     * 充电预计扣费
     *
     * @param chargeStandardItemEntity
     * @param chargeTime_second
     * @return
     */
    public static double estimateBillingAmount(@NonNull ChargeStandardItemEntity chargeStandardItemEntity, long chargeTime_second) {
        BigDecimal estimateBillingAmount = mChargingSettlementV2.estimateBillingAmount(chargeStandardItemEntity, chargeStandardItemEntity.maxPower, chargeTime_second);
        return estimateBillingAmount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 2023-12-11 版本：计算收费 - 主要更改充电时间的统计方式，利用设备的充电时间进行结算
     *
     * @param orderEntity              充电订单实体类
     * @param chargeStandardItemEntity 充电收费标准实体类
     * @param stopTime                 充电停止时间
     * @return
     */
    public double billing_20231211(ChargeOrderEntity orderEntity, ChargeStandardItemEntity chargeStandardItemEntity, long stopTime) {
        BigDecimal totalAmount = new BigDecimal(0);

        // 使用从收费标准实体中获取的计费周期
        int billingInterval = 30 * 60; //30分钟的秒数

        //30分钟收费间隔的单价
        BigDecimal billing30mPrice = new BigDecimal(chargeStandardItemEntity.price / 2);
        //10分钟收费间隔的单价
        BigDecimal billing10mPrice = new BigDecimal(chargeStandardItemEntity.price / 6);

        // 特殊情况1：开始充电后10分钟内停止充电，免收费用
        int freeChargeTimeLimit = SysGlobalConfigEntity.getInt("StartCharge:FreeTime", 600);

        // 特殊情况2：如果充电时间超过30分钟的整数倍的余数部分小于等于10分钟，则余数部分按照十分钟收费
        int chargeOverTimeLimit = 600; // 10分钟的秒数

        if (stopTime == 0 || (stopTime - orderEntity.endTime) > ECacheTime.HOUR) {
            stopTime = orderEntity.endTime;
            ChargeOrderHeartbeatEntity heartbeatEntity = ChargeOrderHeartbeatEntity.getInstance().where("order_id", orderEntity.id).order("create_time DESC").findEntity();
            if (heartbeatEntity != null && heartbeatEntity.id != 0) stopTime = heartbeatEntity.create_time;
        }

        //实际充电时间
//        long actualChargeTime = (stopTime - orderEntity.startTime) / 1000;
        //2023-12-11 更新：以设备的充电时间统计为准，因为结算指令可能会延迟
        long actualChargeTime = orderEntity.totalChargeTime;

        //实际充电时间 <= 免费充电时间 则不收费
        if (actualChargeTime <= freeChargeTimeLimit) return 0;

        //检查 - 结束时间 - 如果充电时间超过30分钟的整数倍的余数部分小于等于10分钟，则余数部分按照十分钟收费
        long overTime = actualChargeTime % (billingInterval);
        if (overTime > 0 && overTime <= chargeOverTimeLimit) {
            //移除多余出来的秒数
            actualChargeTime -= overTime;
            // 如果额外时间不超过10分钟，则按10分钟计费
            totalAmount = totalAmount.add(billing10mPrice);
        }

        long chargeTimeDivision = (long) Math.ceil((double) actualChargeTime / billingInterval);
        totalAmount = totalAmount.add(new BigDecimal(chargeTimeDivision).multiply(billing30mPrice));
        totalAmount = totalAmount.add(new BigDecimal(orderEntity.safeChargeFee));

        if (orderEntity.estimateAmount > 0 && totalAmount.compareTo(new BigDecimal(orderEntity.estimateAmount)) > 0) {
            totalAmount = new BigDecimal(orderEntity.estimateAmount);
        }

        return totalAmount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
