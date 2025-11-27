package com.evcharge.entity.megadata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 街道/城镇日志;
 *
 * @author : JED
 * @date : 2023-8-21
 */
public class MDStreetLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 分配的充电桩唯一编码
     */
    public String cs_code;
    /**
     * 组织代码，所属组织
     */
    public String organize_code;
    /**
     * 日志内容
     */
    public String content;
    /**
     * 日志类型：101-巡检，102-断点，103-上电，104-监控离线，105-监控上线
     */
    public int log_type;
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
    public static MDStreetLogsEntity getInstance() {
        return new MDStreetLogsEntity();
    }
}
