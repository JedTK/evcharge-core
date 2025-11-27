package com.evcharge.entity.admin;

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
 * 管理员-用户组关联 n-n;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AdminToAUGroupEntity extends BaseEntity implements Serializable {
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
     * 分组id
     */
    public long group_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminToAUGroupEntity getInstance() {
        return new AdminToAUGroupEntity();
    }

    /**
     * 管理员绑定用户组
     *
     * @param admin_id     管理员ID
     * @param group_idList 用户组ID列表
     * @return
     */
    public SyncResult bind(long admin_id, String group_idList) {
        return this.beginTransaction(connection -> bindTransaction(connection, admin_id, group_idList));
    }

    /**
     * 管理员绑定用户组
     *
     * @param admin_id     管理员ID
     * @param group_idList 用户组ID列表
     * @return
     */
    public SyncResult bindTransaction(Connection connection, long admin_id, String group_idList) {
        //检查参数是否为空
        if (admin_id <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(group_idList)) return new SyncResult(2, "用户组ID列表不能为空");
        try {
            //检查管理员数组是否存在
            if (!AdminBaseEntity.getInstance().existTransaction(connection, admin_id)) {
                return new SyncResult(3, "请选择正确的管理员");
            }

            //读取存在的用户组数据
            List<Map<String, Object>> list = AUGroupEntity.getInstance().field("id")
                    .whereIn("id", group_idList)
                    .selectTransaction(connection);
            if (list.size() == 0) return new SyncResult(5, "没有可绑定的权限");

            //删除已移除的用户组
            this.where("admin_id", admin_id)
                    .whereNotIn("group_id", group_idList)
                    .delTransaction(connection);

            //读取此角色现在有的用户组数据
            Map<String, Map<String, Object>> rpdic = this.field("admin_id,group_id")
                    .where("admin_id", admin_id)
                    .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "group_id");

            //循环有用的用户组进行数据插入
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Map<String, Object> p = (Map<String, Object>) it.next();
                String group_id = MapUtil.getString(p, "id");

                //检查权限是否存在，如果已经存在不需要再次插入数据
                if (rpdic.containsKey(group_id)) continue;

                //不存在数据则进行数据新增
                Map<String, Object> tmp = new LinkedHashMap<>();
                tmp.put("admin_id", admin_id);
                tmp.put("group_id", group_id);
                if (this.insertTransaction(connection, tmp) == 0) {
                    return new SyncResult(10, "绑定失败");
                }
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员绑定用户组");
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 管理员解除绑定用户组
     *
     * @param admin_id     管理员ID
     * @param group_idList 权限列表ID
     * @return
     */
    public SyncResult unBind(long admin_id, String group_idList) {
        return this.beginTransaction(connection -> unBindTransaction(connection, admin_id, group_idList));
    }

    /**
     * 管理员解除绑定用户组
     *
     * @param admin_id     管理员ID
     * @param group_idList 权限列表ID
     * @return
     */
    public SyncResult unBindTransaction(Connection connection, long admin_id, String group_idList) {
        //检查参数是否为空
        if (admin_id <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(group_idList)) return new SyncResult(2, "用户组ID列表不能为空");

        try {
            int noquery = AdminToAUGroupEntity.getInstance()
                    .where("admin_id", admin_id)
                    .whereIn("group_id", group_idList)
                    .delTransaction(connection);

            if (noquery > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员绑定用户组发生错误");
        }
        return new SyncResult(1, "");
    }
}
