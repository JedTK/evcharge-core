package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目联系人;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WFChargeStationProjectContactsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 姓名
     */
    public String name;
    /**
     * 角色
     */
    public String role;
    /**
     * 联系手机号
     */
    public String mobile;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WFChargeStationProjectContactsEntity getInstance() {
        return new WFChargeStationProjectContactsEntity();
    }
}
