package com.evcharge.service.ChargeCard;

import com.evcharge.entity.chargecard.PhysicalCardMappingDataEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * 实体卡映射数据Service层
 */
public class PhysicalCardMappingDataService {
    private final static String TAG = "实体充电卡映射列表";

    public static PhysicalCardMappingDataService getInstance() {
        return new PhysicalCardMappingDataService();
    }

    /**
     * 添加或更新实体卡映射
     *
     * @param uuid         UUID原始值（一般为16进制）
     * @param uuid_decimal UUID-10进制
     * @param card_number  物理卡号
     * @param forceUpdate  是否强制更新
     * @return 同步结果
     */
    public ISyncResult add(String uuid, String uuid_decimal, String card_number, boolean forceUpdate) {
        if (StringUtil.isEmpty(uuid) && StringUtil.isEmpty(uuid_decimal)) {
            return new SyncResult(2, "请读取卡UUID信息");
        }
        if (StringUtil.isEmpty(card_number)) return new SyncResult(2, "请输入物理卡号");
        try {
            if (StringUtil.isEmpty(uuid) && !StringUtil.isEmpty(uuid_decimal)) {
                // 10进制转16进制
                uuid = new BigInteger(uuid_decimal).toString(16).toUpperCase();
            }
            if (!StringUtil.isEmpty(uuid) && StringUtil.isEmpty(uuid_decimal)) {
                // 16进制转10进制
                uuid_decimal = new BigInteger(uuid, 16).toString();
            }

            PhysicalCardMappingDataEntity entity = PhysicalCardMappingDataEntity.getInstance()
                    .whereOr("uuid", uuid)
                    .whereOr("uuid_decimal", uuid_decimal)
                    .whereOr("card_number", card_number)
                    .findEntity();
            if (entity != null && entity.id != 0 && !forceUpdate) {
                return new SyncResult(3, "此卡已被添加");
            }

            if (entity == null) entity = new PhysicalCardMappingDataEntity();
            entity.uuid = uuid;
            entity.uuid_decimal = uuid_decimal;
            entity.card_number = card_number;

            if (entity.id == 0) {
                int noquery = entity.insert();
                if (noquery > 0) return new SyncResult(0, "");
            } else {
                entity.where("id", entity.id).update();
                return new SyncResult(0, "");
            }
        } catch (NumberFormatException e) {
            LogsUtil.error(e, TAG, "添加映射报错");
        }
        return new SyncResult(1, "添加失败");
    }

    /**
     * 读取卡信息
     *
     * @param identifier 10进制或16进制的uuid/卡号
     * @return 卡映射数据
     */
    public PhysicalCardMappingDataEntity getInfo(String identifier) {
        return PhysicalCardMappingDataEntity.getInstance()
                .field("id,uuid,uuid_decimal,card_number,spu_code")
                .cache(String.format("BaseData:PhysicalCardMappingData:%s", identifier))
                .whereOr("uuid", identifier)
                .whereOr("uuid_decimal", identifier)
                .whereOr("card_number", identifier)
                .findEntity();
    }

    /**
     * 读取物理卡号
     *
     * @param identifier 10进制或16进制的uuid/卡号
     * @return 物理卡号
     */
    public String getCardNumber(String identifier) {
        if (StringUtil.isEmpty(identifier)) return "";
        PhysicalCardMappingDataEntity entity = getInfo(identifier);
        if (entity == null) return "";
        return entity.card_number;
    }
}
