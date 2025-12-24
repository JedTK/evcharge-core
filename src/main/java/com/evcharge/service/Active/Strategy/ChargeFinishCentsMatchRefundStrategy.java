package com.evcharge.service.Active.Strategy;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.station.ChargeOrderEntity;
import com.evcharge.entity.station.ChargeRefundOrderEntity;
import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.ACTStrategy;
import com.evcharge.service.Active.base.IACTStrategy;
import com.evcharge.service.popup.PopupService;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.StringUtil;
import com.xyzs.utils.TimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 充电结算“分位命中”退款策略
 * <p>
 * 策略目标
 * - 在“充电结算完成”这一触发点，根据订单金额的小数点后两位（00~99）进行命中判断。
 * - 命中后执行退款/免单（支持全额/固定/按比例）。
 * <p>
 * 核心规则
 * 1. 从订单提取某个金额字段作为基准金额（示例：totalAmount / payAmount / settlementAmount）。
 * 2. 取基准金额的小数点后两位（00~99），称为 cents。
 * 3. 若 cents 命中配置项 matchCents，则进入退款流程；否则直接结束。
 * <p>
 * 返回码语义约定（与活动引擎配合）
 * - code = 0  : 成功命中并执行完成（或 dryRun 命中但不执行）。
 * - code = -1 : 正常不参与/未命中分支，不需要写入活动日志（用于避免高频场景下日志膨胀）。
 * - code != 0 且 != -1 : 异常或配置问题，建议记录活动日志以便观测与排查。
 * <p>
 * 幂等与并发要求（必须满足）
 * - 上层即使通过日志或业务键做过幂等判定，在并发/重试场景仍可能重复进入策略。
 * - 退款执行必须实现“订单维度幂等”，常见手段：
 * 1) 使用订单字段 CAS：update order set refund_status=1 where refund_status=0
 * 2) 退款单表增加唯一键（orderSN + refundType），重复插入失败即视为已处理
 * 3) 退款接口内部保证幂等（建议仍做本地防重）
 */
@ACTStrategy(code = "STR_CHARGE_FINISH_CENTS_MATCH_REFUND", desc = "充电结算分位匹配退款策略")
public class ChargeFinishCentsMatchRefundStrategy implements IACTStrategy {

    @Override
    public ISyncResult execute(ACTContext ctx) throws Exception {

        /*
         * 0) 解析幂等业务键
         *
         * 约定：ctx.biz_key 为幂等业务键，本策略按“订单号 OrderSN”处理。
         * 若未来需要支持其他幂等键形式，可在此处统一扩展解析逻辑。
         */
        String orderSN = ctx.biz_key;
        if (StringUtil.isEmpty(orderSN)) {
            // 参数缺失属于调用方问题，通常需要记录以便排查调用链
            return new SyncResult(2, "停止执行：biz_key(OrderSN)为空");
        }

        /*
         * 1) 查询订单
         *
         * 高频触发场景下应确保 OrderSN 具备索引（唯一索引或普通索引），避免全表扫描。
         */
        ChargeOrderEntity orderEntity = ChargeOrderEntity.getInstance()
                .where("OrderSN", orderSN)
                .findEntity();
        if (orderEntity == null) {
            // 数据不存在可能是上游传参错误或订单尚未落库，属于需要观测的异常分支
            return new SyncResult(3, "停止执行：无法找到订单号");
        }

        /*
         * 2) 读取策略配置
         *
         * 约定：ctx.config.params_json 为 JSON 字符串，支持运营后台配置。
         * 常用字段示例：
         * - matchCents   : 命中分位列表（0~99），数组或字符串均可
         * - minAmount    : 最小参与金额（可选）
         * - maxAmount    : 最大参与金额（可选）
         * - refundType   : FULL / FIXED / PERCENT
         * - refundFixed  : 固定退款金额（refundType=FIXED）
         * - refundRate   : 退款比例（0~1，refundType=PERCENT）
         * - dryRun       : true 表示仅验证与计算，不执行真实退款（用于灰度/演练）
         */
        JSONObject cfgJson = getStrategyConfig(ctx);

        /*
         * 3) 解析命中分位集合 matchCents
         *
         * 若 matchCents 为空，属于配置错误，应返回可观测错误码（避免被当作“正常不参与”吞掉）。
         */
        Set<Integer> matchCents = parseMatchCents(cfgJson);
        if (matchCents.isEmpty()) {
            return new SyncResult(11, "活动配置错误：matchCents 为空");
        }

        /*
         * 4) 解析可选的金额过滤区间
         *
         * 用途：
         * - 过滤异常小额/异常数据，避免对账噪音
         * - 只让指定金额区间参与活动
         */
        BigDecimal minAmount = JsonUtil.getBigDecimal(cfgJson, "minAmount");
        BigDecimal maxAmount = JsonUtil.getBigDecimal(cfgJson, "maxAmount", new BigDecimal("10"));

        /*
         * 5) dryRun 开关
         *
         * true：仅返回命中信息与退款金额计算结果，不执行真实退款。
         * 适用于上线前灰度验证与日志观察。
         */
        boolean dryRun = JsonUtil.getBoolean(cfgJson, "dryRun", false);

        /*
         * 6) 订单前置校验
         *
         * 此处用于绑定真实业务语义（是否结算完成、是否可退款、用户归属是否一致等）。
         * 建议：
         * - 正常不参与：返回 code = -1
         * - 数据异常/需要排查：返回 code != 0 且 != -1
         */
        SyncResult preCheck = preCheckOrder(ctx, orderEntity);
        if (!preCheck.isSuccess()) {
            return new SyncResult(preCheck.code, preCheck.msg);
        }

        /*
         * 7) 提取订单基准金额
         *
         * 注意：此处示例直接使用 totalAmount；若需要可通过配置 amountField 动态选择字段。
         */
        BigDecimal amount = new BigDecimal(orderEntity.totalAmount).setScale(2, RoundingMode.DOWN);

        /*
         * 8) 金额区间过滤
         *
         * 不满足过滤条件属于“正常不参与”，返回 -1 以避免日志膨胀。
         */
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return new SyncResult(-1, String.format("未命中：金额小于%s", minAmount));
        }
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return new SyncResult(-1, String.format("未命中：金额大于%s", minAmount));
        }

        /*
         * 9) 计算分位 cents（00~99）
         *
         * 采用 RoundingMode.DOWN 截断到两位小数：
         * - 与多数支付展示/存储口径一致（两位小数）
         * - 避免四舍五入导致分位“跳变”
         */
        int cents = calcCents(amount);

        /*
         * 10) 命中判断
         *
         * 未命中属于高频“正常分支”，返回 -1，避免落活动日志造成 IO 与存储压力。
         */
        if (!matchCents.contains(cents)) {
            return new SyncResult(-1, "未命中：分位=" + cents);
        }

        /*
         * 11) 计算退款金额
         *
         * - FULL    ：退款金额=订单金额（免单）
         * - FIXED   ：退款金额=refundFixed
         * - PERCENT ：退款金额=订单金额*refundRate（0~1）
         *
         * 退款金额需满足：
         * - > 0
         * - 不超过订单金额（防止超退）
         */
        BigDecimal refundAmount = calcRefundAmount(cfgJson, amount);
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new SyncResult(11, "活动配置错误：退款金额<=0");
        }
        if (refundAmount.compareTo(amount) > 0) {
            refundAmount = amount;
        }

        /*
         * 12) 构造策略执行快照数据
         *
         * 该数据通常用于活动日志 extra_json：
         * - 命中分位、原始金额、计算出的退款金额、退款方式等
         * - 便于线上快速排查与复现
         */
        Map<String, Object> cb = new LinkedHashMap<>();
        cb.put("orderSN", orderSN);
        cb.put("amount", amount);
        cb.put("cents", cents);
        cb.put("matchCents", matchCents);
        cb.put("refundType", JsonUtil.getString(cfgJson, "refundType", "FULL"));
        cb.put("refundAmount", refundAmount);
        cb.put("dryRun", dryRun);

        /*
         * 13) dryRun：仅返回命中信息，不执行真实退款
         */
        if (dryRun) return new SyncResult(0, "命中但dryRun=true，未执行退款", cb);

        /*
         * 14) 执行退款（必须幂等）
         *
         * refundHandle 内必须保证同一订单不会重复退款，即使策略被并发触发或重试。
         * 若退款失败，返回非 0，通常应记录活动日志以便告警与排查。
         */
        SyncResult refundResult = refundHandle(orderEntity, refundAmount, "活动免单/退款：分位命中=" + cents);
        cb.put("refundResult", JSONObject.toJSONString(refundResult));
        if (!refundResult.isSuccess()) {
            return new SyncResult(20, "退款失败：" + refundResult.msg, cb);
        }

        // 调用弹窗API推送弹窗给用户: 数据库的popup_code调整与activity_code一样即可
        JSONObject var = new JSONObject();
        var.put("OrderSN", orderEntity.OrderSN);
        var.put("totalAmount", orderEntity.totalAmount);
        var.put("refundAmount", refundAmount);
        var.put("endTimeStr", TimeUtil.toTimeString(orderEntity.endTime));
        PopupService.getInstance().push(ctx.activity_code, ctx.uid, var, ctx.biz_key);

        return new SyncResult(0, "退款成功", cb);
    }

    /**
     * 读取策略参数配置
     * <p>
     * 数据来源约定：ctx.config.params_json
     * - 若 ctx/config/params_json 为空或解析失败，返回空对象，避免空指针。
     */
    private JSONObject getStrategyConfig(ACTContext ctx) {
        try {
            if (ctx == null || ctx.config == null) return new JSONObject();
            if (StringUtil.isEmpty(ctx.config.params_json)) return new JSONObject();
            return JSONObject.parseObject(ctx.config.params_json);
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    /**
     * 解析 matchCents（命中分位列表）
     * <p>
     * 支持两种配置形式：
     * 1) JSON 数组： [0, 1, 88]
     * 2) 字符串：   "00,01,88" 或 "0, 1, 88"
     * <p>
     * 返回：
     * - 0~99 的去重集合
     * - 遇到脏值（非数字/超范围）将忽略单项，避免影响整体策略可用性
     */
    private Set<Integer> parseMatchCents(JSONObject cfgJson) {
        Set<Integer> set = new HashSet<>();
        if (cfgJson == null) return set;

        Object v = cfgJson.get("matchCents");

        // 配置形式 1：JSON 数组
        if (v instanceof JSONArray) {
            JSONArray arr = (JSONArray) v;
            for (int i = 0; i < arr.size(); i++) {
                Integer n = arr.getInteger(i);
                if (n == null) continue;
                if (n >= 0 && n <= 99) set.add(n);
            }
            return set;
        }

        // 配置形式 2：字符串
        String s = cfgJson.getString("matchCents");
        if (!StringUtil.isEmpty(s)) {
            String[] parts = s.split("[,，\\s]+");
            for (String p : parts) {
                if (StringUtil.isEmpty(p)) continue;
                try {
                    int n = Integer.parseInt(p.trim());
                    if (n >= 0 && n <= 99) set.add(n);
                } catch (Exception ignore) {
                    // 忽略单项脏值
                }
            }
        }
        return set;
    }

    /**
     * 订单参与条件校验（需按真实业务字段实现）
     * <p>
     * 建议返回码语义：
     * - code = -1：正常不参与（例如未支付/未结算/不满足支付方式），不需要记录活动日志
     * - code != 0 且 != -1：数据异常/需要排查（例如用户不匹配、关键字段异常），建议记录活动日志
     */
    private SyncResult preCheckOrder(ACTContext ctx, ChargeOrderEntity order) {
        if (order.status != 2) return new SyncResult(-1, "充电订单还没结算");
        if (order.paymentTypeId != 1) return new SyncResult(-1, "仅余额支付订单参与活动");
        if (order.uid != ctx.uid) return new SyncResult(11, "订单与用户不匹配");
        return new SyncResult(0, "");
    }

    /**
     * 计算金额的小数点后两位（00~99）
     * <p>
     * 示例：
     * - 12.34  -> 34
     * - 12.00  -> 0
     * - 12.349 -> 34（先截断到两位）
     * <p>
     * 计算口径：
     * - setScale(2, DOWN) 截断两位
     * - *100 后对 100 取余得到 0~99
     */
    private int calcCents(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.DOWN);

        BigDecimal cents = scaled
                .multiply(new BigDecimal("100"))
                .remainder(new BigDecimal("100"))
                .abs();

        return cents.intValue();
    }

    /**
     * 计算退款金额
     * <p>
     * refundType：
     * - FULL    ：refundAmount = baseAmount
     * - FIXED   ：refundAmount = refundFixed
     * - PERCENT ：refundAmount = baseAmount * refundRate（refundRate 限制在 0~1）
     * <p>
     * 统一口径：
     * - 金额保留两位小数并向下截断（DOWN）
     */
    private BigDecimal calcRefundAmount(JSONObject cfgJson, BigDecimal baseAmount) {
        String refundType = cfgJson.getString("refundType");
        if (StringUtil.isEmpty(refundType)) refundType = "FULL";

        if ("FIXED".equalsIgnoreCase(refundType)) {
            BigDecimal fixed = cfgJson.getBigDecimal("refundFixed");
            if (fixed == null) fixed = BigDecimal.ZERO;
            return fixed.setScale(2, RoundingMode.DOWN);
        }

        if ("PERCENT".equalsIgnoreCase(refundType)) {
            BigDecimal rate = cfgJson.getBigDecimal("refundRate");
            if (rate == null) rate = BigDecimal.ONE;

            if (rate.compareTo(BigDecimal.ZERO) < 0) rate = BigDecimal.ZERO;
            if (rate.compareTo(BigDecimal.ONE) > 0) rate = BigDecimal.ONE;

            return baseAmount.multiply(rate).setScale(2, RoundingMode.DOWN);
        }

        return baseAmount.setScale(2, RoundingMode.DOWN);
    }

    /**
     * 执行退款（或免单）
     * <p>
     * 幂等要求：
     * - 同一订单重复触发，不允许重复退款。
     * <p>
     * 备注：
     * - 当前实现示例：通过退款单存在性进行防重（存在则不再发起退款）。
     * - 在高并发场景，建议使用更强的幂等手段（唯一键/CAS）以规避竞态条件。
     */
    private SyncResult refundHandle(ChargeOrderEntity order, BigDecimal refundAmount, String reason) {
        if (ChargeRefundOrderEntity.getInstance()
                .where("OrderSN", order.OrderSN)
                .where("uid", order.uid)
                .exist()) return new SyncResult(11, "订单已退款");
        SyncResult r = ChargeRefundOrderEntity.getInstance().refund(order.OrderSN, refundAmount.doubleValue(), reason);
        if (r.code == 0) {
            ChargeOrderEntity.getInstance()
                    .where("OrderSN", order.OrderSN)
                    .update(new LinkedHashMap<>() {{
                        put("discountAmount", refundAmount);
                    }});
        }
        return r;
    }
}
