package com.evcharge.service.User;


import com.evcharge.entity.chargecard.ChargeCardConfigEntity;
import com.evcharge.entity.chargecard.UserChargeCardEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class UserChargeCardService {

    /**
     * 查询用户有效的充电卡
     * 2、通过uid、start_time、end_time查询还在生效的充电卡
     *
     * @param uid        用户id
     * @param cardTypeId 卡类型：1-数字充电卡，2-NFC-ID实体卡
     * @return 充电卡列表
     */
    public List<Map<String, Object>> getCardList(long uid, String cardTypeId) {
        long nowTime = TimeUtil.getTimestamp();
        UserChargeCardEntity userChargeCardEntity = UserChargeCardEntity.getInstance();
        userChargeCardEntity.where("uid", uid)
                .where("status", 1)
                .where("start_time", "<", nowTime)
                .where("end_time", ">", nowTime)
                .order("end_time asc");

        if (StringUtils.hasLength(cardTypeId)) {
            userChargeCardEntity.whereIn("cardTypeId", cardTypeId);
        }

        List<UserChargeCardEntity> cardList = userChargeCardEntity.selectList();
        if (cardList == null || cardList.isEmpty()) return new LinkedList<>();

        List<Map<String, Object>> list = new LinkedList<>();

        ChargeCardConfigEntity configEntity = null;

        for (UserChargeCardEntity card : cardList) {
            Map<String, Object> nd = new LinkedHashMap<>();
            if (configEntity == null) {
                configEntity = ChargeCardConfigEntity.getInstance().getConfigWithCode(card.spu_code);
            }
            String coverImage = "";
            if (configEntity.coverImage != null) {
                coverImage = configEntity.coverImage;
            }
            nd.put("coverImage", coverImage);
            nd.put("tags", configEntity.tags);
            nd.put("cardNumber", card.cardNumber);
            nd.put("cardName", card.cardName);
            nd.put("subtitle", String.format("%s", configEntity.subtitle));
            nd.put("describe", card.describe);
            nd.put("start_time", card.start_time);
            nd.put("end_time", card.end_time);
            nd.put("dailyChargeTime", configEntity.dailyChargeTime);

            list.add(nd); // 如果不检查充电桩兼容性，所有卡都视为允许
        }

        return list;
    }


}
