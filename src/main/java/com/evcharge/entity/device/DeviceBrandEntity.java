package com.evcharge.entity.device;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 设备品牌;
 *
 * @author : JED
 * @date : 2022-9-15
 */
public class DeviceBrandEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 品牌名字
     */
    public String brandName;
    /**
     * 品牌代码
     */
    public String brandCode;
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
    public static DeviceBrandEntity getInstance() {
        return new DeviceBrandEntity();
    }
}
