package com.evcharge.entity.payment;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 支付方式;
 *
 * @author : JED
 * @date : 2024-4-24
 */
public class PaymentTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 编码
     */
    public String code;
    /**
     * 标题
     */
    public String title;
    /**
     * icon
     */
    public String icon;
    /**
     * 说明
     */
    public String description;
    /**
     * 运营商
     */
    public String operator;
    /**
     * 状态
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PaymentTypeEntity getInstance() {
        return new PaymentTypeEntity();
    }
}