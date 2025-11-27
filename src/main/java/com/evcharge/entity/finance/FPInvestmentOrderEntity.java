package com.evcharge.entity.finance;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.admin.AdminBaseEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.common;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 金融产品 - 投资加盟 - 订单表;
 *
 * @author : JED
 * @date : 2022-11-8
 */
public class FPInvestmentOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 管理员ID
     */
    public long adminId;
    /**
     * 配置ID
     */
    public long config_id;
    /**
     * 标题
     */
    public String title;
    /**
     * 分润模式，每月结算
     */
    public int profitSharingMode;
    /**
     * 投资金额，加盟金额
     */
    public double amount;
    /**
     * 标准桩数量(个)
     */
    public int chargeStaionCount;
    /**
     * 充电设备数量(个)
     */
    public int chargeDeviceCount;
    /**
     * 合作期限（月）
     */
    public int periods;
    /**
     * 预期总收益，百分比
     */
    public double expectedTotalIncomeRate;
    /**
     * 累计总收益（元）
     */
    public BigDecimal totalIncome;
    /**
     * 昨日收益，最后一次收益金额（元）
     */
    public BigDecimal lastIncome;
    /**
     * 累计收益率 = 累计收益/投资金额
     */
    public BigDecimal totalIncomeRate;
    /**
     * 开始生效时间戳
     */
    public long start_time;
    /**
     * 结束时间戳
     */
    public long end_time;
    /**
     * 状态，-1=结束收益，0=待生效，1=生效中
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static FPInvestmentOrderEntity getInstance() {
        return new FPInvestmentOrderEntity();
    }

    /**
     * 总标准桩数量
     *
     * @return
     */
    public long getTotalChargeStaionCount(long adminId) {
        return FPInvestmentOrderEntity.getInstance()
                .cache(String.format("Summary:FPInvestment:%s:chargeStaionCount:Total", adminId))
                .where("adminId", adminId)
                .sumGetLong("chargeStaionCount");
    }

    /**
     * 总投资金额
     *
     * @return
     */
    public BigDecimal getTotalInvestmentAmount(long adminId) {
        return FPInvestmentOrderEntity.getInstance()
                .cache(String.format("Summary:FPInvestment:%s:amount:Total", adminId))
                .where("adminId", adminId)
                .sumGetBigDecimal("amount", 2, RoundingMode.HALF_UP);
    }

    /**
     * 获取全部投资金额
     *
     * @return
     */
    public BigDecimal getGlobalTotalInvestmentAmount() {
        return FPInvestmentOrderEntity.getInstance()
                .cache("Summary:FPInvestment:Global:amount:Total", 5 * 60 * 1000)
                .sumGetBigDecimal("amount", 2, RoundingMode.HALF_UP);
    }
}
