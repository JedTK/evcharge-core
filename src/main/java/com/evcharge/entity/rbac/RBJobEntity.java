package com.evcharge.entity.rbac;

import com.evcharge.entity.admin.AdminToOrganizeJobEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * RBAC组织职位表;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBJobEntity extends BaseEntity implements Serializable {
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
     * 组织id
     */
    public long organize_id;
    /**
     * 备注
     */
    public String remark;

    //endregion

    public static RBJobEntity getInstance() {
        return new RBJobEntity();
    }

    /**
     * 查询管理员所在组织中的职位(只是针对单一职位)
     *
     * @param adminId     管理员id
     * @param organize_id 组织id
     * @return
     */
    public RBJobEntity getWithAdminId(long adminId, long organize_id) {
        return RBJobEntity.getInstance()
                .cache(String.format("Admin:%s:OrganizeJob", adminId))
                .field("job.*")
                .alias("job")
                .join(AdminToOrganizeJobEntity.getInstance().theTableName(), "ao", "ao.job_id = job.id")
                .leftJoin(RBOrganizeToJobEntity.getInstance().theTableName(), "oj", "ao.job_id = oj.job_id")
                .where("oj.organize_id", organize_id)
                .where("ao.admin_id", adminId)
                .order("job.id")
                .findModel();
    }
}
