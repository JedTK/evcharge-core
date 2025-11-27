package com.evcharge.service.Cost;

import com.evcharge.entity.cost.CostCategoryEntity;
import com.evcharge.entity.cost.CostItemEntity;
import com.evcharge.entity.cost.CostProjectMemberEntity;
import com.evcharge.entity.rbac.RBRoleEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

public class CostProjectMemberService {
    /**
     * 标签
     */
    protected final static String TAG = "项目成本参与者";

    // 单例模式的静态实例
    private volatile static CostProjectMemberService _this;

    /**
     * 获取单例
     * 采用双重检查锁定机制确保线程安全
     *
     * @return
     */
    public static CostProjectMemberService getInstance() {
        if (_this == null) {
            synchronized (CostProjectMemberService.class) {
                if (_this == null) _this = new CostProjectMemberService();
            }
        }
        return _this;
    }

    /**
     * 成员列表
     *
     * @param project_code
     * @param cs_id
     * @param page
     * @param limit
     * @return
     */
    public List<Map<String,Object>> getList(@NonNull String project_code
            , @NonNull String cs_id
            , int page
            , int limit
            , boolean inCache
    ) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return null;
        }
        CostProjectMemberEntity entity = CostProjectMemberEntity.getInstance();
        entity.field("member_name,member_phone,member_role,member_role_text");
        if (inCache) {
            entity.cache(String.format("Cost:Items:%s%s:%s_%s", project_code, cs_id, limit, page), ECacheTime.MINUTE);
        }
        return entity
                .whereOr("project_code", project_code)
                .whereOr("cs_id", cs_id)
                .page(page, limit)
                .order("id")
                .select();
    }

    /**
     * 添加成员（支持成员多角色）自动检查是否重复检测
     *
     * @param project_code
     * @param cs_id
     * @param member_name
     * @param member_phone
     * @param member_role
     * @return
     */
    public ISyncResult put(@NonNull String project_code
            , @NonNull String cs_id
            , @NonNull String member_name
            , @NonNull String member_phone
            , @NonNull String member_role) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }
        if (StringUtil.isEmpty(member_name) || StringUtil.isEmpty(member_phone)) {
            return new SyncResult(1, "请输入成员名字或联系手机号");
        }
        if (StringUtil.isEmpty(member_role)) {
            return new SyncResult(1, "请选择成员角色");
        }

        CostProjectMemberEntity memberEntity = new CostProjectMemberEntity();
        if (memberEntity
                .where("member_phone", member_phone)
                .where("member_role", member_role)
                .whereBuilder("AND", "(", "project_code", "=", project_code, "")
                .whereBuilder("OR", "", "cs_id", "=", cs_id, ")")
                .exist()) {
            return new SyncResult(11, "已存在无需再添加");
        }

        memberEntity.project_code = project_code;
        memberEntity.cs_id = cs_id;
        memberEntity.member_name = member_name;
        memberEntity.member_phone = member_phone;
        memberEntity.member_role = member_role;
        memberEntity.member_role_text = RBRoleEntity.getInstance().getNameByCode(member_role, "");
        memberEntity.remark = "";
        memberEntity.create_time = TimeUtil.getTimestamp();
        memberEntity.update_time = memberEntity.create_time;
        if (memberEntity.insert() > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 删除成员
     *
     * @param project_code
     * @param cs_id
     * @param member_phone
     * @param member_role
     * @return
     */
    public ISyncResult del(@NonNull String project_code
            , @NonNull String cs_id
            , @NonNull String member_phone
            , @NonNull String member_role) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }
        if (StringUtil.isEmpty(member_phone)) {
            return new SyncResult(1, "请选择对应角色手机号");
        }
        if (StringUtil.isEmpty(member_role)) {
            return new SyncResult(1, "请选择成员角色");
        }

        int noquery = CostProjectMemberEntity.getInstance()
                .where("member_phone", member_phone)
                .where("member_role", member_role)
                .whereBuilder("AND", "(", "project_code", "=", project_code, "")
                .whereBuilder("OR", "", "cs_id", "=", cs_id, ")")
                .del();
        if (noquery > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }
}
