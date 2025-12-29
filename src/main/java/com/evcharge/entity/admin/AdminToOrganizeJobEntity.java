package com.evcharge.entity.admin;

import com.evcharge.entity.rbac.RBJobEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员-组织职位关联 n-n;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AdminToOrganizeJobEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织职位id
     */
    public long job_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminToOrganizeJobEntity getInstance() {
        return new AdminToOrganizeJobEntity();
    }

    /**
     * 管理员绑定组织职位
     *
     * @param admin_id           管理员ID
     * @param organizeJob_idList 组织职位ID列表
     */
    public SyncResult bind(long admin_id, String organizeJob_idList) {
        return this.beginTransaction(connection -> bindTransaction(connection, admin_id, organizeJob_idList));
    }

    /**
     * 管理员绑定组织职位
     *
     * @param admin_id           管理员ID
     * @param organizeJob_idList 组织职位ID列表
     */
    public SyncResult bindTransaction(Connection connection, long admin_id, String organizeJob_idList) {
        //检查参数是否为空
        if (admin_id <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organizeJob_idList)) return new SyncResult(2, "组织职位ID列表不能为空");
        try {
            //检查管理员数组是否存在
            if (!AdminBaseEntity.getInstance().existTransaction(connection, admin_id)) {
                return new SyncResult(3, "请选择正确的管理员");
            }

            //读取存在的组织职位数据
            List<Map<String, Object>> list = RBJobEntity.getInstance().field("id")
                    .whereIn("id", organizeJob_idList)
                    .selectTransaction(connection);
            if (list.isEmpty()) return new SyncResult(5, "没有可绑定的职位");

            //删除已移除的组织职位
            this.where("admin_id", admin_id)
                    .whereNotIn("job_id", organizeJob_idList)
                    .delTransaction(connection);

            //读取此角色现在有的组织职位数据
            Map<String, Map<String, Object>> rpdic = this.field("admin_id,job_id")
                    .where("admin_id", admin_id)
                    .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "job_id");

            //循环有用的组织职位进行数据插入
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Map<String, Object> p = (Map<String, Object>) it.next();
                String job_id = MapUtil.getString(p, "id");

                //检查权限是否存在，如果已经存在不需要再次插入数据
                if (rpdic.containsKey(job_id)) continue;

                //不存在数据则进行数据新增
                Map<String, Object> tmp = new LinkedHashMap<>();
                tmp.put("admin_id", admin_id);
                tmp.put("job_id", job_id);
                if (this.insertTransaction(connection, tmp) == 0) {
                    return new SyncResult(10, "绑定失败");
                }
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员绑定组织职位");
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 管理员解除绑定组织职位
     *
     * @param admin_id           管理员ID
     * @param organizeJob_idList 权限列表ID
     */
    public SyncResult unBind(long admin_id, String organizeJob_idList) {
        return this.beginTransaction(connection -> unBindTransaction(connection, admin_id, organizeJob_idList));
    }

    /**
     * 管理员解除绑定组织职位
     *
     * @param admin_id           管理员ID
     * @param organizeJob_idList 权限列表ID
     */
    public SyncResult unBindTransaction(Connection connection, long admin_id, String organizeJob_idList) {
        //检查参数是否为空
        if (admin_id <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organizeJob_idList)) return new SyncResult(2, "组织ID列表不能为空");

        try {
            int noquery = this.getInstance()
                    .where("admin_id", admin_id)
                    .whereIn("job_id", organizeJob_idList)
                    .delTransaction(connection);

            if (noquery > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员解绑职位发生错误");
        }
        return new SyncResult(1, "");
    }
}
