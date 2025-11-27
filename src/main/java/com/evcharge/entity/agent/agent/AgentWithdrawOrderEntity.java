package com.evcharge.entity.agent.agent;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理提现表;
 *
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentWithdrawOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    //region -- 实体类属性 --
    /**
     * id,;
     */
    public long id;
    /**
     * 管理员id,;
     */
    public long admin_id;
    /**
     * 组织代码,;
     */
    public String organize_code;
    /**
     * 订单编号,;
     */
    public String ordersn;
    /**
     * 税务主体 1=一般纳税人 2=小规模纳税人,;
     */
    public int tax_subject;
    /**
     * 提现日期,;
     */
    public long withdrawal_time;
    /**
     * 提现金额,;
     */
    public BigDecimal withdrawal_amount;
    /**
     * 提现手续费,;
     */
    public BigDecimal bank_handling_fee;
    /**
     * 开票公司,;
     */
    public String invoice_company;
    /**
     * 完成时间,;
     */
    public long finish_time;
    /**
     * 备注,;
     */
    public String remark;
    /**
     * 状态：-1=已取消 1=已提交，待上传发票，2=已上传发票，审核中 3=已审核，转账中，5=已转账 已完成 -2=拒绝,;
     */
    public int status;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AgentWithdrawOrderEntity getInstance() {
        return new AgentWithdrawOrderEntity();
    }


    /**
     * 创建订单号
     *
     * @param type
     * @return
     */
    public String createOrderSn(String type) {
        String OrderSN;
        switch (type) {
            case "withdraw":
                OrderSN = String.format("WO%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            case "refund":
                OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            default:
                OrderSN = String.format("WO%s%sSN", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
        }
        OrderSN = OrderSN.toUpperCase();
        return OrderSN;
    }

    public AgentWithdrawOrderEntity getOrderInfoByOrderSn(String orderSn) {
        return this
                .where("ordersn", orderSn)
                .findEntity();
    }


}