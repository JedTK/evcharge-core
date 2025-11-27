package com.evcharge.entity.user;

import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户统计表;
 *
 * @author : Jay
 * @date : 2022-9-26
 */
public class UserSummaryEntity extends BaseEntity implements Serializable {
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
     * 账户余额
     */
    public BigDecimal balance;
    /**
     * 电动车数量
     */
    public int ebike_count;
    /**
     * 充电时长
     */
    public int charging_time;
    /**
     * 充电次数
     */
    public long charging_count;
    /**
     * 最后一次充电时间
     */
    public long last_charging_date;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * 可用优惠券数量
     */
    public int use_coupon_count;
    /**
     * 最近充电最大功率，根据程序计算，可能最近10次
     */
    public double lastTimeChargeMaxPower;
    /**
     * 实际充电时间
     */
    public long realChargeTime;
    /**
     * 总电量消耗
     */
    public double totalPowerConsumption;
    /**
     * 乐观锁
     */
    public long revision;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static UserSummaryEntity getInstance() {
        return new UserSummaryEntity();
    }

    /**
     * 初始化统计数据信息
     *
     * @param user_id
     */
    public void initSummary(long user_id) {
        //查询用户是否已经初始化
        if (this.exist(uid)) return;

        //没有该用户，则新建
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", user_id);
        data.put("create_time", TimeUtil.getTimestamp());
//        DataService.getMainCache().set("IsUserInitSummary",1);
        this.insert(data);
        return;
    }

    /**
     * 根据用户id获取统计数据
     *
     * @param uid
     * @return
     */
    public Map<String, Object> getUserSummary(long uid) {
        Map<String, Object> data = this.where("uid", uid)
                .cache(String.format("User:%s:Summary", uid))
                .find();
        if (data == null || data.isEmpty()) {
            this.initSummary(uid);
            data = this.where("uid", uid)
                    .find();
        }
        return data;
    }

    /**
     * 更新用户优惠券数量
     *
     * @param uid
     * @param count
     */
    public void updateUserCouponCount(long uid, int count) {
        Map<String, Object> userSummary = this.getUserSummary(uid);
        Map<String, Object> data = new LinkedHashMap<>();
        int before_count = MapUtil.getInt(userSummary, "use_coupon_count", 0);
        int after_count = before_count + count;
        data.put("use_coupon_count", after_count);
        userSummary.put("use_coupon_count", after_count);
        this.where("id", MapUtil.getLong(userSummary, "id")).update(data);
        DataService.getMainCache().setMap(String.format("User:%s:Summary", uid), userSummary);
    }


    /**
     * 获取用户uid
     *
     * @param uid
     * @return
     */
    public double getBalanceDoubleWithUid(long uid) {
        BigDecimal balance = getBalanceWithUid(uid);
        return balance.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 获取用户uid
     *
     * @param uid
     * @return
     */
    public int getIntegralWithUid(long uid) {
        int userIntegral = DataService.getMainCache().getInt(String.format("User:Integral:%s", uid), 0);
        if (userIntegral != 0) return userIntegral;

        SyncResult r = beginTransaction(connection -> {
            int amount = getIntegralTransaction(connection, uid);
            return new SyncResult(0, "", amount);
        });
        if (r.code == 0) userIntegral = (Integer) r.data;
        return userIntegral;
    }

    /**
     * 获取用户uid
     *
     * @param uid
     * @return
     */
    public int getIntegralTransaction(Connection connection, long uid) {
        if (uid == 0) return 0;
        try {
            int userIntegral = 0;
            Map<String, Object> data = this.field("id,uid,integral")
                    .where("uid", uid)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
            if (data.size() == 0) {
                this.initSummary(uid);
            } else {
                userIntegral = MapUtil.getInt(data, "integral");
            }
            DataService.getMainCache().set(String.format("User:Integral:%s", uid), userIntegral, 5 * 60 * 1000);
            return userIntegral;
        } catch (Exception e) {
            LogsUtil.error(e, "", "获取用户积分发生错误");
        }
        return 0;
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

        return UserIntegralLogEntity.getInstance().where("uid", uid)
                .where("expired_time", ">", 0)
                .where("expired_time", "BETWEEN", "UNIX_TIMESTAMP() and (UNIX_TIMESTAMP() + " + expireDay * 86400 + ")")
                .whereOr("expired_time", 0)
                .sum("change_integral");
    }

    /**
     * 获取用户uid
     *
     * @param uid
     * @return
     */
    public BigDecimal getBalanceWithUid(long uid) {
        BigDecimal balance = DataService.getMainCache().getBigDecimal(String.format("User:Balance:%s", uid), null);
        if (balance != null) return balance;

        SyncResult r = beginTransaction(connection -> {
            BigDecimal amount = getBalanceTransaction(connection, uid);
            return new SyncResult(0, "", amount);
        });
        if (r.code == 0) balance = (BigDecimal) r.data;
        return balance;
    }

    /**
     * 获取用户uid
     *
     * @param uid
     * @return
     */
    public BigDecimal getBalanceTransaction(Connection connection, long uid) {
        if (uid == 0) return new BigDecimal(0);
        try {
            BigDecimal balance = new BigDecimal(0);
            Map<String, Object> data = this.field("id,uid,balance")
                    .where("uid", uid)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
            if (data.isEmpty()) {
                this.initSummary(uid);
            } else {
                balance = MapUtil.getBigDecimal(data, "balance").setScale(2, RoundingMode.HALF_UP);
            }
            DataService.getMainCache().set(String.format("User:Balance:%s", uid), balance, 5 * 60 * 1000);
            return balance;
        } catch (Exception e) {
            LogsUtil.error(e, "", "获取用户余额发生错误");
        }
        return new BigDecimal(0);
    }

    //region double同步

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @return
     */
    public SyncResult updateBalance(long uid
            , double amount
            , String eBalanceUpdateType
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, "", "", null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @return
     */
    public SyncResult updateBalance(long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, "", null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @return
     */
    public SyncResult updateBalance(long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @return
     */
    public SyncResult updateBalance(long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @param testId             （可选）测试ID
     * @return
     */
    public SyncResult updateBalance(long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
            , long testId) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, testId));
    }
    //endregion

    //region double事务

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , double amount
            , String eBalanceUpdateType
    ) {
        return updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, "", "", null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
    ) {
        return updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, "", null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
    ) {
        return updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
    ) {
        return updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @param testId             （可选）测试ID
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , double amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
            , long testId) {
        return updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, testId);
    }
    //endregion

    //region BigDecimal同步

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @return
     */
    public SyncResult updateBalance(long uid
            , BigDecimal amount
            , String eBalanceUpdateType
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, "", "", null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @return
     */
    public SyncResult updateBalance(long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, "", null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @return
     */
    public SyncResult updateBalance(long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, null, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @return
     */
    public SyncResult updateBalance(long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
    ) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, extraData, 0));
    }

    /**
     * 更新用户余额
     *
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @param testId             （可选）测试ID
     * @return
     */
    public SyncResult updateBalance(long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
            , long testId) {
        return beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, extraData, testId));
    }
    //endregion

    //region BigDecimal事务

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , BigDecimal amount
            , String eBalanceUpdateType
    ) {
        return updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, "", "", null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
    ) {
        return updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, "", null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
    ) {
        return updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, null, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
    ) {
        return updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, extraData, 0);
    }

    /**
     * 更新用户余额
     *
     * @param connection         事务
     * @param uid                用户ID
     * @param amount             操作金额
     * @param eBalanceUpdateType 余额操作类型
     * @param memo               备注
     * @param orderSN            （可选）订单号
     * @param extraData          （可选）额外参数
     * @param testId             （可选）测试ID
     * @return
     */
    public SyncResult updateBalanceTransaction(Connection connection
            , long uid
            , BigDecimal amount
            , String eBalanceUpdateType
            , String memo
            , String orderSN
            , Map<String, Object> extraData
            , long testId) {
        try {
//            amount = amount.setScale(2, RoundingMode.HALF_UP);
            //获取当前用户余额
            BigDecimal userBalance = this.getBalanceTransaction(connection, uid);
            //变动余额
//            BigDecimal afterBalance = userBalance.add(amount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal afterBalance = userBalance.add(amount);

            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("uid", uid);
            logData.put("before_balance", userBalance);
            logData.put("change_balance", amount);
            logData.put("after_balance", afterBalance);
            logData.put("type", eBalanceUpdateType);
            logData.put("memo", memo);
            if (testId > 0) logData.put("isTest", 1);
            logData.put("testId", testId);
            logData.put("orderSN", orderSN);
            if (extraData != null && !extraData.isEmpty()) {
                logData.put("extraData", MapUtil.toJSONString(extraData));
            }
            logData.put("create_time", TimeUtil.getTimestamp());
            if (UserBalanceLogEntity.getInstance().insertTransaction(connection, logData) == 0) {
                return new SyncResult(11, "新增用户余额操作日志失败");
            }

            Map<String, Object> userSummaryData = this.where("uid", uid)
                    .field("id,revision")
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);

            long revision = MapUtil.getLong(userSummaryData, "revision");
            userSummaryData.put("revision", revision + 1);
            userSummaryData.put("balance", afterBalance);
            userSummaryData.put("update_time", TimeUtil.getTimestamp());

            if (UserSummaryEntity.getInstance()
                    .where("uid", uid)
                    .where("revision", revision)
                    .updateTransaction(connection, userSummaryData) == 0) {
                return new SyncResult(1, "更新余额失败");
            }
            DataService.getMainCache().set(String.format("User:Balance:%s", uid), afterBalance, 5 * 60 * 1000);

            return new SyncResult(0, "");
        } catch (Exception e) {
            return new SyncResult(1, String.format("更新失败，失败原因：%s", e.getMessage()));
        }
    }
    //endregion
}