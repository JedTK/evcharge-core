package com.evcharge.service.ChargeCard;

import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.ChargeCardTypeEntity;
import com.evcharge.entity.chargecard.PhysicalCardMappingDataEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电卡业务逻辑;
 * <p>
 * /**
 * 用户实体卡绑定
 * 充电卡业务逻辑，大部分旧版本的业务逻辑都在实体类中
 * date：2025-04-14
 */
public class ChargeCardService {
    private final static String TAG = "充电卡业务逻辑";

    public static ChargeCardService getInstance() {
        return new ChargeCardService();
    }

    /**
     * 用户绑定实体卡
     *
     * @param uid        用户id
     * @param cardNumber 充电卡卡号
     * @return 同步结果
     */
    public ISyncResult bindPhysicalCardByUid(long uid, String cardNumber) {
        if (uid == 0) return new SyncResult(99, "登录信息超时");
        if (StringUtil.isEmpty(cardNumber)) return new SyncResult(2, "请输入充电卡卡号");

        try {
            // 查询此卡是否已被绑定
            Map<String, Object> cardMap = UserChargeCardEntity.getInstance()
                    .field("uid")
                    .cache(String.format("TEMP:UserChargeCard:%s", cardNumber), ECacheTime.SECOND * 20)
                    .where("cardNumber", cardNumber)
                    .where("status", 1)
                    .find();
            if (!cardMap.isEmpty()) {
                if (MapUtil.getLong(cardMap, "uid") == uid) {
                    return new SyncResult(411, "您已成功绑定此卡");
                } else {
                    return new SyncResult(410, "此卡已被其他用户绑定");
                }
            }

            // 查询实体卡数据
            PhysicalCardMappingDataEntity physicalCardMappingDataEntity = PhysicalCardMappingDataService.getInstance().getInfo(cardNumber);
            if (physicalCardMappingDataEntity == null || physicalCardMappingDataEntity.id == 0) {
                return new SyncResult(404, "无效卡，请联系客服");
            }

            // 查询卡配置
            ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance().getConfigWithCode(physicalCardMappingDataEntity.spu_code);
            if (configEntity == null) return new SyncResult(405, "无效配置卡");

            // region 2025-06-18 用户只能绑定一种类型的实体卡
            if (configEntity.allowSuperposition == 0) {
                // 表示此卡不允许叠加，则同一种类型的卡，不允许绑定多张
                long now = TimeUtil.getTimestamp();
                if (UserChargeCardEntity.getInstance()
                        .where("uid", uid)
                        .where("spu_code", configEntity.spu_code)
                        .where("status", 1)
                        .where("start_time", "<", now)
                        .where("end_time", ">", now)
                        .exist()) {
                    return new SyncResult(406, "已有同类实体卡未解绑，请先解绑。");
                }
            }

            // endregion

            long start_time = TimeUtil.getTime00();
            long end_time = start_time;
            //region 处理生效时间
            switch (configEntity.typeId) {
                case 1://日
                    if (configEntity.countValue == 1) {
                        start_time = TimeUtil.getTimestamp();
                        //如果只是一天卡，结束时间应该以当前时间的第二天作为结束时间，这样体验效果会好一些
                        end_time = TimeUtil.getTimestamp();
                    }
                    end_time += configEntity.countValue * 86400000L;
                    break;
                case 2://月
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue);
                    break;
                case 3://年
                    end_time = TimeUtil.getAddMonthTimestamp(end_time, configEntity.countValue * 12);
                    break;
                default:
                    return new SyncResult(101, "错误的配置类型");
            }
            //endregion

            long finalStart_time = start_time;
            long finalEnd_time = end_time;
            UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance();
            userChargeCardEntity.uid = uid;
            userChargeCardEntity.cardName = configEntity.cardName;
            userChargeCardEntity.cardNumber = cardNumber;
            userChargeCardEntity.cardConfigId = configEntity.id;
            userChargeCardEntity.spu_code = configEntity.spu_code;
            userChargeCardEntity.cardTypeId = configEntity.cardTypeId;
            userChargeCardEntity.describe = configEntity.describe;
            userChargeCardEntity.status = 1;
            userChargeCardEntity.priority = configEntity.priority;
            userChargeCardEntity.start_time = finalStart_time;
            userChargeCardEntity.end_time = finalEnd_time;
            userChargeCardEntity.OrderSN = "";
            userChargeCardEntity.create_time = TimeUtil.getTimestamp();
            userChargeCardEntity.id = userChargeCardEntity.insertGetId();
            if (userChargeCardEntity.id == 0) return new SyncResult(1, "操作失败");

            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "用户绑定实体卡发生错误");
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 用户解绑实体卡
     *
     * @param uid        用户id
     * @param cardNumber 充电卡卡号
     * @return 同步结构
     */
    public ISyncResult unbindPhysicalCardByUid(long uid, String cardNumber) {
        if (uid == 0) return new SyncResult(99, "登录信息超时");
        if (StringUtil.isEmpty(cardNumber)) return new SyncResult(2, "请输入充电卡卡号");

        try {
            // 查询此卡是否已被绑定
            UserChargeCardEntity cardEntity = UserChargeCardEntity.getInstance()
                    .where("cardNumber", cardNumber)
                    .where("status", 1)
                    .order("id DESC")
                    .findEntity();
            if (cardEntity == null || cardEntity.id == 0) {
                return new SyncResult(404, "无效充电卡");
            }
            if (cardEntity.uid != uid) {
                return new SyncResult(99, "您无权操作此卡");
            }

            // 查询卡配置
            ChargeCardConfigEntity configEntity = ChargeCardConfigEntity.getInstance().getConfigWithCode(cardEntity.spu_code);
            if (configEntity == null) return new SyncResult(405, "无效配置卡");
            if (configEntity.typeId == 1) {
                // 如果是虚拟卡无法解绑
                return new SyncResult(406, "此卡为虚拟卡，无法进行解绑");
            }

            int noquery = cardEntity.update(cardEntity.id, new LinkedHashMap<>() {{
                put("status", 0);
            }});
            if (noquery >= 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "用户绑定实体卡发生错误");
        }
        return new SyncResult(1, "操作失败");
    }
}
