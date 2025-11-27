package com.evcharge.enumdata;

public class EUserIntegralType {

    /**
     * 系统执行任务
     */
    public final static int Systems = 10;
    /**
     * 系统管理员操作
     */
    public final static int Systems_Admin = 11;

    //region充电相关2系列
    /**
     * 充电扣积分
     */
    public final static int Charge = 20;
    /**
     * 充电扣积分退款
     */
    public final static int Charge_Refund = 21;
    //endregion

    //region获取积分相关3系列
    /**
     * 活动获取
     */
    public final static int Active = 30;
    /**
     * 签到获取
     */
    public final static int Checkin = 31;
    /**
     * 分享购买半年卡获取积分
     */
    public final static int ChargeCard_Share = 32;
    //endregion

    //region 扣除4系列
    /**
     * 充值扣除
     */
    public final static int Recharge_Deduct = 40;
    //endregion
}
