package com.evcharge.entity.admin;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 管理员以及用户的分组;与角色多对多关系，相当于额外获得的权限
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AUGroupEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 名称
     */
    public String name;
    /**
     * 编码
     */
    public String code;
    /**
     * 上级ID
     */
    public long parent_id;
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

    public static AUGroupEntity getInstance() {
        return new AUGroupEntity();
    }

    /**
     * 获取管理员所在用户组（针对单一绑定）
     * @param admin_id
     * @return
     */
    public AUGroupEntity getWithAdminId(long admin_id) {
        AdminToAUGroupEntity adminToAUGroupEntity = AdminToAUGroupEntity.getInstance()
                .cache(String.format("Admin:%s:GroupId", admin_id))
                .where("admin_id", admin_id)
                .order("id")
                .findModel();
        if (adminToAUGroupEntity == null || adminToAUGroupEntity.id == 0) return null;

        return AUGroupEntity.getInstance()
                .cache(String.format("BaseData:AUGroup:%s:Detail", adminToAUGroupEntity.group_id))
                .findModel(adminToAUGroupEntity.group_id);
    }
}
