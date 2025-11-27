package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 充电桩-充电卡限制;
 * 当数据不存在时：不进行限制
 * 当数据存在时：检查allow值是否允许（这样做主要可以单独控制几个站点是否允许）
 *
 * @author : JED
 * @date : 2023-12-7
 */
@Deprecated
public class ChargeStationToChargeCardLimitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩唯一编号，新增
     */
    public String CSId;
    /**
     * 充电卡配置ID
     */
    public long cardConfigId;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationToChargeCardLimitEntity getInstance() {
        return new ChargeStationToChargeCardLimitEntity();
    }

    /**
     * 通过充电桩ID和充电卡配置ID查询限制
     *
     * @param CSId         充电桩ID
     * @param cardConfigId 充电卡配置ID
     * @return 返回数据表示有限制，不允许使用
     */
    public boolean isAllow(String CSId, long cardConfigId) {
        return isAllow(CSId, cardConfigId, true);
    }

    /**
     * 通过充电桩ID和充电卡配置ID查询限制
     *
     * @param CSId         充电桩ID
     * @param cardConfigId 充电卡配置ID
     * @param inCache      有限从缓存中获取
     * @return true-允许使用，false-不允许使用
     */
    public boolean isAllow(String CSId, long cardConfigId, boolean inCache) {
        if (inCache) this.cache(String.format("ChargeStation:%s:ChargeCardLimit:%s", CSId, cardConfigId));
        this.where("CSId", CSId).where("cardConfigId", cardConfigId);
        Map<String, Object> data = this.find();
        return data == null || data.isEmpty();
    }
}
