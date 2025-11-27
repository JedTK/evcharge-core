package com.evcharge.entity.device;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 插座最小单元，用于定义
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class SocketUnitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 插座名
     */
    public String name;
    /**
     * 备注
     */
    public String remark;
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
    public static SocketUnitEntity getInstance() {
        return new SocketUnitEntity();
    }
}
