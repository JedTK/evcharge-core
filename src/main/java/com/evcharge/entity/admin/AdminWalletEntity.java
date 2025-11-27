package com.evcharge.entity.admin;


import com.evcharge.enumdata.EBalanceType;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员钱包;
 *
 * @author : JED
 * @date : 2022-12-22
 */
@Deprecated
public class AdminWalletEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long admin_id;
    /**
     * 组织ID
     */
    public long organize_id;
    /**
     * 余额
     */
    public BigDecimal balance;
    /**
     * 锁住的余额
     */
    public BigDecimal lockedBalance;
    /**
     * 乐观锁
     */
    public long revision;
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
    public static AdminWalletEntity getInstance() {
        return new AdminWalletEntity();
    }

    //region 获取用户余额

    /**
     * 获取管理员余额
     *
     * @param admin_id 管理员ID
     * @return
     */
    public BigDecimal getBalanceWithAdminId(long admin_id) {
        return getBalanceWithAdminId(admin_id, true);
    }

    /**
     * 获取管理员余额
     *
     * @param admin_id 管理员ID
     * @param inCache  是否从缓存中获取
     * @return
     */
    public BigDecimal getBalanceWithAdminId(long admin_id, boolean inCache) {
        if (admin_id == 0) return new BigDecimal(0);
        BigDecimal balance = new BigDecimal(0);

        //从缓存中读取
        if (inCache) {
            balance = DataService.getMainCache().getBigDecimal(String.format("Admin:%s:Wallet:Balance", admin_id), new BigDecimal(0));
        }

        //从数据库中读取
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            SyncResult r = beginTransaction(connection -> {
                BigDecimal amount = getBalanceTransaction(connection, admin_id);
                return new SyncResult(0, "", amount);
            });
            if (r.code == 0) balance = (BigDecimal) r.data;
        }
        return balance;
    }

    /**
     * 获取管理员余额
     *
     * @param connection 事务
     * @param admin_id   管理员ID
     * @return
     */
    public BigDecimal getBalanceTransaction(Connection connection, long admin_id) {
        try {
            if (admin_id == 0) return new BigDecimal(0);

            BigDecimal balance = AdminWalletLogEntity.getInstance()
                    .where("admin_id", admin_id)
                    .sumGetBigDecimalTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "amount", 2, RoundingMode.HALF_UP);
            if (balance == null) return new BigDecimal(0);

            DataService.getMainCache().set(String.format("Admin:%s:Wallet:Balance", admin_id), balance, 5 * 60 * 1000);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("balance", balance);
            data.put("update_time", TimeUtil.getTimestamp());

            Map<String, Object> wallet = this.field("id,revision")
                    .where("admin_id", admin_id)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
            if (wallet.isEmpty()) {
                data.put("admin_id", admin_id);
                data.put("revision", 1);
                data.put("create_time", TimeUtil.getTimestamp());
                this.insertTransaction(connection, data);
            } else {
                long walletId = MapUtil.getLong(wallet, "id");
                long revision = MapUtil.getLong(wallet, "revision");
                data.put("revision", revision + 1);
                this.where("id", walletId).updateTransaction(connection, data);
            }
            return balance;
        } catch (Exception e) {
            LogsUtil.error(e, "", "");
        }
        return new BigDecimal(0);
    }

    /**
     * 获取冻结余额
     *
     * @param admin_id 管理员id
     * @return
     */
    public BigDecimal getLockedBalanceWithAdminId(long admin_id) {
        return getLockedBalanceWithAdminId(admin_id, true);
    }

    /**
     * 获取冻结余额
     *
     * @param admin_id 管理员ID
     * @param inCache  是否优先从缓存中获取
     * @return
     */
    public BigDecimal getLockedBalanceWithAdminId(long admin_id, boolean inCache) {
        if (admin_id == 0) return new BigDecimal(0);
        BigDecimal lockedBalance = new BigDecimal(0);

        //从缓存中读取
        if (inCache) {
            lockedBalance = DataService.getMainCache().getBigDecimal(String.format("Admin:%s:Wallet:LockedBalance", admin_id), new BigDecimal(0));
        }

        //从数据库中读取
        if (lockedBalance.compareTo(BigDecimal.ZERO) == 0) {
            SyncResult r = beginTransaction(connection -> {
                BigDecimal amount = getLockedBalanceTransaction(connection, admin_id);
                return new SyncResult(0, "", amount);
            });
            if (r.code == 0) lockedBalance = (BigDecimal) r.data;
        }
        return lockedBalance;
    }

    /**
     * 获取冻结余额
     *
     * @param connection 事务
     * @param admin_id   管理员ID
     * @return
     */
    public BigDecimal getLockedBalanceTransaction(Connection connection, long admin_id) {
        try {
            if (admin_id == 0) return new BigDecimal(0);

            BigDecimal lockedBalance = AdminWalletLogEntity.getInstance()
                    .where("admin_id", admin_id)
                    .where("typeId", EBalanceType.LOCKED.index)
                    .sumGetBigDecimalTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "amount", 2, RoundingMode.HALF_UP);
            if (lockedBalance == null) return new BigDecimal(0);

            lockedBalance = lockedBalance.abs();//去绝对值
            DataService.getMainCache().set(String.format("Admin:%s:Wallet:LockedBalance", admin_id), lockedBalance, 5 * 60 * 1000);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("lockedBalance", lockedBalance);
            data.put("update_time", TimeUtil.getTimestamp());

            Map<String, Object> wallet = this.field("id,revision")
                    .where("admin_id", admin_id)
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
            if (wallet.isEmpty()) {
                data.put("admin_id", admin_id);
                data.put("revision", 1);
                data.put("create_time", TimeUtil.getTimestamp());
                this.insertTransaction(connection, data);
            } else {
                long walletId = MapUtil.getLong(wallet, "id");
                long revision = MapUtil.getLong(wallet, "revision");
                data.put("revision", revision + 1);
                this.where("id", walletId).updateTransaction(connection, data);
            }
            return lockedBalance;
        } catch (Exception e) {
            LogsUtil.error(e, "", "");
        }
        return new BigDecimal(0);
    }

    //endregion

    //region 新增钱包金额操作记录

    /**
     * 新增钱包金额操作记录
     *
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @return
     */
    public SyncResult add(long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount) {
        return beginTransaction(connection -> addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, "", null, "", ""));
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @return
     */
    public SyncResult add(long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc) {
        return beginTransaction(connection -> addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, null, "", ""));
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @param extraClass   （可选）关联类名
     * @param extraOrderSN （可选）关联订单号
     * @return
     */
    public SyncResult add(long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc
            , Class<?> extraClass
            , String extraOrderSN) {
        return this.beginTransaction(connection -> addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, extraClass, extraOrderSN, ""));
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @param extraClass   （可选）关联类名
     * @param extraOrderSN （可选）关联订单号
     * @param extraData    （可选）关联参数
     * @return
     */
    public SyncResult add(long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc
            , Class<?> extraClass
            , String extraOrderSN
            , String extraData) {
        return this.beginTransaction(connection -> addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, extraClass, extraOrderSN, extraData));
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param connection   事务链接
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @return
     */
    public SyncResult addTransaction(Connection connection
            , long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount) {
        return addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, "", null, "", "");
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param connection   事务链接
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @return
     */
    public SyncResult addTransaction(Connection connection
            , long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc) {
        return addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, null, "");
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param connection   事务链接
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @param extraClass   （可选）关联类名
     * @param extraOrderSN （可选）关联订单号
     * @return
     */
    public SyncResult addTransaction(Connection connection
            , long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc
            , Class<?> extraClass
            , String extraOrderSN) {
        return addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, extraClass, extraOrderSN, "");
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param connection   事务链接
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @param extraClass   （可选）关联类名
     * @param extraOrderSN （可选）关联订单号
     * @param extraData    （可选）关联参数
     * @return
     */
    public SyncResult addTransaction(Connection connection
            , long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc
            , Class<?> extraClass
            , String extraOrderSN
            , Map<String, Object> extraData) {
        if (extraData != null && !extraData.isEmpty()) {
            return addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, extraClass, extraOrderSN, MapUtil.toJSONString(extraData));
        } else {
            return addTransaction(connection, admin_id, organize_id, title, eBalanceType, amount, desc, extraClass, extraOrderSN, "");
        }
    }

    /**
     * 新增钱包金额操作记录
     *
     * @param connection   事务链接
     * @param admin_id     管理员ID
     * @param title        标题
     * @param eBalanceType 金额操作类型
     * @param amount       操作的金额
     * @param desc         （可选）备注
     * @param extraClass   （可选）关联类名
     * @param extraOrderSN （可选）关联订单号
     * @param extraData    （可选）关联参数
     * @return
     */
    public SyncResult addTransaction(Connection connection
            , long admin_id
            , long organize_id
            , String title
            , EBalanceType eBalanceType
            , BigDecimal amount
            , String desc
            , Class<?> extraClass
            , String extraOrderSN
            , String extraData) {
        try {
            String OrderSN = String.format("AW%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomInt(10, 99));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("OrderSN", OrderSN);
            data.put("admin_id", admin_id);
            data.put("organize_id", organize_id);
            data.put("title", title);
            data.put("typeId", eBalanceType.index);
            data.put("amount", amount);
            data.put("desc", desc);

            if (extraClass == null) data.put("extraClassName", "");
            else data.put("extraClassName", extraClass.getName());

            data.put("extraOrderSN", extraOrderSN);

            if (StringUtils.hasLength(extraData)) data.put("extraData", extraData);

            data.put("create_time", TimeUtil.getTimestamp());
            if (AdminWalletLogEntity.getInstance().insertTransaction(connection, data) == 0) {
                return new SyncResult(10001, "钱包新增记录失败");
            }
            getBalanceTransaction(connection, admin_id);
            return new SyncResult(0, "", new LinkedHashMap<>() {{
                put("OrderSN", OrderSN);
            }});
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员钱包操作添加日志发生错误");
        }
        return new SyncResult(1, "");
    }
    //endregion
}
