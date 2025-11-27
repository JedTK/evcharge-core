package com.evcharge.entity.admin;

import com.evcharge.entity.rbac.RBOrganizeEntity;
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
 * 管理员-组织关联 n-n;
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class AdminToOrganizeEntity extends BaseEntity implements Serializable {
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
     * 组织id
     */
    @Deprecated
    public long organize_id;
    /**
     * 组织代码
     */
    public String organize_code;

    //endregion

    public static AdminToOrganizeEntity getInstance() {
        return new AdminToOrganizeEntity();
    }


    public AdminToOrganizeEntity getOrganize(long adminId) {
        return this.where("admin_id", adminId).findEntity();
    }

    public long getOrganizeIdByAdminId(long adminId) {
        AdminToOrganizeEntity adminToOrganizeEntity = getOrganize(adminId);
        if (adminToOrganizeEntity == null) return 0;
        return adminToOrganizeEntity.organize_id;
    }

    /**
     *
     * @return long
     */
    public long getOrganizeIdByAdminId() {
        long adminId = AdminBaseEntity.getInstance().getAdminIdWithRequest();
        if (adminId == 0) return 0;
        AdminToOrganizeEntity adminToOrganizeEntity = getOrganize(adminId);
        if (adminToOrganizeEntity == null) return 0;
        return adminToOrganizeEntity.organize_id;
    }

    /**
     * 根据管理员获取组织代码
     * @return String
     */
    public String getOrganizeCodeByAdminId() {
        long adminId = AdminBaseEntity.getInstance().getAdminIdWithRequest();
        if (adminId == 0) return null;
        AdminToOrganizeEntity adminToOrganizeEntity = getOrganize(adminId);
        if (adminToOrganizeEntity == null) return null;
        return adminToOrganizeEntity.organize_code;
    }

    //region 获取 管理员 组织代码

    /**
     * 获取管理员所有组织代码，并以字符串数组返回
     *
     * @return 组织代码数组: 组织A,组织B,组织C
     */
    public String[] getAllOrganizeCodeWithRequest() {
        long admin_id = AdminBaseEntity.getInstance().getAdminIdWithRequest();
        return getAllOrganizeCode(admin_id);
    }

    /**
     * 获取管理员所有组织代码，并以字符串数组返回
     *
     * @param admin_id 管理员 id
     * @return 组织代码数组: 组织A,组织B,组织C
     */
    public String[] getAllOrganizeCode(long admin_id) {
        return getAllOrganizeCode(admin_id, true);
    }

    /**
     * 获取管理员所有组织代码，并以字符串数组返回
     *
     * @param adminId 管理员 ID
     * @param inCache 优先从缓存中获取
     * @return 组织代码字符串，以逗号分隔：组织A,组织B,组织C
     */
    public String[] getAllOrganizeCode(long adminId, boolean inCache) {
        if (adminId == 0) return new String[]{};

        this.field("id,organize_code");
        if (inCache) this.cache(String.format("Admin:OrganizeCodeArray:%s", adminId));

        return this.where("admin_id", adminId).selectForStringArray("organize_code");
    }

    /**
     * 获取管理员所有组织代码，并以字符串数组返回
     *
     * @param adminId 管理员 ID
     * @param inCache 优先从缓存中获取
     * @return 组织代码字符串，以逗号分隔：组织A,组织B,组织C
     */
    private String getAllOrganizeCode20240611(long adminId, boolean inCache) {
        if (adminId == 0) return "";

        this.field("id,organize_code");
        if (inCache) this.cache(String.format("Admin:OrganizeCodeArray:%s", adminId));

        String[] organizeCodes = this.where("admin_id", adminId).selectForStringArray("organize_code");
        if (organizeCodes == null || organizeCodes.length == 0) return "";

        return String.join(",", organizeCodes);
    }

    /**
     * 通过请求获取唯一组织代码
     */
    public String getOrganizeCodeWithRequest() {
        long admin_id = AdminBaseEntity.getInstance().getAdminIdWithRequest();
        return getOrganizeCode(admin_id);
    }

    /**
     * 获取唯一组织代码
     *
     * @param admin_id 管理员 id
     * @return 组织代码数组
     */
    public String getOrganizeCode(long admin_id) {
        return getOrganizeCode(admin_id, true);
    }

    /**
     * 获取唯一组织代码
     *
     * @param admin_id 管理员 id
     * @param inCache  优先从缓存中获取
     * @return 组织代码数组
     */
    public String getOrganizeCode(long admin_id, boolean inCache) {
        if (admin_id == 0) return "";

        this.field("organize_code");
        if (inCache) this.cache(String.format("Admin:OrganizeCode:%s", admin_id));

        Map<String, Object> data = this.where("admin_id", admin_id).find();
        if (data == null || data.isEmpty()) return "";
        return MapUtil.getString(data, "organize_code");
    }
    //endregion

    //region 管理员 绑定 组织代码

    /**
     * 管理员绑定组织
     *
     * @param adminId           管理员ID
     * @param organize_codeList 组织代码列表
     */
    @Deprecated
    public SyncResult bindOrganizeCode(long adminId, String organize_codeList) {
        return this.beginTransaction(connection -> bindOrganizeCodeTransaction(connection, adminId, organize_codeList));
    }

    /**
     * 管理员绑定组织
     *
     * @param adminId           管理员ID
     * @param organize_codeList 组织代码列表
     * @return SyncResult
     */
    public SyncResult bindOrganizeCodeTransaction(Connection connection, long adminId, String organize_codeList) {
        // 检查参数是否有效
        if (adminId <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organize_codeList)) return new SyncResult(2, "组织代码列表不能为空");

        try {
            // 检查管理员是否存在
            if (!AdminBaseEntity.getInstance().existTransaction(connection, adminId)) {
                return new SyncResult(3, "请选择正确的管理员");
            }

            // 读取存在的组织数据
            List<Map<String, Object>> existingOrganizations = RBOrganizeEntity.getInstance().field("id")
                    .whereIn("code", organize_codeList)
                    .selectTransaction(connection);
            if (existingOrganizations.isEmpty()) return new SyncResult(5, "无法绑定，组织代码有误");

            // 删除已移除的组织
            this.where("admin_id", adminId)
                    .whereNotIn("organize_code", organize_codeList)
                    .delTransaction(connection);

            // 读取管理员当前拥有的组织数据
            Map<String, Map<String, Object>> currentOrganizations = this.field("admin_id,organize_code")
                    .where("admin_id", adminId)
                    .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "organize_code");

            // 循环插入新的组织数据
            for (Map<String, Object> organization : existingOrganizations) {
                String organizeCode = MapUtil.getString(organization, "id");

                // 如果组织已经存在，则跳过
                if (currentOrganizations.containsKey(organizeCode)) continue;

                // 插入新的组织数据
                Map<String, Object> newOrganization = new LinkedHashMap<>();
                newOrganization.put("admin_id", adminId);
                newOrganization.put("organize_code", organizeCode);

                if (this.insertTransaction(connection, newOrganization) == 0) {
                    return new SyncResult(10, "绑定失败");
                }
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员绑定组织");
            return new SyncResult(1, "操作失败");
        }
    }

    /**
     * 管理员解除绑定组织
     *
     * @param adminId           管理员ID
     * @param organize_codeList 组织代码列表
     */
    @Deprecated
    public SyncResult unBindOrganizeCode(long adminId, String organize_codeList) {
        return this.beginTransaction(connection -> unBindOrganizeCodeTransaction(connection, adminId, organize_codeList));
    }

    /**
     * 管理员解除绑定组织
     *
     * @param adminId           管理员ID
     * @param organize_codeList 组织代码列表
     */
    @Deprecated
    public SyncResult unBindOrganizeCodeTransaction(Connection connection, long adminId, String organize_codeList) {
        //检查参数是否为空
        if (adminId <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organize_codeList)) return new SyncResult(2, "组织代码列表不能为空");

        try {
            if (this.where("admin_id", adminId)
                    .whereIn("organize_code", organize_codeList)
                    .delTransaction(connection) > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员解绑组织发生错误");
        }
        return new SyncResult(1, "");
    }
    //endregion


    //region （待删除）旧版本 管理员 绑定 组织

    /**
     * 管理员绑定组织
     *
     * @param adminId         管理员ID
     * @param organize_idList 组织ID列表
     * @return
     */
    @Deprecated
    public SyncResult bind(long adminId, String organize_idList) {
        return this.beginTransaction(connection -> bindTransaction(connection, adminId, organize_idList));
    }

    /**
     * 管理员绑定组织
     *
     * @param adminId         管理员ID
     * @param organize_idList 组织ID列表
     * @return
     */
    @Deprecated
    public SyncResult bindTransaction(Connection connection, long adminId, String organize_idList) {
        //检查参数是否为空
        if (adminId <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organize_idList)) return new SyncResult(2, "组织ID列表不能为空");

        try {
            //检查管理员数组是否存在
            if (!AdminBaseEntity.getInstance().existTransaction(connection, adminId)) {
                return new SyncResult(3, "请选择正确的管理员");
            }

            //读取存在的组织数据
            List<Map<String, Object>> list = RBOrganizeEntity.getInstance().field("id")
                    .whereIn("id", organize_idList)
                    .selectTransaction(connection);
            if (list.size() == 0) return new SyncResult(5, "没有可绑定的权限");

            //删除已移除的组织
            this.where("admin_id", adminId)
                    .whereNotIn("organize_id", organize_idList)
                    .delTransaction(connection);

            //读取此角色现在有的组织数据
            Map<String, Map<String, Object>> rpdic = this.field("admin_id,organize_id")
                    .where("admin_id", adminId)
                    .selectForKeyTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.None, "organize_id");

            //循环有用的组织进行数据插入
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Map<String, Object> p = (Map<String, Object>) it.next();
                String organize_id = MapUtil.getString(p, "id");

                //检查权限是否存在，如果已经存在不需要再次插入数据
                if (rpdic.containsKey(organize_id)) continue;

                //不存在数据则进行数据新增
                Map<String, Object> tmp = new LinkedHashMap<>();
                tmp.put("admin_id", adminId);
                tmp.put("organize_id", organize_id);
                if (this.insertTransaction(connection, tmp) == 0) {
                    return new SyncResult(10, "绑定失败");
                }
            }
            return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员绑定组织");
        }
        return new SyncResult(1, "操作失败");
    }

    /**
     * 管理员解除绑定组织
     *
     * @param adminId         管理员ID
     * @param organize_idList 权限列表ID
     * @return
     */
    @Deprecated
    public SyncResult unBind(long adminId, String organize_idList) {
        return this.beginTransaction(connection -> unBindTransaction(connection, adminId, organize_idList));
    }

    /**
     * 管理员解除绑定组织
     *
     * @param adminId         管理员ID
     * @param organize_idList 权限列表ID
     * @return
     */
    @Deprecated
    public SyncResult unBindTransaction(Connection connection, long adminId, String organize_idList) {
        //检查参数是否为空
        if (adminId <= 0) return new SyncResult(2, "请选择正确的管理员");
        if (!StringUtils.hasLength(organize_idList)) return new SyncResult(2, "组织ID列表不能为空");

        try {
            if (this.getInstance()
                    .where("admin_id", adminId)
                    .whereIn("organize_id", organize_idList)
                    .delTransaction(connection) > 0) return new SyncResult(0, "");
        } catch (Exception e) {
            LogsUtil.error(e, "", "管理员解绑组织发生错误");
        }
        return new SyncResult(1, "");
    }
    //endregion
}