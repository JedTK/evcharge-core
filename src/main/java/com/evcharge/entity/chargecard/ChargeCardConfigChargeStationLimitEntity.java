package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 该类用于定义充电卡配置和充电桩之间的限制关系实体。
 * 每个实例代表一条关于某张充电卡在某个或某类充电桩上的限制信息。
 * 通过该类可以查询特定充电桩与特定充电卡配置之间的使用限制。
 *
 * <p>
 * 主要字段包括：
 * - 充电桩唯一编码（可以是具体编码，也可以是通配符 *）
 * - 充电卡配置ID
 * - 是否允许使用
 * - 优先级
 * </p>
 *
 * <p>该类继承自 {@link com.xyzs.entity.BaseEntity}，具备基础的数据库操作能力。</p>
 *
 * @author : JED
 * @date : 2024-10-9
 */
public class ChargeCardConfigChargeStationLimitEntity extends BaseEntity implements Serializable {

    //region -- 实体类属性 --
    /**
     * 实体的唯一ID，用于标识一条充电卡配置与充电桩限制关系。
     */
    public long id;

    /**
     * 充电桩的唯一编码，表示该配置适用于哪个充电桩。
     * 可以是具体的充电桩编码，也可以是 * 号通配符，表示对所有充电桩适用。
     */
    public String unique_code;

    /**
     * 充电卡配置的ID，表示这条限制关系对应的充电卡配置。（待删除）
     */
    @Deprecated
    public long card_configId;

    /**
     * spu编码
     */
    public String spu_code;

    /**
     * 是否允许使用，0 表示不允许，1 表示允许。
     * 当该字段为 0 时，充电卡无法在指定充电桩使用；为 1 时，则允许使用。
     */
    public int allow;

    /**
     * 优先级字段，值越小优先级越高，最小为 1。
     * 在多个限制规则冲突时，优先级较高的规则会被优先使用。
     */
    public int priority;
    //endregion

    /**
     * 获取该类的一个新实例。
     * <p>该方法可以用于快速创建一个新的 {@link ChargeCardConfigChargeStationLimitEntity} 对象。</p>
     *
     * @return 一个新的 {@link ChargeCardConfigChargeStationLimitEntity} 对象
     */
    public static ChargeCardConfigChargeStationLimitEntity getInstance() {
        return new ChargeCardConfigChargeStationLimitEntity();
    }

    /**
     * 根据充电桩ID和充电卡配置ID查询是否有使用限制。
     * <p>
     * 如果不指定 `inCache` 参数，默认从缓存中获取。
     * 该方法会按优先级查询相关的限制规则，并返回是否允许使用。
     * </p>
     *
     * @param CSId          充电桩的唯一ID
     * @param card_configId 充电卡配置的ID
     * @return true 表示允许使用该充电桩，false 表示不允许使用
     */
    public boolean isAllow(String CSId, long card_configId) {
        return isAllow(CSId, card_configId, true);
    }

    /**
     * 根据充电桩ID和充电卡配置ID查询是否有使用限制。
     * <p>
     * 该方法首先从缓存中查询数据，如果 `inCache` 参数为 true。
     * 然后根据 `card_configId` 查询所有相关的限制规则，并根据规则的优先级判断是否允许使用。
     * </p>
     *
     * @param CSId          充电桩的唯一ID
     * @param card_configId 充电卡配置的ID
     * @param inCache       是否从缓存中获取数据
     * @return true 表示允许使用该充电桩，false 表示不允许使用
     */
    public boolean isAllow(String CSId, long card_configId, boolean inCache) {
        if (inCache) this.cache(String.format("ChargeCardConfig:ChargeStationLimit:%s", card_configId));
        this.where("card_configId", card_configId)
                .order("priority");
        List<Map<String, Object>> list = this.select();
        // 如果没有找到任何限制规则，则默认允许使用
        if (list == null || list.isEmpty()) return true;

        boolean allow = false;
        for (Map<String, Object> data : list) {
            String unique_code = MapUtil.getString(data, "unique_code");
            int allowInt = MapUtil.getInt(data, "allow");  // 0 表示不允许，1 表示允许

            // 如果 unique_code 是 "*"，表示对所有充电桩有效，依据 allow 字段确定是否允许
            if (!StringUtil.hasLength(unique_code) || "*".equals(unique_code)) {
                allow = allowInt == 1;
            }

            // 如果 unique_code 与传入的 CSId 匹配，则根据 allow 字段确定是否允许
            if (CSId.equalsIgnoreCase(unique_code)) {
                allow = allowInt == 1;
                break;
            }
        }
        return allow;
    }
}