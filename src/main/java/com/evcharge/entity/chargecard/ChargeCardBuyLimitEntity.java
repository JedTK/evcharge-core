package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 充电卡购买限制-暂时使用;
 * <p>
 * 充电卡购买限制：如果表中没有相关数据，则默认不允许购买；如果存在记录，则允许购买，具体是否允许将依据 allow 字段的值进行判断，allow=0 表示不允许购买，allow=1 表示允许购买。
 *
 * @author : JED
 * @date : 2024-10-10
 */
public class ChargeCardBuyLimitEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电桩唯一编码/规则表达,*表示所有
     */
    public String unique_code;
    /**
     * 充电卡配置ID(待删除)
     */
    @Deprecated
    public long card_configId;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * 允许购买，0-不允许，1-允许（可反向不允许购买）
     */
    public int allow;
    /**
     * 优先级：值越小越高，最大为1
     */
    public int priority;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeCardBuyLimitEntity getInstance() {
        return new ChargeCardBuyLimitEntity();
    }

    /**
     * 获取允许购买的充电卡配置
     *
     * @param CSId  充电站ID
     * @param page  第几页
     * @param limit 每页限制
     */
    public SyncResult getBuyList(@NonNull String CSId, int page, int limit) {
        String[] allowBuySpuCodeList = getAllowSpuCodeList(CSId);
        if (allowBuySpuCodeList == null || allowBuySpuCodeList.length == 0) return new SyncResult(1, "");

        List<Map<String, Object>> list = ChargeCardConfigEntity.getInstance()
                .field("id,spu_code,cardName,subtitle,coverImage,price,dailyChargeTime,cardTypeId,typeId,countValue,describe")
                .whereIn("spu_code", allowBuySpuCodeList)
                .where("usageType", "user")
                .page(page, limit)
                .select();
        if (list == null || list.isEmpty()) return new SyncResult(1, "");
        return new SyncResult(0, "", list);
    }

    /**
     * 获取允许购买的spu_code
     */
    public String[] getAllowSpuCodeList(String CSId) {
        // 查询符合条件的数据列表
        List<ChargeCardBuyLimitEntity> list = this
                .cache(String.format("ChargeCardConfig:BuyLimit:%s", CSId))
                .whereIn("unique_code", new String[]{"*", CSId})
                .order("spu_code,allow,priority")
                .page(1, 1000)
                .selectList();

        // 用于存储允许购买和不允许购买的spu_code
        Map<String, Boolean> spuCodeMap = new HashMap<>();

        if (list != null && !list.isEmpty()) {
            for (ChargeCardBuyLimitEntity entity : list) {
                // 如果 unique_code 为 "*"，只考虑允许购买的情况
                if ("*".equals(entity.unique_code) || !StringUtil.hasLength(entity.unique_code)) {
                    if (entity.allow == 1) {
                        spuCodeMap.putIfAbsent(entity.spu_code, true);
                    }
                }
                // 如果 unique_code 为具体的 CSId，覆盖 "*" 的配置
                else if (CSId.equalsIgnoreCase(entity.unique_code)) {
                    spuCodeMap.put(entity.spu_code, entity.allow == 1);
                }
            }
        }

        // 过滤出允许购买的spu_code
        // 返回允许购买的spu_code数组
        return spuCodeMap.entrySet().stream()
                .filter(Map.Entry::getValue) // 仅保留值为true的项（允许购买）
                .map(Map.Entry::getKey).toArray(String[]::new);
    }
}