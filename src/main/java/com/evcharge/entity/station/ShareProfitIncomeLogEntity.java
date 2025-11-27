package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分润收益记录;
 *
 * @author : JED
 * @date : 2022-12-22
 */
public class ShareProfitIncomeLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public long CSId;
    /**
     * 管理员ID
     */
    public long adminId;
    /**
     * 组织id，可能为0
     */
    public long organize_id;
    /**
     * 收益金额
     */
    public BigDecimal incomeAmount;
    /**
     * (可选)关联订单号
     */
    public String orderSN;
    /**
     * (可选)扩展数据
     */
    public String extraData;
    /**
     * 充电桩分润ID
     */
    public long CSSPId;
    /**
     * 开始时间戳
     */
    public long startTime;
    /**
     * 结束时间戳
     */
    public long endTime;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ShareProfitIncomeLogEntity getInstance() {
        return new ShareProfitIncomeLogEntity();
    }

    /**
     * 新增分润收益
     *
     * @param connection
     * @return
     */
    public SyncResult addTransaction(Connection connection) {
        try {
            this.create_time = TimeUtil.getTimestamp();
            if (this.insertTransaction(connection) == 0) return new SyncResult(1, "新增失败");
            return new SyncResult(0, "");
        } catch (SQLException e) {
            LogsUtil.error(e, "", "");
        } catch (IllegalAccessException e) {
            LogsUtil.error(e, "", "");
        }
        return new SyncResult(1, "");
    }
}
