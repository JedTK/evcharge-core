package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩场地类型;
 *
 * @author : JED
 * @date : 2025-1-17
 */
public class CSSiteTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 合作方类型名
     */
    public String site_type_name;
    /**
     * 合作方类型代码
     */
    public String site_type_code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 上级合作方类型代码
     */
    public String parent_code;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static CSSiteTypeEntity getInstance() {
        return new CSSiteTypeEntity();
    }
}
