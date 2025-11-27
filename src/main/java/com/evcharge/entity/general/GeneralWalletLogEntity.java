package com.evcharge.entity.general;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 通用类钱包余额变动明细;
 *
 * @author : JED
 * @date : 2024-7-12
 */
@Getter
@Setter
public class GeneralWalletLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    private long id;
    /**
     * 收益人联系电话
     */
    private String phone;
    /**
     * 标题
     */
    private String title;
    /**
     * 交易订单号
     */
    private String order_sn;
    /**
     * 操作余额，正负收支余额
     */
    private BigDecimal change_balance;
    /**
     * 当前余额
     */
    private BigDecimal current_balance;
    /**
     * 交易类型
     */
    private String trade_type;
    /**
     * 交易备注
     */
    private String trade_remark;
    /**
     * (可选)关联账单
     */
    private String extra_order_sn;
    /**
     * (可选)额外数据
     */
    private String extra_data;
    /**
     * 操作管理员
     */
    private String operator_phone;
    /**
     * 操作IP
     */
    private String operator_ip;
    /**
     * 创建时间
     */
    private long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static GeneralWalletLogEntity getInstance() {
        return new GeneralWalletLogEntity();
    }
}
