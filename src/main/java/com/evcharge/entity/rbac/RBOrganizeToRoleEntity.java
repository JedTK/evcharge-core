package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 组织-角色关联 1-1;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBOrganizeToRoleEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 组织id
     */
    @Deprecated
    public long organize_id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 角色id
     */
    public long role_id;

    //endregion

    public static RBOrganizeToRoleEntity getInstance() {
        return new RBOrganizeToRoleEntity();
    }

    /**
     * 组织绑定角色 1-1
     *
     * @param organizeId 用户组ID
     * @param roleId     角色ID
     * @return
     */
    public SyncResult bind(long organizeId, long roleId) {
        if (!RBOrganizeEntity.getInstance().exist(organizeId)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(roleId)) return new SyncResult(4, "不存在角色");

        if (this.where("organize_id", organizeId).where("role_id", roleId).exist()) {
            return new SyncResult(0, "");
        } else {
            if (this.insertGetId(new HashMap<>() {{
                put("organize_id", organizeId);
                put("role_id", roleId);
            }}) > 0) return new SyncResult(0, "");
        }
        return new SyncResult(1, "绑定失败");
    }

    /**
     * 组织解除绑定角色
     *
     * @param organize_id 用户组ID
     * @param role_id     角色ID
     * @return
     */
    public SyncResult unBind(long organize_id, long role_id) {
        if (!RBOrganizeEntity.getInstance().exist(organize_id)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        if (this.where("organize_id", organize_id)
                .where("role_id", role_id)
                .del() > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
