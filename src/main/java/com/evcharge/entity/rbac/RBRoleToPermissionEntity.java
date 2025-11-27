package com.evcharge.entity.rbac;

import com.xyzs.annotation.TargetDB;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色-权限 关联 n-n;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBRoleToPermissionEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 角色id
     */
    public long role_id;
    /**
     * 权限id
     */
    public long permission_id;

    //endregion

    public static RBRoleToPermissionEntity getInstance() {
        return new RBRoleToPermissionEntity();
    }

    /**
     * 角色绑定权限
     *
     * @param role_id           角色ID
     * @param permission_idList 权限列表ID
     */
    public SyncResult bind(long role_id, String permission_idList) {
        //检查参数是否为空
        if (role_id <= 0) return new SyncResult(2, "请选择正确的角色");
        if (!StringUtils.hasLength(permission_idList)) return new SyncResult(2, "权限ID列表不能为空");

        //检查角色数据是否存在
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        //读取存在的权限数据
        List<Map<String, Object>> list = RBPermissionEntity.getInstance().field("id")
                .whereIn("id", permission_idList)
                .select();
        if (list.size() == 0) return new SyncResult(5, "没有可绑定的权限");

        RBRoleToPermissionEntity rbRoleToPermissionEntity = new RBRoleToPermissionEntity();
        //开启事务，批量添加
        return rbRoleToPermissionEntity.beginTransaction(connection -> {
            try {
                //删除已移除的权限
                rbRoleToPermissionEntity.where("role_id", role_id)
                        .whereNotIn("permission_id", permission_idList)
                        .delTransaction(connection);

                //读取此角色现在有的权限
                Map<String, Map<String, Object>> rpdic = rbRoleToPermissionEntity.field("role_id,permission_id")
                        .where("role_id", role_id)
                        .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "permission_id");

                //循环有用的权限进行数据插入
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    Map<String, Object> p = (Map<String, Object>) it.next();
                    String permission_id = MapUtil.getString(p, "id");

                    //检查权限是否存在，如果已经存在不需要再次插入数据
                    if (rpdic.containsKey(permission_id)) continue;

                    //不存在数据则进行数据新增
                    Map<String, Object> tmp = new LinkedHashMap<>();
                    tmp.put("role_id", role_id);
                    tmp.put("permission_id", permission_id);
                    if (rbRoleToPermissionEntity.insertTransaction(connection, tmp) == 0) {
                        return new SyncResult(10, "绑定失败");
                    }
                }
                return new SyncResult(0, "");
            } catch (Exception e) {
                LogsUtil.error(e, "", "角色绑定权限事务");
            }
            return new SyncResult(1, "操作失败");
        });
    }

    /**
     * 角色解綁權限
     *
     * @param role_id           角色ID
     * @param permission_idList 权限列表ID
     * @return
     */
    public SyncResult unBind(long role_id, String permission_idList) {
        //检查参数是否为空
        if (role_id <= 0) return new SyncResult(2, "请选择正确的角色");
        if (!StringUtils.hasLength(permission_idList)) return new SyncResult(2, "权限ID列表不能为空");

        //检查角色数据是否存在
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        int noquery = RBRoleToPermissionEntity.getInstance()
                .where("role_id", role_id)
                .whereIn("permission_id", permission_idList)
                .del();

        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
