package com.evcharge.entity.sys;


import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 提现模式配置;
 *
 * @author : JED
 * @date : 2022-12-23
 */
public class WithdrawModeConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 标题
     */
    public String title;
    /**
     * 说明
     */
    public String desc;
    /**
     * 最低提现价格，当提现金额*费率<最低价格时，按最低价格收费
     */
    public double minFee;
    /**
     * 费率，每笔提现的费率
     */
    public BigDecimal feeRate;
    /**
     * 最低提现金额
     */
    public double minAmount;
    /**
     * 最大提现金额
     */
    public double maxAmount;
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
    public static WithdrawModeConfigEntity getInstance() {
        return new WithdrawModeConfigEntity();
    }

    /**
     * 根据配置id获取配置信息
     *
     * @param config_id
     * @return
     */
    public WithdrawModeConfigEntity getWithConfigId(long config_id) {
        return this.cache(String.format("Config:WithdrawMode:%s", config_id), ECacheTime.YEAR).findModel(config_id);
    }

    /**
     * 检查提现金额是否在配置范围内
     *
     * @param amount
     * @return
     */
    public SyncResult compareMinAndMaxWithAmount(BigDecimal amount) {
        if (this.id == 0) throw new NullPointerException();

        BigDecimal minAmount = new BigDecimal(this.minAmount);
        BigDecimal maxAmount = new BigDecimal(this.maxAmount);

        //BigDecimal.compareTo() a<b, 返回-1 a=b，返回0 a>b, 返回1
        if (amount.compareTo(minAmount) == -1) {
            return new SyncResult(101, String.format("提现金额最小%s起", minAmount.setScale(2, RoundingMode.HALF_UP)));
        }
        if (amount.compareTo(maxAmount) == 1) {
            return new SyncResult(102, String.format("提现金额最大%s以下", maxAmount.setScale(2, RoundingMode.HALF_UP)));
        }
        return new SyncResult(0, "");
    }

    /**
     * 根据配置规则进行手续费计算
     *
     * @param amount
     * @return
     */
    public BigDecimal billingFee(BigDecimal amount) {
        if (this.id == 0) throw new NullPointerException();

        BigDecimal minFee = new BigDecimal(this.minFee);
        BigDecimal fee = new BigDecimal(this.minFee);//默认最小手续费
        //BigDecimal.compareTo() a<b, 返回-1 a=b，返回0 a>b, 返回1
        if (feeRate.compareTo(BigDecimal.ZERO) == 1) {
            fee = amount.multiply(feeRate);
            if (fee.compareTo(minFee) == -1) fee = minFee;
        }
        return fee;
    }
}