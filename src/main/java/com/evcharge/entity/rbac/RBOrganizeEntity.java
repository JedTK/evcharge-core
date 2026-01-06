package com.evcharge.entity.rbac;

import com.evcharge.entity.admin.AdminToOrganizeEntity;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;

/**
 * RBAC组织基础信息;组织角色一对多
 *
 * @author : JED
 * @date : 2022-8-29
 */
@TargetDB("evcharge_rbac")
public class RBOrganizeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 省份代码
     */
    public String province ;
    /**
     * 省份名称
     */
    public String city ;
    /**
     * 市代码
     */
    public String district ;
    /**
     * 市名称
     */
    public String street ;
    /**
     * 区代码
     */
    public String communities ;
    /**
     * 统一社会信用代码
     */
    public String unified_social_credit_code ;
    /**
     * 区名称
     */
    public String roads ;
    /**
     * 地址
     */
    public String address ;
    /**
     * 描述
     */
    public String describe ;
    /**
     * 组织机构名称
     */
    public String name;
    /**
     * 组织编码
     */
    public String code;
    /**
     * 机构类型：1=个人，2=公司，3=系统
     */
    public int type_id;
    /**
     * 上级ID
     */
    public long parent_id;


    /**
     * 备注
     */
    public String remark;
    /**
     * 组织编码
     */
    public String home_url;
    /**
     * 创建者id
     */
    public long creator_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    public static RBOrganizeEntity getInstance() {
        return new RBOrganizeEntity();
    }

    /**
     * 创建组织
     *
     * @param connection        事务链接
     * @param copy_organized_Id 需要复制的原组织ID信息，主要复制对应的职位
     * @param organized_name    组织名称
     * @return
     */
    public SyncResult createOrganizeAndJobTransaction(Connection connection, long copy_organized_Id, String organized_name) {
        try {
            RBOrganizeEntity organizeEntity = new RBOrganizeEntity();
            organizeEntity.name = organized_name;
            organizeEntity.code = "";
            organizeEntity.parent_id = 0;
            organizeEntity.remark = "";
            organizeEntity.id = organizeEntity.insertGetIdTransaction(connection);

            List<RBOrganizeToJobEntity> list = RBOrganizeToJobEntity.getInstance().where("organize_id", copy_organized_Id).selectList();
            for (RBOrganizeToJobEntity nd : list) {
                nd.organize_id = organizeEntity.id;
                nd.id = 0;
                if (nd.insertTransaction(connection) == 0) return new SyncResult(101, "复制信息出错");
            }
            return new SyncResult(0, "", organizeEntity);
        } catch (Exception e) {
            LogsUtil.error(e, "", "");
        }
        return new SyncResult(1, "");
    }

//    /**
//     * 获取管理员所在组织信息ID
//     *
//     * @param admin_id 管理员ID
//     * @return
//     */
//    public long getOrganizeIdWithAdminId(long admin_id) {
//        RBOrganizeEntity organizeEntity = RBOrganizeEntity.getInstance().getWithAdminId(admin_id);
//        if (organizeEntity == null || organizeEntity.id == 0) return 0;
//        return organizeEntity.id;
//    }
//
//    /**
//     * 获取管理员所在组织信息
//     *
//     * @param admin_id 管理员ID
//     * @return
//     */
//    public RBOrganizeEntity getWithAdminId(long admin_id) {
//        AdminToOrganizeEntity adminToOrganizeEntity = AdminToOrganizeEntity.getInstance()
//                .cache(String.format("Admin:%s:OrganizeId", admin_id))
//                .where("admin_id", admin_id)
//                .findEntity();
//        if (adminToOrganizeEntity == null || adminToOrganizeEntity.id == 0) return null;
//
//        return RBOrganizeEntity.getInstance()
//                .cache(String.format("Organize:%s:Detail", adminToOrganizeEntity.organize_id))
//                .findEntity(adminToOrganizeEntity.organize_id);
//    }
//
    /**
     * 根据组织id查询创建者id
     *
     * @param organize_id
     * @return
     */
    public long getCreatorIdWithOrganizeId(long organize_id) {
        RBOrganizeEntity organizeEntity = RBOrganizeEntity.getInstance()
                .cache(String.format("Organize:%s:Detail", organize_id))
                .findEntity(organize_id);
        if (organizeEntity == null || organizeEntity.id == 0) return 0;
        return organizeEntity.creator_id;
    }

//    /**
//     * 通过组织查询绑定的充电桩id数组
//     *
//     * @param organize_id 组织id
//     * @return
//     */
//    public long[] getChargeStationIdWithOrganizeId(long organize_id) {
//        return getChargeStationIdWithOrganizeId(organize_id, false);
//    }
//
//    /**
//     * 通过组织查询绑定的充电桩id数组
//     *
//     * @param organize_id 组织id
//     * @return
//     */
//    public long[] getChargeStationIdWithOrganizeId(long organize_id, boolean inCache) {
//        RBOrganizeToChargeStationEntity entity = RBOrganizeToChargeStationEntity.getInstance();
//        if (inCache) entity.cache("Organize:%s:ChargeStation:Id", organize_id);
//        return entity.field("CSId")
//                .where("organize_id", organize_id)
//                .selectForLongArray("CSId");
//    }
}
