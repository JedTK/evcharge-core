package com.evcharge.entity.sys;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 银行基础数据;
 *
 * @author : JED
 * @date : 2023-01-17
 */
public class SysBankBranchEntity extends BaseEntity implements Serializable {

    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 银行支行名
     */
    public String branch_name;
    /**
     * 银行id
     */
    public long bank_id;
    /**
     * 省份行政代码
     */
    public String province_code;
    /**
     * 城市行政代码
     */
    public String city_code;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysBankBranchEntity getInstance() {
        return new SysBankBranchEntity();
    }

    /**
     * 查询银行支行信息
     *
     * @param id
     * @return
     */
    public SysBankBranchEntity getWithId(long id) {
        return this.cache(String.format("BaseData:SysBankBranch:%s", id))
                .findModel(id);
    }
}
