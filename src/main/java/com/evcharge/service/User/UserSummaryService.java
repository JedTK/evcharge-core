package com.evcharge.service.User;

import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.user.UserBalanceLogEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.ThreadUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserSummaryService {
    private final static String TAG = "用户数据统计";

    /**
     * 根据用户id获取统计数据
     *
     * @param uid uid
     * @return
     */
    public UserSummaryEntity getUserSummary(long uid) {
        UserSummaryEntity data = UserSummaryEntity.getInstance().where("uid", uid)
                .cache(String.format("User:%s:Summary", uid))
                .findEntity();
        if (data == null) {
            UserSummaryEntity.getInstance().initSummary(uid);
            data = UserSummaryEntity.getInstance().where("uid", uid)
                    .findEntity();
        }
        return data;
    }

    /**
     * 获取用户uid
     *
     * @param uid uid
     * @return BigDecimal
     */
    public BigDecimal getBalanceTransaction(Connection connection, long uid) {
        if (uid == 0) return new BigDecimal(0);
        try {
            BigDecimal balance = new BigDecimal(0);
            Map<String, Object> data = UserSummaryEntity.getInstance().field("id,uid,balance")
                    .where("uid", uid)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
            if (data.isEmpty()) {
                UserSummaryEntity.getInstance().initSummary(uid);
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, "", "", null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, "", null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, new BigDecimal(amount), eBalanceUpdateType, memo, orderSN, extraData, testId));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, "", "", null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, "", null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, null, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, extraData, 0));
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
        return UserSummaryEntity.getInstance().beginTransaction(connection -> updateBalanceTransaction(connection, uid, amount, eBalanceUpdateType, memo, orderSN, extraData, testId));
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

            Map<String, Object> userSummaryData = UserSummaryEntity.getInstance().where("uid", uid)
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

    // region remark - 更新用户充电次数、时长任务、总耗电量、三个月内最高充电功率、最近充电时间

    /**
     * 更新用户充电次数、时长任务、总耗电量、三个月内最高充电功率
     *
     * @param uid
     */
    public static void asyncUpdateUserChargeDataTask(long uid) {
        ThreadUtil.getInstance().execute("更新用户充电数据", () -> syncUpdateUserChargeDataTask(uid, 0, null));
    }

    /**
     * 更新用户充电次数、时长任务、总耗电量、三个月内最高充电功率、最近充电时间
     *
     * @param lastOrderEntity 最近充电订单
     */
    public static void asyncUpdateUserChargeDataTask(ChargeOrderEntity lastOrderEntity) {
        if (lastOrderEntity == null) return;
        ThreadUtil.getInstance().execute("充电完成更新用户充电数据", () -> syncUpdateUserChargeDataTask(lastOrderEntity.uid, 0, lastOrderEntity));
    }

    /**
     * 更新用户充电次数、时长任务、总耗电量、三个月内最高充电功率
     *
     * @param uid        用户id
     * @param cacheTtlMs 缓存时间（毫秒级)：>0时减少执行频率，=0时默认缓存10分钟，<0时立刻执行
     */
    public static void syncUpdateUserChargeDataTask(long uid, int cacheTtlMs) {
        syncUpdateUserChargeDataTask(uid, cacheTtlMs, null);
    }

    /**
     * 更新用户充电次数、时长任务、总耗电量、三个月内最高充电功率、最近充电时间
     *
     * @param uid             用户id
     * @param cacheTtlMs      缓存时间（毫秒级)：>0时减少执行频率，=0时默认缓存10分钟，<0时立刻执行
     * @param lastOrderEntity 最近充电的订单实体类
     */
    public static void syncUpdateUserChargeDataTask(long uid, int cacheTtlMs, ChargeOrderEntity lastOrderEntity) {
        String key = String.format("User:UpdateChargeDataTask:%s", uid);
        // 读缓存：只有当 >0 时才读
        if (cacheTtlMs > 0) {
            int haveCache = DataService.getMainCache().getInt(key, 0);
            if (haveCache >= 1) return;
        }

        try {
            long total_charge_time = 0;
            long total_charge_count = 0;
            double power_consumption = 0;
            double lastTimeChargeMaxPower = 0;

            // 查询充电时长、充电次数、总耗电量
            Map<String, Object> data = ChargeOrderEntity.getInstance()
                    .field("SUM(totalChargeTime) AS total_charge_time," +
                            "COUNT(id) AS total_charge_count," +
                            "SUM(powerConsumption) AS power_consumption")
                    .where("uid", uid)
                    .where("status", 2)
                    .find();
            if (data != null && data.size() > 0) {
                total_charge_time = MapUtil.getLong(data, "total_charge_time");
                total_charge_count = MapUtil.getLong(data, "total_charge_count");
                power_consumption = MapUtil.getDouble(data, "power_consumption");
            }

            //三个月内最高充电功率
            Map<String, Object> maxPowerData = ChargeOrderEntity.getInstance()
                    .field("id,maxPower")
                    .where("uid", uid)
                    .where("status", 2)
                    .where("startTime", ">=", TimeUtil.getMonthBegin(-3))
                    .where("startTime", "<=", TimeUtil.getMonthEnd24())
                    .order("maxPower DESC")
                    .find();
            if (maxPowerData != null && maxPowerData.size() > 0) {
                lastTimeChargeMaxPower = MapUtil.getDouble(maxPowerData, "maxPower");
            }

            //更新的数据
            Map<String, Object> set_data = new LinkedHashMap<>();
            set_data.put("charging_count", total_charge_count);
            set_data.put("charging_time", total_charge_time);
            set_data.put("realChargeTime", total_charge_time);
            set_data.put("totalPowerConsumption", power_consumption);
            set_data.put("lastTimeChargeMaxPower", lastTimeChargeMaxPower);
            if (lastOrderEntity != null && lastOrderEntity.id != 0) {
                set_data.put("last_charging_date", lastOrderEntity.startTime);
            }

            // 更新数据库
            UserSummaryEntity.getInstance().where("uid", uid).update(set_data);

            // 写缓存：>0 按给定TTL写；=0 用默认TTL写；<0 不写
            if (cacheTtlMs > 0) {
                DataService.getMainCache().set(key, 1, cacheTtlMs);
            } else if (cacheTtlMs == 0) {
                DataService.getMainCache().set(key, 1, ECacheTime.MINUTE * 5); // 默认10分钟
            }

            LogsUtil.info(TAG, "[%s] - 更新充电数据 - 总充电次数：%s，总充电时间：%s小时 (%s秒)",
                    uid, total_charge_count, TimeUtil.convertToHour(total_charge_time), total_charge_time);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s] - 更新充电数据发生错误");
        }
    }

    /**
     * 批量更新所有用户的充电数据任务。
     * 注意事项：
     * - 此函数应在离线任务或定时任务中执行，不建议在主线程直接调用；
     * - 建议在大数据量情况下，加入日志与异常捕获；
     * - 若 {@code syncUpdateUserChargeDataTask} 逻辑耗时较长，可考虑使用多线程并发执行。
     */
    public static void runUpdateUserChargeDataTask() {
        int page = 1;   // 当前页码
        int rows = 500; // 每页查询数量
        try {
            while (true) {
                // 从 UserEntity 表中分页查询用户ID列表
                List<Map<String, Object>> list = UserEntity.getInstance()
                        .field("id")           // 仅查询 id 字段即可
                        .page(page, rows)      // 分页查询
                        .select();             // 执行查询

                // 若无更多数据则退出循环
                if (list == null || list.isEmpty()) break;

                for (Map<String, Object> map : list) {
                    long uid = MapUtil.getLong(map, "id");

                    // 调用同步更新任务
                    syncUpdateUserChargeDataTask(uid, -1, null);

                }

                page++; // 翻页
            }
        } catch (Exception e) {
            // 建议加入日志，防止单个用户异常中断整个循环
            LogsUtil.error(e, TAG, "批量更新所有用户的充电数据任务 - 发生错误");
        }
    }

    // endregion

}
