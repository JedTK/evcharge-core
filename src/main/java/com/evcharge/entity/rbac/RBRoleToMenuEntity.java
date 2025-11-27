package com.evcharge.entity.rbac;

import com.evcharge.entity.sys.SysMenuEntity;
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
 * 角色-菜单关联;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBRoleToMenuEntity extends BaseEntity implements Serializable {
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
     * 菜单id
     */
    public long menu_id;

    //endregion

    public static RBRoleToMenuEntity getInstance() {
        return new RBRoleToMenuEntity();
    }

    /**
     * 角色绑定菜单
     *
     * @param role_id     角色ID
     * @param menu_idList 菜单ID列表
     * @return
     */
    public SyncResult bind(long role_id, String menu_idList) {
        //检查参数是否为空
        if (role_id <= 0) return new SyncResult(2, "请选择正确的角色");
        if (!StringUtils.hasLength(menu_idList)) return new SyncResult(2, "菜单ID列表不能为空");

        //检查角色数据是否存在
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");

        //读取存在的菜单数据
        List<Map<String, Object>> list = SysMenuEntity.getInstance().field("id")
                .whereIn("id", menu_idList)
                .select();
        if (list.size() == 0) return new SyncResult(5, "没有可绑定的权限");

        //开启事务，批量添加
        return this.beginTransaction(connection -> {
            try {
                //删除已移除的菜单
                this.where("role_id", role_id)
                        .whereNotIn("menu_id", menu_idList)
                        .delTransaction(connection);

                //读取此角色现在有的菜单
                Map<String, Map<String, Object>> rpdic = this.field("role_id,menu_id")
                        .where("role_id", role_id)
                        .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "menu_id");

                //循环有用的菜单进行数据插入
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    Map<String, Object> p = (Map<String, Object>) it.next();
                    String menu_id = MapUtil.getString(p, "id");

                    //检查菜单是否存在，如果已经存在不需要再次插入数据
                    if (rpdic.containsKey(menu_id)) continue;

                    //不存在数据则进行数据新增
                    Map<String, Object> tmp = new LinkedHashMap<>();
                    tmp.put("role_id", role_id);
                    tmp.put("menu_id", menu_id);
                    if (this.insertTransaction(connection, tmp) == 0) {
                        return new SyncResult(10, "绑定失败");
                    }
                }
                return new SyncResult(0, "");
            } catch (Exception e) {
                LogsUtil.error(e, "", "角色绑定菜单事务");
            }
            return new SyncResult(1, "操作失败");
        });
    }

    /**
     * 角色解绑菜单
     *
     * @param role_id     角色ID
     * @param menu_idList 菜单ID列表
     * @return
     */
    public SyncResult unBind(long role_id, String menu_idList) {
        //检查参数是否为空
        if (role_id <= 0) return new SyncResult(2, "请选择正确的角色");
        if (!StringUtils.hasLength(menu_idList)) return new SyncResult(2, "菜单ID列表不能为空");

        //检查角色数据是否存在
        if (!RBRoleEntity.getInstance().exist(role_id)) return new SyncResult(4, "不存在角色");
        int noquery = this.where("role_id", role_id)
                .whereIn("menu_id", menu_idList)
                .del();

        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
