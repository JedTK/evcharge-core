package com.evcharge.entity.station.qc;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 巡检静态资源表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class InspectionStaticResourceEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 关联的巡检日志明细ID
     */
    public long detail_id ;
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
    public static InspectionStaticResourceEntity getInstance() {
        return new InspectionStaticResourceEntity();
    }
}