package com.evcharge.service.Cost;

import com.evcharge.entity.chargestatsionproject.WFChargeStationProjectEntity;
import com.evcharge.entity.cost.CostItemEntity;
import com.evcharge.entity.cost.CostProjectEntity;
import com.evcharge.entity.cost.CostProjectMemberEntity;
import com.evcharge.entity.station.ChargeStationEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 项目成本 - 业务逻辑
 */
public class CostProjectService {

    /**
     * 标签
     */
    protected final static String TAG = "项目成本系统";

    // 单例模式的静态实例
    private volatile static CostProjectService _this;

    /**
     * 获取单例
     * 采用双重检查锁定机制确保线程安全
     *
     * @return
     */
    public static CostProjectService getInstance() {
        if (_this == null) {
            synchronized (CostProjectService.class) {
                if (_this == null) _this = new CostProjectService();
            }
        }
        return _this;
    }

    // region 实体类函数

    /**
     * 通过 项目编码 获取 项目成本信息
     *
     * @param project_code 项目编码
     * @return
     */
    public CostProjectEntity getByProjectCode(@NonNull String project_code) {
        return getByProjectCode(project_code, true);
    }

    /**
     * 通过 项目编码 获取 项目成本信息
     *
     * @param project_code 项目编码
     * @param inCache      是否优先从缓存中获取
     * @return
     */
    public CostProjectEntity getByProjectCode(@NonNull String project_code, boolean inCache) {
        if (StringUtil.isEmpty(project_code)) return null;

        CostProjectEntity costProjectEntity = new CostProjectEntity();
        if (inCache) costProjectEntity.cache(String.format("BaseData:CostProject:%s", project_code));
        return costProjectEntity.where("project_code", project_code)
                .findEntity();
    }

    /**
     * 通过 充电桩编码 获取 项目成本信息
     *
     * @param cs_id 充电桩编码
     * @return
     */
    public CostProjectEntity getByCSId(@NonNull String cs_id) {
        return getByCSId(cs_id, true);
    }

    /**
     * 通过 充电桩编码 获取 项目成本信息
     *
     * @param cs_id   充电桩编码
     * @param inCache 是否优先从缓存中获取
     * @return
     */
    public CostProjectEntity getByCSId(@NonNull String cs_id, boolean inCache) {
        if (StringUtil.isEmpty(cs_id)) return null;

        CostProjectEntity costProjectEntity = new CostProjectEntity();
        if (inCache) costProjectEntity.cache(String.format("BaseData:CostProject:%s", cs_id));
        return costProjectEntity.where("cs_id", cs_id)
                .findEntity();
    }

    // endregion

    // region 业务逻辑

    /**
     * 创建 项目成本
     *
     * @param project_code 项目编码
     * @param cs_id        充电桩编码
     * @param creator_id   创建者id
     * @return
     */
    public ISyncResult create(@NonNull String project_code
            , @NonNull String cs_id
            , String remark
            , long creator_id) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }

        try {
            if (CostProjectEntity.getInstance()
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .exist()) {
                return new SyncResult(11, "项目成本已存在，无需再创建");
            }

            // 查询项目信息
            WFChargeStationProjectEntity wfChargeStationProjectEntity = WFChargeStationProjectEntity
                    .getInstance()
                    .getWithProjectIdOrCSId(project_code, cs_id, true);
            if (wfChargeStationProjectEntity == null) {
                return new SyncResult(3, "无此项目资料，请检查项目编码是否正确");
            }

            CostProjectEntity projectEntity = CostProjectEntity.getInstance();
            projectEntity.project_code = wfChargeStationProjectEntity.projectId;
            projectEntity.cs_id = wfChargeStationProjectEntity.CSId;
            projectEntity.creator_id = creator_id;
            projectEntity.status = 1;
            projectEntity.create_time = TimeUtil.getTimestamp();
            projectEntity.update_time = TimeUtil.getTimestamp();
            projectEntity.cost_total_amount = BigDecimal.ZERO;

            // 查询充电桩信息
            ChargeStationEntity chargeStationEntity = ChargeStationEntity.getInstance().getWithCSId(cs_id);
            if (chargeStationEntity == null) return new SyncResult(4, "无此站点资料，请检查站点编码是否正确");

            projectEntity.project_name = chargeStationEntity.name;
            projectEntity.organize_code = chargeStationEntity.organize_code;
            projectEntity.platform_code = chargeStationEntity.platform_code;
            projectEntity.remark = remark;
            projectEntity.id = projectEntity.insertGetId();
            if (projectEntity.id > 0) {

                // 插入其他代码
                return new SyncResult(0, "");
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s-%s] 创建项目成本发生错误", project_code, cs_id);
        }
        return new SyncResult(1, "");
    }

    /**
     * 创建 项目成本
     *
     * @param project_code      项目编码（project_code与cs_id必需传递一个值才可以查询到对应的资料）
     * @param cs_id             充电桩编码
     * @param creator_id        创建者id
     * @param copy_project_code (可选) 需要复制内容的项目编号（copy_project_code与copy_cs_id必需有一个值才可以复制）
     * @param copy_cs_id        （可选）需要复制内容的站点编号
     * @return
     */
    public ISyncResult createFromCopy(@NonNull String project_code
            , @NonNull String cs_id
            , long creator_id
            , @NonNull String copy_project_code
            , @NonNull String copy_cs_id
            , String remark
    ) {
        ISyncResult r = create(project_code, cs_id, remark, creator_id);
        if (r.isSuccess()) {
            List<CostItemEntity> item_list = CostItemEntity.getInstance()
                    .field("item_name,category_code,spu_code,spec,unit,unit_price,purchase_channel,invoice_type,invoice_tax_rate,is_recyclable,remark")
                    .whereOr("project_code", copy_project_code)
                    .whereOr("cs_id", copy_cs_id)
                    .selectList();
            if (item_list == null || item_list.isEmpty()) {
                LogsUtil.warn(TAG, "无法复制项目[(%s)%s]的内容", copy_project_code, copy_cs_id);
                return new SyncResult(0, "");
            }
            CostItemService.getInstance().batchPut(project_code, cs_id, item_list, creator_id);
            return new SyncResult(0, "");
        }
        return new SyncResult(1, "");
    }

    /**
     * 删除项目
     *
     * @param project_code
     * @param cs_id
     * @return
     */
    public ISyncResult del(@NonNull String project_code, @NonNull String cs_id) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }

        if (CostProjectEntity.getInstance()
                .whereOr("project_code", project_code)
                .whereOr("cs_id", cs_id)
                .update(new LinkedHashMap<>() {{
                    put("status", 0);
                }}) > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 修改项目
     *
     * @param project_code
     * @param cs_id
     * @return
     */
    public ISyncResult update(@NonNull String project_code
            , @NonNull String cs_id
            , @NonNull String project_name
            , @NonNull String remark) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }

        if (CostProjectEntity.getInstance()
                .whereOr("project_code", project_code)
                .whereOr("cs_id", cs_id)
                .update(new LinkedHashMap<>() {{
                    put("project_name", project_name);
                    put("remark", remark);
                }}) > 0) return new SyncResult(0, "");
        return new SyncResult(1, "");
    }

    /**
     * 获取成本项目列表
     *
     * @param search_text 搜索文本
     * @param page        第几页
     * @param limit       每页显示多少
     * @return
     */
    public ISyncResult getList(String search_text
            , int page
            , int limit
    ) {
        String cacheKey = common.md5(search_text);

        try {
            String cs_table = ChargeStationEntity.getInstance().theTableName();
            String searchTextPattern = "";
            if (!StringUtil.isEmpty(search_text)) searchTextPattern = String.format("%%%s%%", search_text);

            CostProjectEntity countEntity = new CostProjectEntity();
            countEntity.cache(String.format("Cost:ProjectList:%s:TotalCount", cacheKey))
                    .alias("c")
                    .join(cs_table, "cs", "cs.CSId = c.cs_id")
                    .where("c.status", ">", 0);

            if (!StringUtil.isEmpty(search_text)) {
                countEntity.whereBuilder("AND", "(", "c.project_code", "like", searchTextPattern, "")
                        .whereBuilder("OR", "c.project_name", "like", searchTextPattern)
                        .whereBuilder("OR", "c.cs_id", "like", searchTextPattern)
                        .whereBuilder("OR", "c.remark", "like", searchTextPattern)
                        .whereBuilder("OR", "c.organize_code", "like", searchTextPattern)
                        .whereBuilder("OR", "c.platform_code", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.name", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.stationNumber", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.province", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.city", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.district", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.street", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.communities", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.roads", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.address", "like", searchTextPattern, ")");
            }
            long count = countEntity.count();
            if (count == 0) return new SyncResult(1, "");

            CostProjectEntity listEntity = new CostProjectEntity();
            listEntity.field("project_code,cs_id,project_name,c.status,c.remark,cost_total_amount,c.create_time,c.update_time")
                    .cache(String.format("Cost:ProjectList:%s:%s_%s", cacheKey, limit, page))
                    .alias("c")
                    .join(cs_table, "cs", "cs.CSId = c.cs_id")
                    .where("c.status", ">", 0)
                    .page(page, limit)
                    .order("c.create_time");
            if (!StringUtil.isEmpty(search_text)) {
                listEntity.whereBuilder("AND", "(", "c.project_code", "like", searchTextPattern, "")
                        .whereBuilder("OR", "c.project_name", "like", searchTextPattern)
                        .whereBuilder("OR", "c.cs_id", "like", searchTextPattern)
                        .whereBuilder("OR", "c.remark", "like", searchTextPattern)
                        .whereBuilder("OR", "c.organize_code", "like", searchTextPattern)
                        .whereBuilder("OR", "c.platform_code", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.name", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.stationNumber", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.province", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.city", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.district", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.street", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.communities", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.roads", "like", searchTextPattern)
                        .whereBuilder("OR", "cs.address", "like", searchTextPattern, ")");
            }

            List<Map<String, Object>> list = listEntity.select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取成本项目列表");
        }
        return new SyncResult(1, "");
    }

    /**
     * 项目成本 详情
     *
     * @param project_code 项目编码
     * @param cs_id        站点编码
     * @return
     */
    public ISyncResult getDetail(@NonNull String project_code, @NonNull String cs_id) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }
        try {
            String cacheKey = common.md5(String.format("%s%s"
                    , project_code
                    , cs_id
            ));

            Map<String, Object> data = CostProjectEntity.getInstance()
                    .field("c.project_code,c.cs_id,c.project_name,c.status AS project_status"
                            + ",cs.name AS cs_name,cs.stationNumber,cs.station_attr,cs.arch,cs.ad_panel_count,cs.status AS cs_status"
                            + ",cs.station_level,cs.totalSocket,cs.mainImage,cs.online_time"
                            + ",cs.province,cs.city,cs.district,cs.street,cs.communities,cs.roads,cs.address"
                    )
                    .cache(String.format("Cost:Project:Detail:%s", cacheKey))
                    .alias("c")
                    .join(ChargeStationEntity.getInstance().theTableName(), "cs", "cs.CSId = c.cs_id")
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .find();
            if (data != null && !data.isEmpty()) return new SyncResult(0, "", data);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s-%s] 创建项目成本发生错误", project_code, cs_id);
        }
        return new SyncResult(1, "");
    }

    /**
     * 获取项目模板列表
     *
     * @param page
     * @param limit
     * @return
     */
    public ISyncResult getTemplateList(int page, int limit) {
        try {
            int count = CostProjectEntity.getInstance()
                    .cache("BaseData:CostProject:Template:Count")
                    .where("is_template", 1)
                    .where("status", ">", 0)
                    .count();
            if (count == 0) return new SyncResult(1, "");

            List<Map<String, Object>> list = CostProjectEntity.getInstance()
                    .field("project_code,cs_id,project_name,cost_total_amount")
                    .cache(String.format("BaseData:CostProject:Template:%s_%s", page, limit))
                    .where("is_template", 1)
                    .where("status", ">", 0)
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");
            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "获取项目模板列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 同步成本总金额
     *
     * @return
     */
    public void syncCostTotalAmount(@NonNull String project_code, @NonNull String cs_id) {
        try {
            BigDecimal cost_total_amount = CostItemEntity.getInstance()
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .sumGetBigDecimal("total_amount");

            CostProjectEntity.getInstance()
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .update(new LinkedHashMap<>() {{
                        put("cost_total_amount", cost_total_amount);
                    }});
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[%s-%s] 同步合计金额发生错误", project_code, cs_id);
        }
    }

    // endregion
}
