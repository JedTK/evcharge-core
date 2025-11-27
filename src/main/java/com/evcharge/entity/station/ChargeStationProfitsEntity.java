package com.evcharge.entity.station;


import com.evcharge.entity.chargestatsionproject.WFChargeStationProfitsInfoEntity;
import com.evcharge.entity.chargestatsionproject.WFChargeStationProjectEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分润信息配置;
 *
 * @author : Jay
 * @date : 2024-3-7
 */
@Deprecated
public class ChargeStationProfitsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目id
     */
    public long cs_id;
    /**
     * 分润角色 场地方/居间人(推荐人)
     */
    public int profit_role;
    /**
     * 分润类型
     */
    public int profit_type;
    /**
     * 渠道用户ID
     */
    public long channel_id;
    /**
     * 分润模式 1=月结电位分润、2=手动电量分润、3=实时电量分润、4=实时消费金额(计次充电、内购卡金额)百分比分润
     */
    public int profit_mode;
    /**
     * 收益单价，按单价收益时用
     */
    public double income_price;
    /**
     * 收益比率，按百分比收益时用
     */
    public double income_ratio;
    /**
     * 状态
     */
    public int status;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationProfitsEntity getInstance() {
        return new ChargeStationProfitsEntity();
    }


    /**
     * 从立项创建分润信息
     *
     * @param CSId
     * @param projectId
     */
    public void wfCreate(long CSId, String projectId) {
        WFChargeStationProjectEntity wfChargeStationProjectEntity = WFChargeStationProjectEntity.getInstance().getWithProjectId(projectId);

        if (wfChargeStationProjectEntity == null) return;

        List<WFChargeStationProfitsInfoEntity> list = WFChargeStationProfitsInfoEntity.getInstance()
                .where("project_id", projectId)
                .selectList();

        if (list.isEmpty()) return;

        for (WFChargeStationProfitsInfoEntity wfChargeStationProfitsInfoEntity : list) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("cs_id", CSId);
            data.put("profit_role", wfChargeStationProfitsInfoEntity.profit_role);
            data.put("profit_type", wfChargeStationProfitsInfoEntity.profit_type);
            data.put("channel_id", wfChargeStationProfitsInfoEntity.channel_id);
            data.put("profit_mode", wfChargeStationProfitsInfoEntity.profit_mode);
            data.put("income_price", wfChargeStationProfitsInfoEntity.income_price);
            data.put("income_ratio", wfChargeStationProfitsInfoEntity.income_ratio);
            data.put("status", wfChargeStationProfitsInfoEntity.status);
            data.put("create_time", TimeUtil.getTimestamp());
//            data.put("update_time",wfChargeStationProfitsInfoEntity.update_time);
            ChargeStationProfitsEntity.getInstance().insert(data);
        }
    }


}