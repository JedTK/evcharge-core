package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充电桩项目付款项;
 *
 * @author : JED
 * @date : 2023-10-25
 */
public class WFChargeStationPaymentItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 付款项：预付款、项目尾款、其他
     */
    public String paymentTitle;
    /**
     * 付款金额/元
     */
    public BigDecimal amount;
    /**
     * 付款日期
     */
    public long paymentTime;
    /**
     * 备注
     */
    public String remark;
    /**
     * 创建者id
     */
    public long creater_id;
    /**
     * 创建时间戳
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
    public static WFChargeStationPaymentItemEntity getInstance() {
        return new WFChargeStationPaymentItemEntity();
    }
}
