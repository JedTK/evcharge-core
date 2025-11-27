package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 银行基础数据;
 *
 * @author : JED
 * @date : 2022-11-21
 */
public class SysBankEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 银行名
     */
    public String bankName;
    /**
     * 银行代码
     */
    public String bankCode;
    /**
     * 银行logo
     */
    public String bankLogo;
    /**
     * icon正方形
     */
    public String bank_icon_square;
    /**
     * icon长方形
     */
    public String bank_icon_rectangle;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysBankEntity getInstance() {
        return new SysBankEntity();
    }

    /**
     * 查询银行信息
     *
     * @param id
     * @return
     */
    public SysBankEntity getWithId(long id) {
        return this.cache(String.format("BaseData:SysBank:%s", id))
                .findModel(id);
    }
}
