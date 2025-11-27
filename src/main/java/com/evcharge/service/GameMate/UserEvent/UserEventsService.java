package com.evcharge.service.GameMate.UserEvent;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.gamemate.badge.events.UserChargeEventsEntity;
import com.evcharge.entity.gamemate.badge.events.UserConsumeEventsEntity;
import com.evcharge.entity.gamemate.badge.events.UserGeneralEventsEntity;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserEventsService {


    /**
     * 创建消费类型事件日志
     *
     * @param uid         uid
     * @param orderSn     orderSn
     * @param consumeType consumeType
     * @param consumeFee  consumeFee
     * @param metadata    metadata
     */
    public void createConsumeEvent(long uid, String orderSn, String consumeType, BigDecimal consumeFee, JSONObject metadata) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("uid", uid);
        info.put("order_sn", orderSn);
        info.put("consume_type", consumeType);
        info.put("consume_fee", consumeFee);
        info.put("metadata", metadata.toString());
        info.put("create_time", TimeUtil.getTimestamp());
        UserConsumeEventsEntity.getInstance().insert(info);
    }

    /**
     * 创建充电记录事件表
     *
     * @param chargeOrderEntity chargeOrderEntity
     */
    public void createChargeEvent(ChargeOrderEntity chargeOrderEntity) {

        if (chargeOrderEntity == null) return;
        String paymentType = "";
        if (chargeOrderEntity.paymentTypeId == 1) {
            paymentType = "balance";
        }
        if (chargeOrderEntity.paymentTypeId == 2) {
            paymentType = "chargeCard";
        }
        if (chargeOrderEntity.paymentTypeId == 3) {
            paymentType = "integral";
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("uid", chargeOrderEntity.uid);
        info.put("order_sn", chargeOrderEntity.OrderSN);
        info.put("station_id", chargeOrderEntity.CSId);
        info.put("payment_type", paymentType);
        info.put("start_time", chargeOrderEntity.startTime);
        info.put("end_time", chargeOrderEntity.stopTime);
        info.put("charge_time", chargeOrderEntity.totalChargeTime);
        info.put("total_fee", chargeOrderEntity.totalAmount);
        info.put("is_holiday", isWeekend(chargeOrderEntity.startTime)); //判断是否在周末充电
        UserChargeEventsEntity.getInstance().insert(info);
    }

    /**
     * 创建通用事件表
     *
     * @param uid        uid
     * @param eventType  eventType
     * @param entityType entityType
     * @param entityId   entityId
     * @param metadata   metadata
     */
    public void createGeneralEvent(long uid, String eventType, String entityType, long entityId, JSONObject metadata) {
        /**
         * 需要查重 比如登录，如果存在登录类型事件，就不需要写入
         */
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("uid", uid);
        info.put("event_type", eventType);
        info.put("entity_type", entityType);
        info.put("entity_id", entityId);
        info.put("metadata", metadata.toString());
        UserGeneralEventsEntity.getInstance().insert(info);
    }

    /**
     * 判断是否在节假日
     *
     * @param millis millis
     * @return boolean
     */
    private static boolean isWeekend(long millis) {
        // 将时间戳转换为本地时间（默认使用系统时区，也可指定 ZoneId.of("Asia/Shanghai")）
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();

        // 周六或周日
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }


}
