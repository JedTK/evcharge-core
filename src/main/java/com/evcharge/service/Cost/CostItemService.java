package com.evcharge.service.Cost;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.cost.CostCategoryEntity;
import com.evcharge.entity.cost.CostItemEntity;
import com.evcharge.entity.cost.CostProjectEntity;
import com.xyzs.cache.ECacheTime;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.DataService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncListResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;
import lombok.NonNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Time;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目成本 内容项 - 业务逻辑
 */
public class CostItemService {
    /**
     * 标签
     */
    protected final static String TAG = "项目成本内容项";

    // 单例模式的静态实例
    private volatile static CostItemService _this;

    /**
     * 获取单例
     * 采用双重检查锁定机制确保线程安全
     *
     * @return
     */
    public static CostItemService getInstance() {
        if (_this == null) {
            synchronized (CostItemService.class) {
                if (_this == null) _this = new CostItemService();
            }
        }
        return _this;
    }

    // region 业务逻辑函数

    /**
     * 获取成本项目的内容项列表
     *
     * @param project_code 项目编号
     * @param cs_id        站点编号
     * @param page         第几页
     * @param limit        每页显示多少
     * @return
     */
    public ISyncResult getList(@NonNull String project_code
            , @NonNull String cs_id
            , int page
            , int limit
    ) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }

        String countCacheKey = String.format("Cost:Items:%s%s:TotalCount", project_code, cs_id);
        String listCacheKey = String.format("Cost:Items:%s%s:%s_%s", project_code, cs_id, limit, page);

        try {
            // region 从缓存中读取总记录数
            long count = DataService.getMainCache().getInt(countCacheKey, -1);
            if (count != -1) {
                // 如果缓存中存在总数数据，则尝试从缓存中获取对应的分页数据
                List<Map<String, Object>> list = DataService.getMainCache().getList(listCacheKey);
                if (list != null && !list.isEmpty()) return new SyncListResult(count, page, limit, list);
            }
            // endregion

            count = CostItemEntity.getInstance()
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .count();
            if (count == 0) return new SyncResult(1, "");

            List<Map<String, Object>> list = CostItemEntity.getInstance()
                    .field("project_code,cs_id,item_name,c.category_code,category_name"
                            + ",spu_code,spec,unit,unit_price,quantity,total_amount,purchase_channel"
                            + ",invoice_type,invoice_tax_rate,remark,admin_id,c.update_time"
                    )
                    .alias("c")
                    .join(CostCategoryEntity.getInstance().theTableName(), "cc", "cc.category_code = c.category_code")
                    .whereOr("project_code", project_code)
                    .whereOr("cs_id", cs_id)
                    .page(page, limit)
                    .order("c.id")
                    .select();
            if (list == null || list.isEmpty()) return new SyncResult(1, "");

            DataService.getMainCache().set(countCacheKey, count, ECacheTime.MINUTE);
            DataService.getMainCache().setList(listCacheKey, list, ECacheTime.MINUTE);

            return new SyncListResult(count, page, limit, list);
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "获取成本项目的内容项列表发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 批量添加/修改 项目成本 内容项
     *
     * @param project_code 项目编码
     * @param item_list    内容项列表
     * @param admin_id     操作管理员id
     * @return
     */
    public ISyncResult batchPut(@NonNull String project_code
            , @NonNull String cs_id
            , @NonNull List<CostItemEntity> item_list
            , long admin_id
    ) {
        if (StringUtil.isEmpty(project_code) && StringUtil.isEmpty(cs_id)) {
            return new SyncResult(2, "请选择项目或站点");
        }

        CostProjectEntity projectEntity;
        if (!StringUtil.isEmpty(project_code)) {
            projectEntity = CostProjectService.getInstance().getByProjectCode(project_code);
        } else {
            projectEntity = CostProjectService.getInstance().getByCSId(cs_id);
        }
        if (projectEntity == null) {
            return new SyncResult(3, "无此项目或站点信息，请检查项目编号或站点编号");
        }
        //开启事务批量操作
        SyncResult r = CostProjectEntity.getInstance().beginTransaction(connection -> {
            for (CostItemEntity item : item_list) {
                // 计算总金额
                if (item.unit_price.compareTo(BigDecimal.ZERO) > 0 && item.quantity > 0) {
                    item.total_amount = item.unit_price.multiply(new BigDecimal(item.quantity));
                } else {
                    item.total_amount = BigDecimal.ZERO;
                }
                item.admin_id = admin_id;
                item.update_time = TimeUtil.getTimestamp();

                // 通过类别查询物料是否可以回收
                if (!StringUtil.isEmpty(item.category_code) && item.is_recyclable == 0) {
                    CostCategoryEntity costCategoryEntity = CostCategoryEntity.getInstance().getByCode(item.category_code);
                    if (costCategoryEntity != null) {
                        item.is_recyclable = costCategoryEntity.is_recyclable;
                    }
                }

                if (item.id == 0) {
                    //新增
                    item.project_code = projectEntity.project_code;
                    item.cs_id = projectEntity.cs_id;
                    item.create_time = TimeUtil.getTimestamp();

                    Map<String, Object> set_data = item.toMap();
                    if (item.insertTransaction(connection, set_data) == 0) {
                        LogsUtil.warn(TAG, "新增数据失败 - %s", JSONObject.from(set_data));
                        return new SyncResult(1, "");
                    }
                } else {
                    //修改：需要判断一下是否是这个项目的内容，如果不是则不进行修改
                    if (!item.where("id", item.id)
                            .whereOr("(", "project_code", "=", projectEntity.project_code, "")
                            .whereOr("", "cs_id", "=", projectEntity.cs_id, ")")
                            .exist()) {
                        continue;
                    }
                    Map<String, Object> set_data = item.toMap();
                    set_data.remove("id");
                    if (item.updateTransaction(connection, item.id, set_data) == 0) {
                        LogsUtil.warn(TAG, "更新数据失败 - %s - %s", item.id, JSONObject.from(set_data));
                        return new SyncResult(1, "");
                    }
                }
            }
            return new SyncResult(0, "");
        });
        if (r.code == 0) {
            String countCacheKey = String.format("Cost:Items:%s%s:TotalCount", project_code, cs_id);
            String listCacheKey = String.format("Cost:Items:%s%s:%s_%s", project_code, cs_id, 1, 200);

            DataService.getMainCache().del(countCacheKey);
            DataService.getMainCache().del(listCacheKey);

            CostProjectService.getInstance().syncCostTotalAmount(project_code, cs_id);
        }
        return r;
    }
    // endregion
}
