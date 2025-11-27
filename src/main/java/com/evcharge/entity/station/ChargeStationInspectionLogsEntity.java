package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩巡查记录;
 *
 * @author : JED
 * @date : 2023-8-7
 */
public class ChargeStationInspectionLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 联系人姓名
     */
    public String contacts;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 管理员id
     */
    public long admin_id;
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
    public static ChargeStationInspectionLogsEntity getInstance() {
        return new ChargeStationInspectionLogsEntity();
    }
}
