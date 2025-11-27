package com.evcharge.entity.sys;

import com.evcharge.entity.admin.AdminBaseEntity;
import com.evcharge.entity.rbac.RBRoleToMenuEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 菜单;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class SysMenuEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 图标代码
     */
    public String icon;
    /**
     * 名称
     */
    public String name;
    /**
     * 路径
     */
    public String path;
    /**
     * 上级ID
     */
    public long parent_id;
    /**
     * 排序索引
     */
    public long sort_index;
    /**
     * 备注
     */
    public String remark;

    //endregion

    public static SysMenuEntity getInstance() {
        return new SysMenuEntity();
    }

    /**
     * 读取管理员的所有菜单
     */
    public List<Map<String, Object>> getMenu(long admin_id) {
        return getMenu(admin_id, 0);
    }

    /**
     * 读取管理员的所有菜单
     */
    public List<Map<String, Object>> getMenu(long admin_id, long systemId) {
        List<Object> roleIdList = AdminBaseEntity.getInstance().getRoleIdList(admin_id);
        if (roleIdList == null || roleIdList.isEmpty()) return new LinkedList<>();
        RBRoleToMenuEntity rbRoleToMenuEntity = RBRoleToMenuEntity.getInstance();
        rbRoleToMenuEntity.field("s.id,s.icon,s.name,s.parent_id,s.path")
                .alias("r")
                .join(SysMenuEntity.getInstance().theTableName(), "s", "r.menu_id = s.id")
                .whereIn("role_id", roleIdList)
                .order("sort_index DESC,parent_id,id");

        if (systemId > 0) {
            rbRoleToMenuEntity.where("s.systemId", systemId);
        }
        return rbRoleToMenuEntity.select();
    }
}