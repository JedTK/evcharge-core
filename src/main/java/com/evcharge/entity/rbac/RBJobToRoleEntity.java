package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 组织职位-角色关联 1-1;
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBJobToRoleEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 组织职位id
     */
    public long job_id ;
    /**
     * 角色id
     */
    public long role_id ;

    //endregion

    public static RBJobToRoleEntity getInstance() {
        return new RBJobToRoleEntity();
    }

    /**
     * 组织绑定角色 1-1
     *
     * @param job_id 用户组ID
     * @param role_id     角色ID
     * @return
     */
    public SyncResult bind(long job_id, long role_id) {
        if (!RBJobEntity.getInstance().exist(job_id)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        if (this.where("job_id", job_id).exist()) {
            if (this.where("job_id", job_id).update(new HashMap<>() {{
                put("role_id", role_id);
            }}) > 0) return new SyncResult(0, "");
        } else {
            this.job_id = job_id;
            this.role_id = role_id;
            if (this.insertGetId() > 0) return new SyncResult(0, "");
        }
        return new SyncResult(1, "绑定失败");
    }

    /**
     * 组织解除绑定角色
     *
     * @param job_id 用户组ID
     * @param role_id     角色ID
     * @return
     */
    public SyncResult unBind(long job_id, long role_id) {
        if (!RBJobEntity.getInstance().exist(job_id)) return new SyncResult(3, "不存在用户组");
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        if (this.where("job_id", job_id)
                .where("role_id", role_id)
                .del() > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
