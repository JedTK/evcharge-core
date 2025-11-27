package com.evcharge.entity.chargestatsionproject;


import com.alibaba.fastjson2.JSONArray;
import com.evcharge.entity.admin.AdminBaseEntity;
import com.evcharge.entity.workflow.WorkFlowEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充电桩项目;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WFChargeStationProjectEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 名称
     */
    public String name;
    /**
     * 状态：-1-删除，0-初始化，1-进行中，2-项目完成，3-取消
     */
    public int status;
    /**
     * 省（省份）
     */
    public String province;
    /**
     * 市（城市
     */
    public String city;
    /**
     * 区（行政区划）
     */
    public String districts;
    /**
     * 街道/城镇
     */
    public String street;
    /**
     * 城市社区和乡村
     */
    public String communities;
    /**
     * 道路
     */
    public String roads;
    /**
     * 城市和农村地址
     */
    public String addresses;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 结构：0-无，1-棚，2-架
     */
    public int arch;
    /**
     * 充电位数量
     */
    public int totalSocket;
    /**
     * 场宽
     */
    public float fieldWidth;
    /**
     * 广告位数量
     */
    public int adSlotsCount;
    /**
     * 土地属性
     */
    public String landAttr;
    /**
     * 百米/业务车辆保有量
     */
    public int eBikeQuantity;
    /**
     * 供电方式：1-新装电表，2-园区/物业供电
     */
    public int powerSupply;
    /**
     * 是否分润：0-否，1-是
     */
    public int isShareProfit;
    /**
     * 预计工期
     */
    public int expectedWorkDay;
    /**
     * 竣工时间
     */
    public long done_time;
    /**
     * 收费标准配置
     */
    public long chargeStandardConfigId;
    /**
     * 充电时长配置
     */
    public long chargeTimeConfigId;
    /**
     * 停车收费配置ID
     */
    public long parkingConfigId;
    /**
     * 安全充电保险，0=不启用，1=启用
     */
    public int safeCharge;
    /**
     * 安全充电保险费用
     */
    public double safeChargeFee;
    /**
     * 创建者id
     */
    public long creater_id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 工作流id
     */
    public long workflow_id;
    /**
     * 当前工作流步骤
     */
    public int workflow_step;
    /**
     * 当前工作流步骤代码 弃用
     */
    @Deprecated
    public String current_step_code;
    /**
     * 工作流步骤代码
     */
    public String step_code;
    /**
     * 工作流日志
     */
    public String workflow_logs;
    /**
     * 来源，''为元气充内部，用户-user,街道-street
     */
    public String source;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;
    /**
     * CSId
     */
    public String CSId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WFChargeStationProjectEntity getInstance() {
        return new WFChargeStationProjectEntity();
    }

    /**
     * 通过项目id获取一个项目信息
     *
     * @param projectId 项目id
     * @return 项目信息
     */
    public WFChargeStationProjectEntity getWithProjectId(String projectId) {
        return getWithProjectId(projectId, true);
    }

    /**
     * 通过项目id获取一个项目信息
     *
     * @param projectId 项目id
     * @param inCache   是否优先从缓冲中获取
     * @return 项目信息
     */
    public WFChargeStationProjectEntity getWithProjectId(String projectId, boolean inCache) {
        if (inCache) this.cache(String.format("BaseData:WFChargeStationProject:%s", projectId));
        return this.where("projectId", projectId)
                .findEntity();
    }

    /**
     * 通过站点ID获取一个项目信息
     *
     * @param CSId 站点编码ID
     * @return 项目信息
     */
    public WFChargeStationProjectEntity getWithCSId(String CSId) {
        return getWithCSId(CSId, true);
    }

    /**
     * 通过项目id获取一个项目信息
     *
     * @param CSId    站点编码ID
     * @param inCache 是否优先从缓冲中获取
     * @return 项目信息
     */
    public WFChargeStationProjectEntity getWithCSId(String CSId, boolean inCache) {
        if (inCache) this.cache(String.format("BaseData:WFChargeStationProject:%s", CSId));
        return this.where("CSId", CSId)
                .findEntity();
    }

    /**
     * 通过项目id或站点ID获取一个项目信息
     *
     * @param projectId 项目Id
     * @param CSId      站点编码ID
     * @param inCache   是否优先从缓冲中获取
     * @return 项目信息
     */
    public WFChargeStationProjectEntity getWithProjectIdOrCSId(String projectId, String CSId, boolean inCache) {
        WFChargeStationProjectEntity entity = getWithProjectId(projectId, inCache);
        if (entity != null) return entity;

        entity = getWithCSId(CSId, inCache);
        return entity;
    }

    /**
     * 通过项目id获取工作流数据
     *
     * @param projectId 项目id
     * @return 工作流
     */
    public WorkFlowEntity getWorkFlowWithProjectId(String projectId) {
        return getWorkFlowWithProjectId(projectId, true);
    }

    /**
     * 通过项目id获取工作流数据
     *
     * @param projectId 项目id
     * @param inCache   是否优先从缓冲中获取
     * @return 工作流
     */
    public WorkFlowEntity getWorkFlowWithProjectId(String projectId, boolean inCache) {
        if (!StringUtils.hasLength(projectId)) return null;
        WFChargeStationProjectEntity wfChargeStationProjectEntity = getWithProjectId(projectId);
        if (wfChargeStationProjectEntity == null) return null;

        WorkFlowEntity workFlowEntity = new WorkFlowEntity();
        if (inCache) workFlowEntity.cache(String.format("WorkFlow:%s", wfChargeStationProjectEntity.workflow_id));
        workFlowEntity = workFlowEntity.findEntity(wfChargeStationProjectEntity.workflow_id);
        return workFlowEntity;
    }

    /**
     * 更新备注
     *
     * @param projectId   项目id
     * @param op_admin_id 操作管理员
     */
    public void updateRemark(String projectId, long op_admin_id, String remarkValue) {
        if (!StringUtils.hasLength(remarkValue)) return;

        Map<String, Object> data = this.field("id,projectId,remark")
                .where("projectId", projectId)
                .find();
        if (data == null || data.isEmpty()) return;

        String remarkStr = MapUtil.getString(data, "remark");
        JSONArray remark_json = null;
        if (StringUtils.hasLength(remark)) {
            remark_json = JSONArray.parseArray(remarkStr);
        }
        if (remark_json == null) remark_json = new JSONArray();

        AdminBaseEntity adminBaseEntity = AdminBaseEntity.getInstance().getWithId(op_admin_id);
        remark_json.add(new LinkedHashMap<>() {{
            put("remark", remarkValue);
            put("op_admin_id", op_admin_id);
            put("op_admin_name", adminBaseEntity.last_name + adminBaseEntity.first_name);
            put("create_time", TimeUtil.getTimestamp());
        }});
        data.put("remark", remark_json.toJSONString());
        this.where("projectId", projectId).update(data);
    }

    /**
     * 通过项目Id查询CSId
     *
     * @param projectId 项目ID
     * @return
     */
    public String getCSIdWithProjectId(String projectId) {
        return getCSIdWithProjectId(projectId, true);
    }

    /**
     * 通过项目Id查询CSId
     *
     * @param projectId 项目ID
     * @param inCache   是否优先从缓存中获得
     * @return
     */
    public String getCSIdWithProjectId(String projectId, boolean inCache) {
        if (!StringUtils.hasLength(projectId)) return "";
        if (inCache) this.cache(String.format("BaseData:WFChargeStationProject:%s:CSId", projectId));
        Map<String, Object> data = this.field("id,CSId,projectId")
                .where("projectId", projectId)
                .find();
        return MapUtil.getString(data, "CSId");
    }
}
