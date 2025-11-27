package com.evcharge.entity.agent.agent;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 代理站点表;
 *
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentToStationEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 站点id
     */
    public String CSId;
    /**
     * 固定分账金额
     */
    public BigDecimal split_amount ;
    /**
     * 分账比例
     */
    public BigDecimal split_rate ;
    /**
     * 备注
     */
    public String remark;
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
    public static AgentToStationEntity getInstance() {
        return new AgentToStationEntity();
    }


    /**
     * 绑定站点
     *
     * @param organizeCode 组织代码
     * @param CSIds        站点csid列表
     * @return
     */
    public SyncResult bind(String organizeCode, String CSIds,BigDecimal splitAmount,BigDecimal splitRate) {

        if (!StringUtils.hasLength(CSIds)) return new SyncResult(1, "站点ID不能为空");
        if (!StringUtils.hasLength(organizeCode)) return new SyncResult(1, "组织代码不能为空");

        String[] CSIdsArr = CSIds.split(",");
        return DataService.getMainDB().beginTransaction(connection -> {
            for (String CSId : CSIdsArr) {
                AgentToStationEntity agentToStationEntity = AgentToStationEntity.getInstance()
                        .cache(String.format("AgentToStation:check:%s", CSIds), 86400 * 1000)
                        .where("CSId", CSId)
                        .findEntity();
                if (agentToStationEntity != null) continue;
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("organize_code", organizeCode);
                data.put("CSId", CSId);
                data.put("split_amount", splitAmount);
                data.put("split_rate", splitRate);
                data.put("create_time", TimeUtil.getTimestamp());
                long id =  AgentToStationEntity.getInstance().name("AgentToStation").insertGetId(data);
                if (id == 0) return new SyncResult(1, "绑定站点失败");
            }
            return new SyncResult(0, "绑定成功");
        });


    }

    /**
     * 解绑站点
     *
     * @param organizeCode 组织代码
     * @param CSIds        站点csid列表
     * @return
     */
    public SyncResult unbind(String organizeCode, String CSIds) {

        if (!StringUtils.hasLength(CSIds)) return new SyncResult(1, "站点ID不能为空");
        if (!StringUtils.hasLength(organizeCode)) return new SyncResult(1, "组织代码不能为空");

        String[] CSIdsArr = CSIds.split(",");
        return DataService.getMainDB().beginTransaction(connection -> {
            for (String CSId : CSIdsArr) {
                AgentToStationEntity agentToStationEntity = this
                        .cache(String.format("AgentToStation:check:%s", CSIds), 86400 * 1000)
                        .where("CSId", CSId)
                        .findEntity();
                if (agentToStationEntity == null) continue;

                long id = AgentToStationEntity.getInstance()
                        .where("organize_code",organizeCode)
                        .where("CSId",CSId)
                        .delTransaction(connection);

                if (id != 0) return new SyncResult(1, "解绑站点失败");
                DataService.getMainCache().del(String.format("AgentToStation:check:%s", CSIds));
            }
            return new SyncResult(0, "解绑成功");
        });


    }
}