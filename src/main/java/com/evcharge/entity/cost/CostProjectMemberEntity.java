package com.evcharge.entity.cost;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 项目成本成员;
 *
 * @author : JED
 * @date : 2025-1-13
 */
public class CostProjectMemberEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 其他关联的项目编码，如立项的项目编码
     */
    public String project_code;
    /**
     * 充电桩唯一编号
     */
    public String cs_id;
    /**
     * 成员姓名
     */
    public String member_name;
    /**
     * 成员联系手机号
     */
    public String member_phone;
    /**
     * 角色
     */
    public String member_role;
    /**
     * 角色中文
     */
    public String member_role_text;
    /**
     * 备注
     */
    public String remark;
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
     */
    public static CostProjectMemberEntity getInstance() {
        return new CostProjectMemberEntity();
    }
}
