package com.evcharge.entity.basedata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 收费标准分组;
 *
 * @author : JED
 * @date : 2022-10-8
 */
public class ChargeStandardConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置名
     */
    public String configName;
    /**
     * 计费类型：1-按峰值功率收费，2-按电量计费
     */
    public int billingType;
    /**
     * 拥有者id
     */
    public long owner_id;
    /**
     * 组织id
     */
    public long organize_id;
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
    public static ChargeStandardConfigEntity getInstance() {
        return new ChargeStandardConfigEntity();
    }
}