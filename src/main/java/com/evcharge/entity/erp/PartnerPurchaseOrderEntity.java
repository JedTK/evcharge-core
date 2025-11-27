package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 合伙人：采购订单;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerPurchaseOrderEntity extends BaseEntity implements Serializable {
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
     * 折扣率
     */
    public BigDecimal discountedRate;
    /**
     * 折扣金额
     */
    public BigDecimal discountedAmount;
    /**
     * 税率
     */
    public BigDecimal taxRate;
    /**
     * 税额
     */
    public BigDecimal taxAmount;
    /**
     * 订单总额
     */
    public double totalAmount;
    /**
     * 应付款金额
     */
    public double payableAmount;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 状态：-1-删除，0-草稿，1-待审核，2-审核通过，3-审核不通过，4-订单完成
     */
    public int status;
    /**
     * 备注
     */
    public String remark;
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
    public static PartnerPurchaseOrderEntity getInstance() {
        return new PartnerPurchaseOrderEntity();
    }
}
