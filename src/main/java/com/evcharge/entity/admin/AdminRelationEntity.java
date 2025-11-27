package com.evcharge.entity.admin;


import com.evcharge.entity.sys.RelationGroupEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 管理员关系;
 *
 * @author : JED
 * @date : 2022-11-21
 */
public class AdminRelationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员ID
     */
    public long from_aid;
    /**
     * 管理员ID
     */
    public long to_aid;
    /**
     * 分组ID
     */
    public long groupId;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminRelationEntity getInstance() {
        return new AdminRelationEntity();
    }

    /**
     * 通过分组代码添加一段关系
     *
     * @param from_aid  管理员1
     * @param to_aid    管理员2
     * @param groupCode 关系分组代码
     * @return
     */
    public SyncResult add(long from_aid, long to_aid, String groupCode) {
        RelationGroupEntity groupEntity = RelationGroupEntity.getInstance()
                .cache(String.format("BaseData:RelationGroup:%s", groupCode))
                .where("code", groupCode)
                .findModel();
        if (groupEntity == null || groupEntity.id == 0) return new SyncResult(300, "不存在关系code");

        return add(from_aid, to_aid, groupEntity.id);
    }

    /**
     * 通过分组ID添加一段关系
     *
     * @param from_aid 管理员1
     * @param to_aid   管理员2
     * @param groupId  关系分组
     * @return
     */
    public SyncResult add(long from_aid, long to_aid, long groupId) {
        //检查是否存在关系
        if (this.where("from_aid", from_aid).where("to_aid", to_aid).where("groupId", groupId).exist()) {
            return new SyncResult(0, "");
        }

        this.from_aid = from_aid;
        this.to_aid = to_aid;
        this.groupId = groupId;
        this.create_time = TimeUtil.getTimestamp();
        this.id = this.insertGetId();
        if (this.id > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 获取关系数据
     *
     * @param from_aid  用户id
     * @param groupCode 关系分组code
     * @return
     */
    public List<Map<String, Object>> getList(long from_aid, String groupCode, int page, int limit) {
        RelationGroupEntity groupEntity = RelationGroupEntity.getInstance()
                .cache(String.format("BaseData:RelationGroup:%s", groupCode))
                .where("code", groupCode)
                .findModel();
        if (groupEntity == null || groupEntity.id == 0) return new LinkedList<>();
        return getList(from_aid, groupEntity.id, page, limit);
    }

    /**
     * 获取关系数据
     *
     * @param from_aid 用户id
     * @param groupId  关系分组id
     * @return
     */
    public List<Map<String, Object>> getList(long from_aid, long groupId, int page, int limit) {
        List<Map<String, Object>> list = this.cache(String.format("Relation:Admin:%s:%s:List:%s_%s", from_aid, groupId, page, limit))
                .field("a.id,a.to_aid,b.account,b.last_name,b.first_name,b.gender,b.email,b.phone_num,b.avatar,b.remark")
                .alias("a")
                .join(AdminBaseEntity.getInstance().theTableName(), "b", "a.to_aid = b.id")
                .where("a.from_aid", from_aid)
                .where("groupId", groupId)
                .page(page, limit)
                .selectList();
        return list;
    }
}
