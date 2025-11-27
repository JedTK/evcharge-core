package com.evcharge.entity.general;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.enumdata.EBalanceType;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

/**
 * 通用类钱包;
 *
 * @author : JED
 * @date : 2024-7-12
 */
public class GeneralWalletEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 收益人联系电话
     */
    private String phone;
    /**
     * 组织代码（可选）
     */
    private String organize_code;
    /**
     * 余额
     */
    private BigDecimal balance;
    /**
     * 乐观锁
     */
    private long revision;
    /**
     * 创建时间
     */
    private long create_time;
    /**
     * 更新时间
     */
    private long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static GeneralWalletEntity getInstance() {
        return new GeneralWalletEntity();
    }

    /**
     * 初始化钱包
     *
     * @param phone 用户手机
     * @return 是否初始化了
     */
    public boolean initWallet(@NonNull String phone) {
        try {
            int isInit = initCache().getInt(String.format("GeneralWallet:init:%s", phone));
            if (isInit == 1) return true;

            if (this.where("phone", phone).exist()) {
                initCache().set(String.format("GeneralWallet:init:%s", phone), 1);
                return true;
            }
            int noquery = this.insert(new LinkedHashMap<>() {{
                put("phone", phone);
                put("organize_code", "");
                put("balance", BigDecimal.ZERO);
                put("revision", 0);
                put("create_time", TimeUtil.getTimestamp());
                put("update_time", TimeUtil.getTimestamp());
            }});
            return noquery > 0;
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "初始化钱包发生错误");
        }
        return false;
    }

    /**
     * 获取余额
     *
     * @param phone 手机号码
     * @return
     */
    @NonNull
    public BigDecimal getBalance(@NonNull String phone) {
        SyncResult r = beginTransaction(connection -> {
            BigDecimal balance = getBalanceTransaction(connection, phone);
            return new SyncResult(0, "", balance);
        });
        return r.code == 0 ? (BigDecimal) r.data : BigDecimal.ZERO;
    }

    /**
     * 获取余额
     *
     * @param connection 事务链接器
     * @param phone      手机号码
     * @return
     */
    @NonNull
    public BigDecimal getBalanceTransaction(@NonNull Connection connection, @NonNull String phone) {
        try {
            BigDecimal balance = initCache().getBigDecimal(String.format("GeneralWallet:Balance:%s", phone), null);
            if (balance == null) {
                Map<String, Object> data = this.field("id,balance")
                        .where("phone", phone)
                        .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None);
                if (data == null || data.isEmpty()) {
                    initWallet(phone);
                } else {
                    balance = MapUtil.getBigDecimal(data, "balance", 2, RoundingMode.HALF_UP);
                }
                initCache().set(String.format("GeneralWallet:Balance:%s", phone), balance, 5 * ECacheTime.MINUTE);
            }
            if (balance != null) return balance;
        } catch (Exception e) {
            LogsUtil.error(e, "", "获取用户余额发生错误");
        }
        return BigDecimal.ZERO;
    }

    //region 新增余额操作

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult addBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, "", null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult addBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult addBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, "", extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult addBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult addBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, extra_data, operator_phone, operator_ip));
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult addBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, "", null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult addBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult addBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, "", extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult addBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult addBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        return updateBalanceTransaction(connection, phone, title, change_balance.abs(), trade_type, trade_remark, extra_order_sn, extra_data, operator_phone, operator_ip);
    }
    //endregion

    //region 扣除余额操作

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult deductBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, "", null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult deductBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult deductBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, "", extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult deductBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult deductBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, extra_data, operator_phone, operator_ip));
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult deductBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, "", null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult deductBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult deductBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, "", extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult deductBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult deductBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        return updateBalanceTransaction(connection, phone, title, change_balance.negate(), trade_type, trade_remark, extra_order_sn, extra_data, operator_phone, operator_ip);
    }
    //endregion

    //region 余额更新操作

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult updateBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, "", null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult updateBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, extra_order_sn, null, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult updateBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, "", extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult updateBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, extra_order_sn, extra_data, "", ""));
    }

    /**
     * 余额更新操作事务
     *
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult updateBalance(@NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        return this.beginTransaction(connection -> updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, extra_order_sn, extra_data, operator_phone, operator_ip));
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @return
     */
    public SyncResult updateBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark) {
        return updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, "", null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @return
     */
    public SyncResult updateBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn) {
        return updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, extra_order_sn, null, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult updateBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, "", extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @return
     */
    public SyncResult updateBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data) {
        return updateBalanceTransaction(connection, phone, title, change_balance, trade_type, trade_remark, extra_order_sn, extra_data, "", "");
    }

    /**
     * 余额更新操作事务
     *
     * @param connection     事务连接器
     * @param phone          联系手机，关联用户的
     * @param title          交易标题
     * @param change_balance 操作余额，正负收支余额
     * @param trade_type     交易类型
     * @param trade_remark   交易备注
     * @param extra_order_sn (可选)关联账单
     * @param extra_data     (可选)额外数据
     * @param operator_phone 操作管理员
     * @param operator_ip    操作IP
     * @return
     */
    public SyncResult updateBalanceTransaction(@NonNull Connection connection
            , @NonNull String phone
            , @NonNull String title
            , @NonNull BigDecimal change_balance
            , @NonNull EBalanceType trade_type
            , String trade_remark
            , String extra_order_sn
            , JSONObject extra_data
            , String operator_phone
            , String operator_ip) {
        try {
            //余额
            BigDecimal balance = this.getBalanceTransaction(connection, phone);
            //当前余额
            BigDecimal current_balance = balance.add(change_balance).setScale(4, RoundingMode.HALF_UP);

            // region 插入余额变动日志数据
            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("phone", phone);
            logData.put("title", title);
            logData.put("order_sn", String.format("GW%s%s", TimeUtil.toTimeString("yyyyMMddHHmmss"), common.randomInt(1000, 9999)));
            logData.put("change_balance", balance);
            logData.put("current_balance", current_balance);
            logData.put("trade_type", trade_type.toString());
            logData.put("trade_remark", trade_remark);

            if (StringUtil.hasLength(extra_order_sn)) logData.put("extra_order_sn", extra_order_sn);
            if (extra_data != null) logData.put("extra_data", extra_data.toJSONString());
            if (StringUtil.hasLength(operator_phone)) logData.put("operator_phone", operator_phone);
            if (StringUtil.hasLength(operator_ip)) logData.put("operator_ip", operator_ip);

            logData.put("create_time", TimeUtil.getTimestamp());
            if (GeneralWalletLogEntity.getInstance().insertTransaction(connection, logData) == 0) {
                return new SyncResult(11, "新增操作日志失败");
            }
            // endregion

            // region 更新钱包数据
            Map<String, Object> entityData = this.where("phone", phone)
                    .field("id,revision")
                    .findTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
            if (entityData == null || entityData.isEmpty()) initWallet(phone);

            long revision = MapUtil.getLong(entityData, "revision", 0);
            if (GeneralWalletEntity.getInstance()
                    .where("phone", phone)
                    .where("revision", revision)
                    .updateTransaction(connection, new LinkedHashMap<>() {{
                        put("revision", revision + 1);
                        put("balance", current_balance);
                        put("update_time", TimeUtil.getTimestamp());
                    }}) == 0) {
                return new SyncResult(1, "更新余额失败");
            }
            // endregion

            initCache().del(String.format("GeneralWallet:Balance:%s", phone));

            return new SyncResult(0, "");
        } catch (Exception e) {
            return new SyncResult(1, String.format("更新失败，失败原因：%s", e.getMessage()));
        }
    }
    //endregion
}
