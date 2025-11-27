package com.evcharge.entity.inspect.station;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;


@TargetDB("inspect")
public class ChargeStationStaticResourceEntity extends BaseEntity implements Serializable {


    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 站点uuid
     */
    public String cs_uuid ;
    /**
     * 资源类型
     */
    public String resource_type ;
    /**
     * 文件路径
     */
    public String file_path ;
    /**
     * 文件名
     */
    public String file_name ;
    /**
     * 文件大小
     */
    public long file_size ;
    /**
     * 状态 0=正常 1=删除
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ChargeStationStaticResourceEntity getInstance() {
        return new ChargeStationStaticResourceEntity();
    }


}
