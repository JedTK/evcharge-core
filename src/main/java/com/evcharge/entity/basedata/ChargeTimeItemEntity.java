package com.evcharge.entity.basedata;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电时长配置项;
 *
 * @author : JED
 * @date : 2022-10-8
 */
public class ChargeTimeItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置ID
     */
    public long configId;
    /**
     * 显示的名称
     */
    public String name;
    /**
     * 充电时长，单位：秒
     */
    public int chargeTime;
    /**
     * 充满自停标识，0=否，1=是
     */
    public int chargeAutoStop;

    //endregion

    /**
     * 获得一个实例
     */
    public static ChargeTimeItemEntity getInstance() {
        return new ChargeTimeItemEntity();
    }

    /**
     * 通过充电时长项Id查询数据
     *
     * @param chargeTimeItemId 充电时长项
     */
    public ChargeTimeItemEntity getItemWithId(long chargeTimeItemId) {
        return ChargeTimeItemEntity.getInstance()
                .cache(String.format("ChargeTime:Item:%s", chargeTimeItemId))
                .findEntity(chargeTimeItemId);
    }

    /**
     * 优化阶梯式充电：自动重启时按照“首次选择时长 - 已充电时长”精确匹配剩余时长所需的档位
     * 时间：2025-09-28
     * <p>
     * 业务说明：
     * - 用户最初选择了一个充电时长（由 chargeTimeItemId 对应的档位给出，例如 4 小时）。
     * - 中途因故障/过载停充，系统准备“自动重启订单”时，应当根据已充时长，计算出“剩余应充时长”，
     * 并选择一个最合适的充电档位，确保累计充电总时长 ≈ 用户最初设定。
     * <p>
     * 规则：
     * 1）remaining = max(0, initial.chargeTime - order_charge_time)
     * 2）remaining <= 0 ：返回 0 表示无需重启（可按系统约定调整行为）
     * 3）在相同 configId 下，选择 chargeTime >= remaining 的最小档位
     * 4）若不存在 ≥ remaining 的档位：
     * 4.1 首先尝试使用 default_chargeTimeItemId（若其同属该 config 且存在）
     * 4.2 否则退到该 config 下“最大”档位，尽量贴近剩余时长
     * <p>
     * 注意：
     * - 本函数仅返回“建议的档位ID”，实际系统还应在创建/重启订单时，将“预期总时长=首次选择时长”写入订单元数据，
     * 以便后续再次重启时持续沿用该初始设定。
     *
     * @param chargeTimeItemId         用户首次选择的档位ID
     * @param order_charge_time        当前订单累计已充时长（单位与 chargeTimeItemEntity.chargeTime 保持一致：建议统一为秒/分钟）
     * @param default_chargeTimeItemId 无法精确匹配时的默认档位ID（可传首选默认）
     * @return 推荐用于自动重启的档位ID；返回 0 表示无需重启
     */
    public long selectChargeTimeItemOnAutoRestart(long chargeTimeItemId,
                                                  long order_charge_time,
                                                  long default_chargeTimeItemId) {
        // 1) 读取用户最初选择的档位
        ChargeTimeItemEntity initial = ChargeTimeItemEntity.getInstance().getItemWithId(chargeTimeItemId);
        if (initial == null) {
            // 初始档位都拿不到时，退到默认或 0
            return default_chargeTimeItemId > 0 ? default_chargeTimeItemId : 0L;
        }

        // 2) 计算剩余时长（不为负，并且剩余30分钟就不需要再重启了）
        long remaining = initial.chargeTime - order_charge_time;
        if (remaining <= 600) {
            // 已经满足或超过用户最初设定的总时长，无需重启
            return 0L;
        }

        // 3) 在同一 configId 下查找“最小且 >= remaining”的档位
        Map<String, Object> geRow = ChargeTimeItemEntity.getInstance()
                .field("id,chargeTime")
                .where("configId", initial.configId)
                .where("chargeTime", ">=", remaining)
                .order("chargeTime ASC")
                .limit(1)
                .find();

        if (geRow != null && !geRow.isEmpty()) {
            // 命中精确或上取整的档位
            return MapUtil.getLong(geRow, "id", default_chargeTimeItemId);
        }

        // 4) 若没有 >= remaining 的档位，先尝试默认档位（要求同配置且存在）
        if (default_chargeTimeItemId > 0) return default_chargeTimeItemId;

        // 5) 再退到该配置下的“最大档位”，尽量贴近剩余时长
        Map<String, Object> maxRow = ChargeTimeItemEntity.getInstance()
                .field("id,chargeTime")
                .where("configId", initial.configId)
                .order("chargeTime DESC")
                .limit(1)
                .find();
        if (maxRow != null && !maxRow.isEmpty()) {
            return MapUtil.getLong(maxRow, "id", 0L);
        }
        // 6) 兜底
        return 0L;
    }

    /**
     * 优化阶梯式充电（临近值优先）：
     * 在同一 configId 下，同时取到 floor(<=remaining) 与 ceil(>=remaining) 两个候选，
     * 选择与 remaining 绝对误差更小的档位；若等距，优先选择更短的（floor）。
     * <p>
     * 规则补充：
     * - 剩余时长 remaining = max(0, initial.chargeTime - order_charge_time)
     * - remaining <= THRESHOLD_MINUTES（如 30 分钟）：返回 0（无需重启）
     * - 候选选取顺序：
     * 1) 在同 config 下取 ceil 与 floor
     * 2) 同时存在：按“更接近”选；若等距，选 floor（更短）
     * 3) 仅有其一：直接用该候选
     * 4) 若都不存在，尝试 default_chargeTimeItemId（需同 config 且存在）
     * 5) 最后退回该 config 的最大档位
     */
    public long selectChargeTimeItemNearestOnAutoRestart(long chargeTimeItemId,
                                                         long order_charge_time,
                                                         long default_chargeTimeItemId) {
        // --- 常量、入参校验 ---
        final long THRESHOLD_MINUTES = 30; // 与注释保持一致（你之前代码是 600 秒）
        ChargeTimeItemEntity initial = ChargeTimeItemEntity.getInstance().getItemWithId(chargeTimeItemId);
        if (initial == null) {
            return default_chargeTimeItemId > 0 ? default_chargeTimeItemId : 0L;
        }

        // --- 计算 remaining（分钟/与库字段一致）---
        long remaining = initial.chargeTime - order_charge_time;
        if (remaining <= THRESHOLD_MINUTES) {
            return 0L; // 小于等于阈值，不再重启
        }

        // --- 取 ceil：同 configId，chargeTime >= remaining 的最小值 ---
        Map<String, Object> ceilRow = ChargeTimeItemEntity.getInstance()
                .field("id,chargeTime")
                .where("configId", initial.configId)
                .where("chargeTime", ">=", remaining)
                .order("chargeTime ASC")
                .limit(1)
                .find();

        // --- 取 floor：同 configId，chargeTime <= remaining 的最大值 ---
        Map<String, Object> floorRow = ChargeTimeItemEntity.getInstance()
                .field("id,chargeTime")
                .where("configId", initial.configId)
                .where("chargeTime", "<=", remaining)
                .order("chargeTime DESC")
                .limit(1)
                .find();

        boolean hasCeil = (ceilRow != null && !ceilRow.isEmpty());
        boolean hasFloor = (floorRow != null && !floorRow.isEmpty());

        if (hasCeil && hasFloor) {
            long ceilId = MapUtil.getLong(ceilRow, "id", 0L);
            long floorId = MapUtil.getLong(floorRow, "id", 0L);
            long ceilTime = MapUtil.getLong(ceilRow, "chargeTime", Long.MAX_VALUE);
            long floorTime = MapUtil.getLong(floorRow, "chargeTime", Long.MIN_VALUE);

            long diffCeil = Math.abs(ceilTime - remaining);
            long diffFloor = Math.abs(remaining - floorTime);

            if (diffFloor < diffCeil) {
                return floorId;                // 更接近，取更短
            } else if (diffCeil < diffFloor) {
                return ceilId;                 // 更接近，取更长
            } else {
                return floorId;                // 等距，偏短，避免超出初始时长
            }
        } else if (hasCeil) {
            return MapUtil.getLong(ceilRow, "id", default_chargeTimeItemId);
        } else if (hasFloor) {
            return MapUtil.getLong(floorRow, "id", 0L);
        }

        // --- 两端都不存在：优先 default（需同 config 且存在）---
        if (default_chargeTimeItemId > 0) {
            ChargeTimeItemEntity def = ChargeTimeItemEntity.getInstance().getItemWithId(default_chargeTimeItemId);
            if (def != null && def.configId == initial.configId) {
                return default_chargeTimeItemId;
            }
        }

        // --- 兜底：用该 config 的最大档位 ---
        Map<String, Object> maxRow = ChargeTimeItemEntity.getInstance()
                .field("id,chargeTime")
                .where("configId", initial.configId)
                .order("chargeTime DESC")
                .limit(1)
                .find();
        if (maxRow != null && !maxRow.isEmpty()) {
            return MapUtil.getLong(maxRow, "id", 0L);
        }

        return 0L;
    }
}
