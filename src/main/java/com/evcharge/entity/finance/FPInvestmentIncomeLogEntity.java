package com.evcharge.entity.finance;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 金融产品 - 投资加盟 - 收益日志;
 *
 * @author : JED
 * @date : 2022-11-9
 */
public class FPInvestmentIncomeLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 日期时间戳，精确到月
     */
    public long date_month;
    /**
     * 管理员ID
     */
    public long adminId;
    /**
     * 第几期
     */
    public int stage;
    /**
     * 订单ID
     */
    public long order_id;
    /**
     * 收益金额
     */
    public BigDecimal incomeAmount;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static FPInvestmentIncomeLogEntity getInstance() {
        return new FPInvestmentIncomeLogEntity();
    }
}
