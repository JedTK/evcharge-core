package com.evcharge.entity.finance;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 金融产品;
 * 通用表，用于记录产品名称和描述，具体配置表另外存放
 *
 * @author : JED
 * @date : 2022-11-7
 */
public class FinanceProductsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 标题
     */
    public String title;
    /**
     * 产品代码
     */
    public String code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态：
     */
    public int status;
    /**
     * 数据表：配置
     */
    public String DBTable_Config;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static FinanceProductsEntity getInstance() {
        return new FinanceProductsEntity();
    }
}
