
package com.evcharge.entity.RSProfit;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分润收益日志;
 *
 * @author : JED
 * @date : 2024-7-1
 */
@Getter
@Setter
@TargetDB("evcharge_rsprofit")
public class RSProfitIncomeLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期，格式：yyyy-MM
     */
    public String date;
    /**
     * 日期时间戳
     */
    public long date_time;
    /**
     * 充电桩ID
     */
    public String cs_id;
    /**
     * 收益人联系电话
     */
    public String channel_phone;
    /**
     * 收益人角色：1-场地方，2-居间人，3-商务，4-物业，5-合作伙伴
     */
    public int channel_role;
    /**
     * 分润模式代码
     */
    public String mode_code;
    /**
     * 收益类型：1-普通，2-工资。主要用于区别员工的收益数据
     */
    public int income_type;
    /**
     * 分润配置ID
     */
    public long config_id;
    /**
     * 收益金额
     */
    public BigDecimal amount;
    /**
     * 原始数据JSON格式
     */
    public String raw_data;
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
     */
    public static RSProfitIncomeLogsEntity getInstance() {
        return new RSProfitIncomeLogsEntity();
    }
}
