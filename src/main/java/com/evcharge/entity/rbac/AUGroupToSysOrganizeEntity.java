package com.evcharge.entity.rbac;


import com.evcharge.entity.admin.AUGroupEntity;
import com.evcharge.entity.admin.AdminBaseEntity;
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
 * 用户组绑定系统组织;
 *
 * @author : JED
 * @date : 2022-12-30
 */
@TargetDB("evcharge_rbac")
public class AUGroupToSysOrganizeEntity extends BaseEntity implements Serializable {
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
     * 组织ID
     */
    @Deprecated
    public long organize_id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 职位ID
     */
    public long job_id;
    /**
     * 管理系统ID,0表示用户自定义
     */
    public long systemId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AUGroupToSysOrganizeEntity getInstance() {
        return new AUGroupToSysOrganizeEntity();
    }

    public SyncResult bindOrganize(long organizeId, long groupId) {
        //检查参数是否为空
        if (groupId <= 0) return new SyncResult(2, "请选择正确的用户组");
        if (organizeId <= 0) return new SyncResult(2, "请选择正确的组织");

        try {
            //检查管理员数组是否存在
            if (!AUGroupEntity.getInstance().exist(groupId)) {
                return new SyncResult(3, "请选择正确的用户组");
            }

            //读取存在的组织数据
            if (!RBOrganizeEntity.getInstance().exist(organizeId)) {
                return new SyncResult(3, "请选择正确的用户组");
            }

            //删除已移除的组织
            this.where("organize_id", organizeId)
                    .where("group_id", groupId)
                    .del();

            this.insert(new LinkedHashMap<>(){{
                put("group_id",groupId);
                put("organize_id",organizeId);
            }});
            //读取此角色现在有的组织数据
//            Map<String, Map<String, Object>> rpdic = this.field("admin_id,organize_id")
//                    .where("admin_id", adminId)
//                    .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "organize_id");
//
//            //循环有用的组织进行数据插入
//            Iterator it = list.iterator();
//            while (it.hasNext()) {
//                Map<String, Object> p = (Map<String, Object>) it.next();
//                String organize_id = MapUtil.getString(p, "id");
//
//                //检查权限是否存在，如果已经存在不需要再次插入数据
//                if (rpdic.containsKey(organize_id)) continue;
//
//                //不存在数据则进行数据新增
//                Map<String, Object> tmp = new LinkedHashMap<>();
//                tmp.put("admin_id", adminId);
//                tmp.put("organize_id", organize_id);
//                if (this.insertTransaction(connection, tmp) == 0) {
//                    return new SyncResult(10, "绑定失败");
//                }
//            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "用户组绑定组织");
        }
        return new SyncResult(1, "操作失败");
    }
}
