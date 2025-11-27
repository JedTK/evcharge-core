package com.evcharge.strategy.ChargeCard;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.entity.device.DeviceEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;


/**
 * 充电卡策略工厂
 */
public class ChargeCardStrategyFactory extends BaseChargeCardStrategy {

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
        // 后面如果类型多，可以改成使用 Spring 注解注册策略类
        switch (configEntity.cardTypeId) {
            case 1: // 时效数字充电卡 : 这是一种具有时效性的虚拟充电卡，通常应用于数字月卡或季卡等形式，用户可根据卡片的时效性进行充电和使用，适用于周期性计费和使用场景。
                return new SyncResult(3003, "无效充电卡"); // 此卡启动策略由另外的代码逻辑控制

            case 2: // 时效实体卡 : 这是一种具有时效性的NFC实体卡，支持用户通过刷卡进行充电或其他时效性相关的服务，适用于需要实体卡的场景，且具有时效限制。
                return super.executeQueryPreHandle(deviceEntity, cardEntity, configEntity, trans_data);
            case 3: // 多功能账户实体卡 : 这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
                return new PhysicalMultifunctionAccountCardStrategy().executeQueryPreHandle(deviceEntity, cardEntity, configEntity, trans_data);
        }
        return new SyncResult(3003, "无效充电卡");
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
        switch (configEntity.cardTypeId) {
            case 1: // 时效数字充电卡 : 这是一种具有时效性的虚拟充电卡，通常应用于数字月卡或季卡等形式，用户可根据卡片的时效性进行充电和使用，适用于周期性计费和使用场景。
                return new SyncResult(3003, "无效充电卡"); // 此卡启动策略由另外的代码逻辑控制

            case 2: // 时效实体卡 : 这是一种具有时效性的NFC实体卡，支持用户通过刷卡进行充电或其他时效性相关的服务，适用于需要实体卡的场景，且具有时效限制。
                return super.executeQueryHandle(deviceEntity, cardEntity, configEntity, trans_data);
            case 3: // 多功能账户实体卡 : 这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
                return new PhysicalMultifunctionAccountCardStrategy().executeQueryHandle(deviceEntity, cardEntity, configEntity, trans_data);
        }
        return new SyncResult(3003, "无效充电卡");
    }

    /**
     * 执行 充电卡 充电能力前预处理
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 充电前检查
     */
    @Override
    public ISyncResult executeChargingPreHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        switch (configEntity.cardTypeId) {
            case 1: // 时效数字充电卡 : 这是一种具有时效性的虚拟充电卡，通常应用于数字月卡或季卡等形式，用户可根据卡片的时效性进行充电和使用，适用于周期性计费和使用场景。
                return new SyncResult(3003, "无效充电卡"); // 此卡启动策略由另外的代码逻辑控制

            case 2: // 时效实体卡 : 这是一种具有时效性的NFC实体卡，支持用户通过刷卡进行充电或其他时效性相关的服务，适用于需要实体卡的场景，且具有时效限制。
                return super.executeChargingPreHandle(deviceEntity, cardEntity, configEntity, trans_data);
            case 3: // 多功能账户实体卡 : 这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
                return new PhysicalMultifunctionAccountCardStrategy().executeChargingPreHandle(deviceEntity, cardEntity, configEntity, trans_data);
        }
        return new SyncResult(3003, "无效充电卡");
    }

    /**
     * 执行 充电卡 的充电能力
     * 根据卡类型，执行不同卡的固有能力，比如：
     * 1、时效数字充电卡：这是一种具有时效性的虚拟充电卡，通常应用于数字月卡或季卡等形式，用户可根据卡片的时效性进行充电和使用，适用于周期性计费和使用场景。
     * 2、时效实体卡：这是一种具有时效性的NFC实体卡，支持用户通过刷卡进行充电或其他时效性相关的服务，适用于需要实体卡的场景，且具有时效限制。
     * 3、多功能账户实体卡：这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
     *
     * @param deviceEntity 设备实体类
     * @param cardEntity   用户充电卡实体类
     * @param configEntity 充电卡配置实体类
     * @param trans_data   透传参数
     * @return 是否成功执行能力
     */
    @Override
    public ISyncResult executeChargingHandle(DeviceEntity deviceEntity, UserChargeCardEntity cardEntity, ChargeCardConfigEntity configEntity, JSONObject trans_data) {
        switch (configEntity.cardTypeId) {
            case 1: // 时效数字充电卡 : 这是一种具有时效性的虚拟充电卡，通常应用于数字月卡或季卡等形式，用户可根据卡片的时效性进行充电和使用，适用于周期性计费和使用场景。
                return new SyncResult(3003, "无效充电卡"); // 此卡启动策略由另外的代码逻辑控制

            case 2: // 时效实体卡 : 这是一种具有时效性的NFC实体卡，支持用户通过刷卡进行充电或其他时效性相关的服务，适用于需要实体卡的场景，且具有时效限制。
                return super.executeChargingHandle(deviceEntity, cardEntity, configEntity, trans_data);
            case 3: // 多功能账户实体卡 : 这是一种账户绑定的NFC实体卡，刷卡时会优先启动账户中的时效数字充电卡或余额进行支付。用户可以通过该卡实现更加灵活的充电支付方式，优先使用账户内的数字月卡、季卡等虚拟卡片，或者账户余额进行消费。
                return new PhysicalMultifunctionAccountCardStrategy().executeChargingHandle(deviceEntity, cardEntity, configEntity, trans_data);
        }
        return new SyncResult(3003, "无效充电卡");
    }

}
