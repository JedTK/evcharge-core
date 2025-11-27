package com.evcharge.entity.consumecenter.payment;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 支付方式表;null
 *
 * @author : Jay
 * @date : 2025-9-16
 */
@TargetDB("evcharge_consumecenter")
public class ConsumePaymentTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 支付方式ID,;
     */
    public long id;
    /**
     * 支付方式唯一编码,;
     */
    public String method_code;
    /**
     * 支付方式名称,;
     */
    public String method_name;
    /**
     * 状态 0=禁用 1=启用,;
     */
    public int status;
    /**
     * 配置信息,;
     */
    public String config_json;
    /**
     * 创建时间,;
     */
    public long created_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static ConsumePaymentTypeEntity getInstance() {
        return new ConsumePaymentTypeEntity();
    }
}