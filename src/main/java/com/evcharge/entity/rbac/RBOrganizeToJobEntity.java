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
 * 组织-职位关联 1-n;
 *
 * @author : JED
 * @date : 2022-9-1
 */
@TargetDB("evcharge_rbac")
public class RBOrganizeToJobEntity extends BaseEntity implements Serializable {
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
     * 组织职位id
     */
    public long job_id;

    //endregion

    public static RBOrganizeToJobEntity getInstance() {
        return new RBOrganizeToJobEntity();
    }


    /**
     * 组织绑定职位
     *
     * @param organize_id        组织ID
     * @param organizeJob_idList 职位ID列表
     * @return
     */
    public SyncResult bind(final long organize_id, final String organizeJob_idList) {
        //检查参数是否为空
        if (organize_id <= 0) return new SyncResult(2, "请选择正确的组织");
        if (!StringUtils.hasLength(organizeJob_idList)) return new SyncResult(2, "职位ID列表不能为空");

        //检查组织数据是否存在
        if (!RBOrganizeEntity.getInstance().exist(organize_id)) return new SyncResult(4, "不存在组织");

        //读取存在的职位数据
        List<Map<String, Object>> list = RBJobEntity.getInstance().field("id")
                .whereIn("id", organizeJob_idList)
                .select();
        if (list.size() == 0) return new SyncResult(5, "没有可绑定的职位");

        //开启事务，批量添加
        return this.beginTransaction(connection -> {
            try {
                //删除已移除的职位
                this.where("organize_id", organize_id)
                        .whereNotIn("job_id", organizeJob_idList)
                        .delTransaction(connection);

                //读取此角色现在有的职位
                Map<String, Map<String, Object>> rpdic = this.field("organize_id,job_id")
                        .where("organize_id", organize_id)
                        .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "job_id");

                //循环有用的权限进行数据插入
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    Map<String, Object> p = (Map<String, Object>) it.next();
                    String job_id = MapUtil.getString(p, "id");

                    //检查权限是否存在，如果已经存在不需要再次插入数据
                    if (rpdic.containsKey(job_id)) continue;

                    //不存在数据则进行数据新增
                    Map<String, Object> tmp = new LinkedHashMap<>();
                    tmp.put("organize_id", organize_id);
                    tmp.put("job_id", job_id);
                    if (this.insertTransaction(connection, tmp) == 0) {
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
     * 组织解綁职位
     *
     * @param organize_id        组织ID
     * @param organizeJob_idList 职位ID列表
     * @return
     */
    public SyncResult unBind(long organize_id, String organizeJob_idList) {
        //检查参数是否为空
        if (organize_id <= 0) return new SyncResult(2, "请选择正确的角色");
        if (!StringUtils.hasLength(organizeJob_idList)) return new SyncResult(2, "权限ID列表不能为空");

        //检查角色数据是否存在
        if (!RBOrganizeEntity.getInstance().exist(organize_id)) return new SyncResult(4, "不存在角色");

        int noquery = this.where("organize_id", organize_id)
                .whereIn("job_id", organizeJob_idList)
                .del();

        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
