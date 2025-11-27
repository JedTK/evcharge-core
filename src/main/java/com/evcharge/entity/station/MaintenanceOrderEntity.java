package com.evcharge.entity.station;


import com.alibaba.fastjson2.JSONArray;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 维修订单;
 *
 * @author : JED
 * @date : 2023-7-10
 */
public class MaintenanceOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 充电桩id
     */
    public long CSId;
    /**
     * 问题类型：电力问题、网络连接问题、硬件故障
     */
    public String ProblemType;
    /**
     * 问题描述
     */
    public String ProblemDescription;
    /**
     * 严重性：0-低，1-普通，2-非常严重
     */
    public int Severity;
    /**
     * 状态：-1-删除，0-等待审核,1-审核不通过，2-处理中，3-完成
     */
    public int status;
    /**
     * 维修时间线，时间段的json数据
     */
    public String Timeline;
    /**
     * 维修结果
     */
    public String MaintenanceResult;
    /**
     * (可选)技术人员姓名
     */
    public String technicianName;
    /**
     * (可选)技术人员电话
     */
    public String technicianPhone;
    /**
     * (可选)电工姓名
     */
    public String electricianName;
    /**
     * (可选)电工联系电话
     */
    public String electricianPhone;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 维护时间
     */
    public long maintenance_time;
    /**
     * 更新时间戳
     */
    public long update_time;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 审核管理员id
     */
    public long review_admin_id;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static MaintenanceOrderEntity getInstance() {
        return new MaintenanceOrderEntity();
    }

    /**
     * 添加时间线操作
     *
     * @param OrderSN 订单号
     * @param content 内容
     * @return
     */
    public SyncResult addTimeline(String OrderSN, String content) {
        MaintenanceOrderEntity orderEntity = MaintenanceOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findModel();
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(3, "查询不到相关订单数据");

        return orderEntity.addTimeline(content);
    }

    /**
     * 添加时间线操作
     *
     * @param content 内容
     * @return
     */
    public SyncResult addTimeline(String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", TimeUtil.toTimeString());
        data.put("content", content);

        JSONArray array = new JSONArray();
        if (StringUtils.hasLength(this.Timeline)) {
            array = JSONArray.parseArray(this.Timeline);
            if (array == null) array = new JSONArray();
        }
        array.add(data);

        String timelineContent = array.toJSONString();
        if (this.update(this.id, new LinkedHashMap<>() {{
            put("Timeline", timelineContent);
            put("update_time", TimeUtil.getTimestamp());
        }}) > 0) return new SyncResult(0, "");
        return new SyncResult(1, "操作失败");
    }
}
