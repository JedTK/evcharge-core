package com.evcharge.entity.user;


import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户积分变动表;
 *
 * @author : Jay
 * @date : 2023-11-14
 */
public class UserIntegralLogEntity extends BaseEntity implements Serializable {
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
     * 类型
     */
    public int type_id;
    /**
     * 事件id 不同的类型不同的事件id
     */
    public long event_id;
    /**
     * 变动前积分
     */
    public int before_integral;
    /**
     * 变动的积分
     */
    public int change_integral;
    /**
     * 变动后积分
     */
    public int after_integral;
    /**
     * 过期时间
     */
    public int expired_time;
    /**
     * 额外参数
     */
    public String extra_data;
    /**
     * 测试id
     */
    public long test_id;
    /**
     * 备注
     */
    public String memo;
    /**
     * 状态 -1=已过期 1=已使用 这个状态针对用户获取积分的过期和使用状态管理
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static UserIntegralLogEntity getInstance() {
        return new UserIntegralLogEntity();
    }


    /**
     * 获取用户过期的积分，默认7天
     *
     * @param uid
     * @return
     */
    public int getExpiredTimeIntegral(long uid) {
        return getExpiredTimeIntegral(uid, 7);
    }

    /**
     * 获取用户过期的积分
     *
     * @param uid
     * @param expireDay
     * @return
     */
    public int getExpiredTimeIntegral(long uid, int expireDay) {

        return this.where("uid", uid)
                .where("expired_time", ">", 0)
                .where("expired_time", "BETWEEN", "UNIX_TIMESTAMP() and (UNIX_TIMESTAMP() + " + expireDay * 86400 * 1000 + ")")
                .where("change_integral",">",0)
                .where("status", 0)
                .sum("change_integral");
    }

    /**
     * 获取未过期的积分
     *
     * @param uid
     * @return
     */
    public int getNotExpiredIntegral(long uid) {
        /**
         * 1、获取已经过期的积分
         * 2、获取总积分
         * 3、总积分-过期积分
         * 4、更新积分表
         */
        int userIntegral = UserSummaryEntity.getInstance().getIntegralWithUid(uid);
        int expiredIntegral = getExpiredTimeIntegral(uid);
        int lastIntegral = userIntegral - expiredIntegral;
        if (lastIntegral < 0) {
            lastIntegral = 0;
        }


        return 0;
    }

    public void updateExpiredIntegral(long uid) {
        Map<String, Object> data = new LinkedHashMap<>();
        long currentTime = TimeUtil.getTimestamp();
        data.put("status", -1);
        this.where("uid", uid)
                .where("status", 0)
                .where("expired_time", ">", currentTime)
                .where("change_integral",">",0)
                .update(data);

    }



    /**
     * 消耗积分
     * @param uid
     * @param integral
     * @return
     */
    public SyncResult consumeIntegral(long uid, int integral) {
        /**
         * 1、获取即将过期的积分列表
         * 2、批量更新
         */
//        DataService.getMainDB().executeSQLTransaction();

        return DataService.getMainDB().beginTransaction(connection -> {
            return consumeIntegral(connection,uid,integral);
        });
    }

    public SyncResult consumeIntegral(Connection connection,long uid, int integral) {
        /**
         * 1、获取即将过期的积分列表
         * 2、批量更新
         */
        String sql=String.format("select * from UserIntegralLog WHERE uid = %s order by case when expired_time > 0 AND expired_time <= UNIX_TIMESTAMP(NOW()) THEN 1 WHEN expired_time = 0 THEN 3 ELSE 2 END,create_time ASC");
//        SELECT *
//                FROM UserIntegralLog
//        WHERE uid = <用户ID>
//                ORDER BY
//                CASE
//        WHEN expired_time > 0 AND expired_time <= UNIX_TIMESTAMP(NOW()) THEN 1 -- 优先使用即将过期的积分
//        WHEN expired_time = 0 THEN 3 -- 不限期积分，最后使用
//        ELSE 2 -- 近期积分
//                END,
//        create_time ASC;


//        DataService.getMainDB().executeSQLTransaction();




        return new SyncResult(1, "");
    }
    /**
     * 更新积分
     *
     * @param uid
     * @param amount
     * @param updateType
     * @param memo
     * @param expiredTime
     * @param eventId
     * @return
     */
    public SyncResult updateIntegral(
            long uid
            , int amount
            , int updateType
            , String memo
            , long expiredTime
            , long eventId
    ) {

        return DataService.getMainDB().beginTransaction(connection -> {
            return updateIntegralTransaction(connection, uid, amount, updateType, memo, expiredTime, eventId, null, 0);
        });

    }


    /**
     * 更新用户积分
     *
     * @param connection
     * @param uid
     * @param amount
     * @param updateType
     * @param memo
     * @param expiredTime 时间戳
     * @param eventId
     * @param extraData
     * @param testId
     * @return
     */
    public SyncResult updateIntegralTransaction(Connection connection
            , long uid
            , int amount
            , int updateType
            , String memo
            , long expiredTime
            , long eventId
            , Map<String, Object> extraData
            , long testId) {
        System.out.println("changeIntegral="+amount);
        try {
            int userIntegral = UserSummaryEntity.getInstance().getIntegralTransaction(connection, uid);
            int afterIntegral = userIntegral + amount;
            if (afterIntegral < 0) {
                afterIntegral = 0;
            }
            Map<String, Object> logData = new LinkedHashMap<>();

            logData.put("uid", uid);
            logData.put("before_integral", userIntegral);
            logData.put("change_integral", amount);
            logData.put("after_integral", afterIntegral);
            logData.put("expired_time", expiredTime);
            logData.put("type_id", updateType);
            logData.put("memo", memo);
            logData.put("test_id", testId);
            logData.put("event_id", eventId);
            if (extraData != null && extraData.size() > 0) {
                logData.put("extra_data", MapUtil.toJSONString(extraData));
            }
            logData.put("create_time", TimeUtil.getTimestamp());
            if (this.insertTransaction(connection, logData) == 0) {
                return new SyncResult(11, "新增用户积分操作日志失败");
            }
            Map<String, Object> userSummaryData = UserSummaryEntity.getInstance().where("uid", uid)
                    .field("id,revision")
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);

            long revision = MapUtil.getLong(userSummaryData, "revision");
            userSummaryData.put("revision", revision + 1);
            userSummaryData.put("integral", afterIntegral);
            userSummaryData.put("update_time", TimeUtil.getTimestamp());

            if (UserSummaryEntity.getInstance()
                    .where("uid", uid)
                    .where("revision", revision)
                    .updateTransaction(connection, userSummaryData) == 0) {
                return new SyncResult(1, "更新余额失败");
            }
            DataService.getMainCache().set(String.format("User:Integral:%s", uid), afterIntegral, 5 * 60 * 1000);

            return new SyncResult(0, "");


        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }

    }


}