package com.evcharge.entity.finance;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 金融产品-投资加盟-配置;
 *
 * @author : JED
 * @date : 2022-11-7
 */
public class FPInvestmentConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 加盟项目名称
     */
    public String title;
    /**
     * 充电设备数量(个)
     */
    public int chargeDeviceCount;
    /**
     * 起投金额（元）
     */
    public int targetAmount;
    /**
     * 状态：0=删除，1=启动
     */
    public int status;
    /**
     * 合作期限（月）
     */
    public int periods;
    /**
     * 运营模式，暂时：委托平台运营
     */
    public int operationMode;
    /**
     * 分润模式，暂时：固定分润，每月结算
     */
    public int profitSharingMode;
    /**
     * 预期回本周期（月）
     */
    public int expectedPaybackPeriod;
    /**
     * 预期总回报倍数，显示请*100
     */
    public double expectedTotalIncomeRate;
    /**
     * 预期年化收益率，显示请*100
     */
    public double expectedAnnualizedRate;
    /**
     * 担保措施：刚性兑付、平台兜底
     */
    public int ensureType;
    /**
     * 渠道提成税后，显示请*100
     */
    public double commissionRate;
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
    public static FPInvestmentConfigEntity getInstance() {
        return new FPInvestmentConfigEntity();
    }
}
