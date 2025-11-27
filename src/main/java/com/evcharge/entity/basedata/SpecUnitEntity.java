package com.evcharge.entity.basedata;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 规格单元（插座和设备公用），用于定义
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class SpecUnitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 规格名
     */
    public String name;
    /**
     * 规格编码
     */
    public String code;
    /**
     * 备注
     */
    public String remark;
    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SpecUnitEntity getInstance() {
        return new SpecUnitEntity();
    }
}
