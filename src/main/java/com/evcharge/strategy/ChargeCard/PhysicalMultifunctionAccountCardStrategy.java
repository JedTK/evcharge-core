package com.evcharge.strategy.ChargeCard;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.device.DeviceSocketEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserEbikeEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.ECacheTime;
import com.evcharge.enumdata.EChargeMode;
import com.evcharge.enumdata.EChargePaymentType;
import com.evcharge.utils.EvChargeHelper;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多功能账户实体卡 能力策略
 * 这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
 */
public class PhysicalMultifunctionAccountCardStrategy extends BaseChargeCardStrategy {
    private final static String TAG = "多功能账户实体卡策略";

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
        String CSId = JsonUtil.getString(trans_data, "CSId");

        Map<String, Object> data = new LinkedHashMap<>();

        // 检查用户是否存在 时效数字充电卡
        List<Map<String, Object>> cardList = UserChargeCardEntity.getInstance().getCardList(cardEntity.uid, CSId, "1");
        for (Map<String, Object> cardMap : cardList) {
            int allow = MapUtil.getInt(cardMap, "allow");
            // 是否允许使用
            if (allow == 0) continue;

            // 有 时效数字充电卡 则使用时效数字充电卡来启动充电
            data.put("ChargeMode", EChargeMode.MONTH);
            data.put("moneyBalance", 0); // 当充电卡为余额卡时使用
            data.put("expireTimestamp", MapUtil.getLong(cardMap, "end_time"));
            return new SyncResult(0, "", data);
        }

        // 检查用户余额
        double balance = UserSummaryEntity.getInstance().getBalanceDoubleWithUid(cardEntity.uid);

        data.put("ChargeMode", EChargeMode.HOUR);
        data.put("moneyBalance", balance); // 当充电卡为余额卡时使用
        data.put("expireTimestamp", 0);
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
     * 刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 是否成功执行能力
     */
    @Override
    public ISyncResult executeChargingHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        String CSId = JsonUtil.getString(trans_data, "CSId");
        String deviceCode = JsonUtil.getString(trans_data, "deviceCode");
        int port = JsonUtil.getInt(trans_data, "port");

        boolean canUseChargeCard = false;
        EvChargeHelper helper = EvChargeHelper.getInstance()
                .setUid(cardEntity.uid)
                .setDeviceCode(deviceCode)
                .setPort(port)
                .setSafeCharge(false)
                .setLimitChargePower(SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 210)) // 默认最小功率启动
                .setOverloadRestart(true); // 默认过载重启订单

        ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithDeviceCode(deviceEntity.deviceCode);

        // 检查用户是否存在 时效数字充电卡
        List<Map<String, Object>> cardList = UserChargeCardEntity.getInstance().getCardList(cardEntity.uid, CSId, "1");
        for (Map<String, Object> cardMap : cardList) {
            int allow = MapUtil.getInt(cardMap, "allow");
            if (allow == 0) continue;
            String tempCardNumber = MapUtil.getString(cardMap, "cardNumber");
            UserChargeCardEntity tempCardEntity = UserChargeCardEntity.getInstance().getCardWithNumber(tempCardNumber);

            //检查充电在此充电桩是否可用
            if (tempCardEntity == null) continue;
            if (chargeStationEntity != null) {
                SyncResult cardCheckResult = tempCardEntity.checkChargeCardForStation(chargeStationEntity, tempCardEntity);
                if (cardCheckResult.code != 0) continue;
            }

            // 判断充电时间是否少于10分钟，如果少于10分钟就不再使用
//            long chargeTimeBalance = tempCardEntity.getTodayCardTimeBalance(tempCardEntity, true);
//            if (chargeTimeBalance <= ECacheTime.MINUTE * 10) continue;

            // 有 时效数字充电卡 则使用时效数字充电卡来启动充电
            helper.setCardNumber(MapUtil.getString(cardMap, "cardNumber"))
                    .setPaymentType(EChargePaymentType.ChargeCard)
            ;
            canUseChargeCard = true;
            break;
        }

        // 检查用户余额
        if (!canUseChargeCard) {
            helper.setPaymentType(EChargePaymentType.Balance)
                    .setChargeTimeItemId(12) // 展示默认使用8小时充满自停项
//                    .setChargeTimeItemId(0) // 2025-04-10 新增逻辑如果充电时长没设置，则按8小时设定并启动充满自停，按照8小时预计扣费
            ;
        }

        // region 获取最后充电功率
        Map<String, Object> order_data = ChargeOrderEntity.getInstance()
                .field("OrderSN,maxPower")
                .where("uid", cardEntity.uid)
                .where("status", "<>", 0)
                .order("id DESC")
                .find();
        if (!order_data.isEmpty()) {
            double maxPower = MapUtil.getDouble(order_data, "maxPower");
            //获取用户记录的充电器最大充电功率
            double userChargerMaxPower = UserEbikeEntity.getInstance().getChargerMaxPowerWithUid(cardEntity.uid);
            //获取系统默认安全充电功率
            double safeChargePower = SysGlobalConfigEntity.getDouble("Default:ChargeSafePower", 210);
            //对比最大值并且安全校验一下
            double power = Math.max(userChargerMaxPower, safeChargePower);
            power = Math.max(power, maxPower);
            if (power < 0) power = safeChargePower;
            if (power > 1000) {
                DeviceSocketEntity deviceSocketEntity = DeviceSocketEntity.getInstance().getWithDeviceCode(deviceEntity.deviceCode, port);
                if (deviceSocketEntity != null) {
                    power = deviceSocketEntity.limitChargePower;
                } else power = 1000;
            }
            helper.setLimitChargePower(power);
        }

        // endregion

        //启动充电
        SyncResult chargeResult = helper.start();

        LogsUtil.info(TAG, "NFC刷卡 - 充电请求 - %s - %s - %s", deviceCode, chargeResult.toJsonString(), JSONObject.from(helper).toJSONString());
        if (chargeResult.code != 0) return new SyncResult(3001, "启动充电失败");
        return new SyncResult(0, "");
    }

}
