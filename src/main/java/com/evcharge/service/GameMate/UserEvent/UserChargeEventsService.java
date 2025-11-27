package com.evcharge.service.GameMate.UserEvent;

import com.evcharge.entity.gamemate.badge.events.UserChargeEventsEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserChargeEventsService {


    /**
     * 检查可以获得哪些徽章
     */
    public void check(long uid) {
        //充电分几类 充电区域 充电次数 充电费用 充电时长
        int chargeCount = getChargeCount(uid);
        int chargeTime=getTotalChargeTime(uid);


    }

    /**
     * 获取充电次数
     * @param uid
     * @return
     */
    public int getChargeCount(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid).count("id");
    }

    /**
     * 获取使用充电卡充电次数
     * @param uid uid
     * @return
     */
    public int getChargeCountForCard(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid)
                .where("payment_type", "chargeCard")
                .count("id");
    }

    /**
     * 获取使用余额充电次数
     * @param uid uid
     * @return
     */
    public int getChargeCountForBalance(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid)
                .where("payment_type", "balance")
                .count("id");
    }

    /**
     * 获取使用积分充电次数
     * @param uid uid
     * @return int
     */
    public int getChargeCountForIntegral(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid)
                .where("payment_type", "integral")
                .count("id");
    }

    /**
     * 获取总充电费用
     * @param uid  uid
     * @return BigDecimal
     */
    public BigDecimal getTotalChargeFee(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid).sumGetBigDecimal("total_fee");
    }

    /**
     * 获取总充电时长
     * @param uid uid
     * @return int
     */
    public int getTotalChargeTime(long uid) {
        return UserChargeEventsEntity.getInstance().where("uid", uid).sum("charge_time");

    }


}
