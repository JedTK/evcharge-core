package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 安装工单资源表;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class InstallWorkOrderResEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 安装工单ID
     */
    public long iworder_id;
    /**
     * 资源链接
     */
    public int url;
    /**
     * 文件类型
     */
    public String fileType;
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
    public static InstallWorkOrderResEntity getInstance() {
        return new InstallWorkOrderResEntity();
    }
}
