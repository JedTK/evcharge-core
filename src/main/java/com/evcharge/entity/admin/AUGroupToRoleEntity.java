package com.evcharge.entity.admin;

import com.evcharge.entity.rbac.RBRoleEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 管理员组-角色关联 1-1;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AUGroupToRoleEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 分组id
     */
    public long group_id;
    /**
     * 角色id
     */
    public long role_id;

    //endregion

    public static AUGroupToRoleEntity getInstance() {
        return new AUGroupToRoleEntity();
    }

    /**
     * 用户组绑定角色 1-1
     *
     * @param group_id 用户组ID
     * @param role_id  角色ID
     * @return
     */
    public SyncResult bind(long group_id, long role_id) {
        if (!AUGroupEntity.getInstance().exist(group_id)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        if (this.where("group_id", group_id).exist()) {
            if (this.where("group_id", group_id).update(new HashMap<>() {{
                put("role_id", role_id);
            }}) > 0) return new SyncResult(0, "");
        } else {
            this.group_id = group_id;
            this.role_id = role_id;
            if (this.insertGetId() > 0) return new SyncResult(0, "");
        }
        return new SyncResult(1, "绑定失败");
    }

    /**
     * 用户组解除绑定角色 1-1
     *
     * @param group_id 用户组ID
     * @param role_id  角色ID
     * @return
     */
    public SyncResult unBind(long group_id, long role_id) {
        if (!AUGroupEntity.getInstance().exist(group_id)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        if (this.where("group_id", group_id).where("role_id", role_id).del() > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

}
