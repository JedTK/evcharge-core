package com.evcharge.service.Active;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.ACTConfigEntity;
import com.evcharge.entity.active.ACTLogsEntity;
import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.IAsyncListener;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import lombok.NonNull;

import java.util.*;

/**
 * 活动业务逻辑（Activity Trigger Service）
 * <p>
 * 设计目标
 * 1、将“活动触发”从主业务中解耦出来：主业务只负责在关键节点调用 trigger/triggerAsync，
 * 具体触发哪些活动、执行什么策略，全部由配置与策略注册决定。
 * 2、提供统一的触发入口：按场景(scene_code)找到绑定的活动列表，然后逐个执行。
 * 3、保证幂等：同一活动 + 同一场景 + 同一用户 + 同一 biz_key 只能执行一次。
 * <p>
 * 使用方式建议
 * - 在业务关键点调用 triggerAsync：例如充电结算后、充值回调后、进入某页面后等。
 * - biz_key 必须能表示一次业务事件的唯一性（订单号/充值单号/自定义唯一键），
 * 以保证幂等与避免重复发奖/重复退款/重复弹窗等严重问题。
 * <p>
 * 返回码约定（建议作为团队约定长期保持一致）
 * - code = 0  ：表示执行成功（或整体触发流程成功完成）
 * - code = -1 ：表示“未命中/不适用/跳过”，属于正常分支，一般不记日志、不告警
 * - code != 0 且 code != -1 ：表示异常或失败，通常需要记录日志方便排查
 * <p>
 * 日志策略
 * - 对于 -1：不作为错误，不输出 warn（避免日志污染）
 * - 对于 非 0 且非 -1：输出 warn
 * - 对于异常：输出 error，并返回 code=1 的兜底结果
 * <p>
 * 线程模型
 * - triggerAsync 使用线程池异步执行，不阻塞主业务流程（适合低耦合、不影响主流程结果的活动）
 * - trigger 为同步执行（适合你希望立即得知结果的场景，但仍建议活动尽量保持轻量）
 */
public class ACTService {

    private final static String TAG = "活动业务";

    private volatile static ACTService instance;

    /**
     * 获取服务实例
     * <p>
     * 注意
     * - 当前实现是“每次返回一个新对象”，不是单例缓存。
     * - 如果 ACTService 内部未来要保存状态（例如缓存、统计信息），建议改为真正单例或交给容器管理。
     */
    public static ACTService getInstance() {
        if (instance == null) {
            synchronized (ACTService.class) {
                if (instance == null) instance = new ACTService();
            }
        }
        return instance;
    }

    /**
     * 活动异步触发器
     * <p>
     * 语义
     * - 将 trigger(...) 放入线程池执行，避免阻塞主流程。
     * - 适用于：结算后抽奖、弹窗消息、发券、积分、站内信等“可延后执行”的动作。
     * <p>
     * 参数说明
     *
     * @param scene_code     场景编码，用于关联 ACTScene 配置（例如 charge_finish / recharge_callback）
     * @param uid            用户ID，必须 > 0
     * @param biz_key        幂等业务键：要求“同一场景下，同一用户的一次业务事件”保持唯一
     * @param params         策略执行所需参数，允许为 null（策略内部自行处理）
     * @param iAsyncListener 异步回调监听：这里统一 onResult(0, r)，r 为 trigger 的结果
     *                       <p>
     *                       注意事项
     *                       - 异步模式下，主流程无法感知活动执行失败；失败只能靠日志与活动日志表回溯。
     *                       - 如果活动包含“必须影响主流程结果”的强约束逻辑（通常不建议），不应使用 triggerAsync。
     */
    public void triggerAsync(@NonNull ACTSceneCode scene_code, long uid, String biz_key, JSONObject params, IAsyncListener iAsyncListener) {
        triggerAsync(scene_code.code(), uid, biz_key, params, iAsyncListener);
    }

    /**
     * 活动同步触发器（按场景触发多个活动）
     * <p>
     * 处理流程
     * 1、参数校验（scene_code、uid）
     * 2、根据 scene_code 查询该场景绑定的活动列表（ACTSceneService.getList）
     * 3、遍历每个活动编码 activity_code，逐个触发 triggerByActivateCode
     * 4、统计成功与失败次数（注意：-1 也会计入 error 计数，但不会 warn 打印）
     * <p>
     * 返回数据
     * - code=0：表示触发流程跑完（不代表每个活动都成功）
     * - data：包含 success / error 统计
     * <p>
     * 失败与异常
     * - 当某个活动返回 code=-1：表示正常跳过（例如活动未开始/已结束/未命中/已执行过）
     * - 当某个活动返回 code!=0 且 !=-1：记录 warn，并计入 error
     * - 发生未捕获异常：记录 error，并返回 code=1 的兜底结果
     * <p>
     * 参数说明
     *
     * @param scene_code 场景编码（必填）
     * @param uid        用户ID（必填，必须 > 0）
     * @param biz_key    幂等业务键（必填）
     * @param params     参数值（可为 null，建议传入以便策略取用）
     */
    public ISyncResult trigger(@NonNull ACTSceneCode scene_code, long uid, @NonNull String biz_key, JSONObject params) {
        return trigger(scene_code.code(), uid, biz_key, params);
    }

    /**
     * 活动异步触发器
     * <p>
     * 语义
     * - 将 trigger(...) 放入线程池执行，避免阻塞主流程。
     * - 适用于：结算后抽奖、弹窗消息、发券、积分、站内信等“可延后执行”的动作。
     * <p>
     * 参数说明
     *
     * @param scene_code     场景编码，用于关联 ACTScene 配置（例如 charge_finish / recharge_callback）
     * @param uid            用户ID，必须 > 0
     * @param biz_key        幂等业务键：要求“同一场景下，同一用户的一次业务事件”保持唯一
     * @param params         策略执行所需参数，允许为 null（策略内部自行处理）
     * @param iAsyncListener 异步回调监听：这里统一 onResult(0, r)，r 为 trigger 的结果
     *                       <p>
     *                       注意事项
     *                       - 异步模式下，主流程无法感知活动执行失败；失败只能靠日志与活动日志表回溯。
     *                       - 如果活动包含“必须影响主流程结果”的强约束逻辑（通常不建议），不应使用 triggerAsync。
     */
    public void triggerAsync(String scene_code, long uid, String biz_key, JSONObject params, IAsyncListener iAsyncListener) {
        ThreadUtil.getInstance().execute(() -> {
            ISyncResult r = trigger(scene_code, uid, biz_key, params);
            if (iAsyncListener != null) iAsyncListener.onResult(0, r);
        });
    }

    /**
     * 活动同步触发器（按场景触发多个活动）
     * <p>
     * 处理流程
     * 1、参数校验（scene_code、uid）
     * 2、根据 scene_code 查询该场景绑定的活动列表（ACTSceneService.getList）
     * 3、遍历每个活动编码 activity_code，逐个触发 triggerByActivateCode
     * 4、统计成功与失败次数（注意：-1 也会计入 error 计数，但不会 warn 打印）
     * <p>
     * 返回数据
     * - code=0：表示触发流程跑完（不代表每个活动都成功）
     * - data：包含 success / error 统计
     * <p>
     * 失败与异常
     * - 当某个活动返回 code=-1：表示正常跳过（例如活动未开始/已结束/未命中/已执行过）
     * - 当某个活动返回 code!=0 且 !=-1：记录 warn，并计入 error
     * - 发生未捕获异常：记录 error，并返回 code=1 的兜底结果
     * <p>
     * 参数说明
     *
     * @param scene_code 场景编码（必填）
     * @param uid        用户ID（必填，必须 > 0）
     * @param biz_key    幂等业务键（必填）
     * @param params     参数值（可为 null，建议传入以便策略取用）
     */
    public ISyncResult trigger(@NonNull String scene_code, long uid, @NonNull String biz_key, JSONObject params) {
        if (StringUtil.isEmpty(scene_code)) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少场景编码");
            return new SyncResult(2, "缺少场景编码");
        }
        if (uid <= 0) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少用户id");
            return new SyncResult(2, "缺少用户id");
        }
        try {
            // 读取该场景绑定的活动配置列表（通常来自 ACTScene 表）
            // 约定：list 中每一项至少包含 activity_code 字段
            List<Map<String, Object>> list = ACTSceneService.getInstance().getList(scene_code);
            if (list == null || list.isEmpty()) return new SyncResult(3, "当前场景无配置任何活动");

            int success = 0;
            int error = 0;
            for (Map<String, Object> data : list) {
                String activity_code = MapUtil.getString(data, "activity_code");
                ISyncResult r = triggerByActivateCode(activity_code, scene_code, uid, biz_key, params);
                if (r.isSuccess()) success++;
                else error++;
            }

            // 回调数据：用于业务侧了解本次触发执行情况
            Map<String, Object> cb_data = new LinkedHashMap<>();
            cb_data.put("success", success);
            cb_data.put("error", error);
            return new SyncResult(0, "", cb_data);
        } catch (Exception e) {
            // 触发器主流程兜底异常，避免活动系统影响主业务稳定性
            LogsUtil.error(e, TAG, "调用触发器过程中发生错误");
        }
        return new SyncResult(1, "");
    }

    /**
     * 按活动编码触发单个活动
     * <p>
     * 处理流程（严格按顺序）
     * 1、校验 activity_code
     * 2、读取活动配置 ACTConfig（含状态、时间范围、策略编码等）
     * 3、检查活动开关状态 cfg.status
     * 4、检查活动有效期（start_time/end_time 都不为 0 时才判断）
     * 5、幂等校验：同 activity_code + scene_code + uid + biz_key 只允许执行一次
     * 6、获取策略实现：ACTStrategyFactory.getStrategy(cfg.strategy_code)
     * 7、构建 ACTContext，并调用 strategy.execute(ctx)
     * 8、根据返回码决定是否写入活动日志表 ACTLogs
     * <p>
     * 日志写入规则
     * - r.code == -1：不写日志（因为属于正常“跳过/未命中”）
     * - r.code == 0 ：写日志（成功日志）
     * - r.code != 0 且 != -1：写日志（失败日志）并输出 warn（当前实现是先 warn 后返回）
     * <p>
     * 幂等说明（非常关键）
     * - exist(...) 的判断若依赖数据库，建议保证有唯一索引或强一致手段，否则高并发下可能重复执行：
     * 推荐在 ACTLogs 表建立唯一键 (activity_code, scene_code, uid, biz_key)
     * 然后 add(...) 时捕获 duplicate key 也返回 -1 或特定码，做到“最终一致幂等”。
     * <p>
     * 参数说明
     *
     * @param activity_code 活动编码（必填）
     * @param scene_code    场景编码（必填）
     * @param uid           用户ID（必填）
     * @param biz_key       幂等业务键（必填）
     * @param params        参数值（可为 null）
     */
    private ISyncResult triggerByActivateCode(@NonNull String activity_code, @NonNull String scene_code, long uid, @NonNull String biz_key, JSONObject params) {
        if (StringUtil.isEmpty(activity_code)) {
            LogsUtil.warn(TAG, "调用触发器错误：缺少活动编码");
            return new SyncResult(2, "缺少活动编码");
        }

        // 读取活动配置：决定活动是否启用、有效期、策略类型等
        ACTConfigEntity cfg = ACTConfigService.getInstance().getConfig(activity_code);
        if (cfg == null) {
            LogsUtil.warn(TAG, "调用触发器错误：无 %s 活动配置", activity_code);
            return new SyncResult(2, "缺少活动配置");
        }

        // 活动开关：非启用直接跳过
        if (cfg.status != 1) {
            // 不输出 warn：避免“活动停用”刷屏
            return new SyncResult(-1, String.format("[%s] - 活动停止使用", activity_code));
        }

        // 时间窗口控制：当 start_time/end_time 都不为 0，视为存在有效期限制
        long now_time = TimeUtil.getTimestamp();
        if (cfg.start_time != 0 && cfg.end_time != 0) {
            if (now_time < cfg.start_time) {
                return new SyncResult(-1, String.format("[%s] - 活动还没开始", activity_code));
            }
            if (now_time > cfg.end_time) {
                return new SyncResult(-1, String.format("[%s] - 活动已结束", activity_code));
            }
        }

        // 幂等校验：同一活动、同一场景、同一用户、同一业务键只执行一次
        if (ACTLogsService.getInstance().exist(activity_code, scene_code, uid, biz_key)) {
            return new SyncResult(-1, "已执行过");
        }

        // 获取策略实现：策略必须提前注册到 ACTStrategyFactory
        IACTStrategy strategy = ACTStrategyFactory.getStrategy(cfg.strategy_code);
        if (strategy == null) {
            LogsUtil.warn(TAG, "[%s] - 没有被注册，无法使用", activity_code);
            return new SyncResult(11, String.format("[%s] - 没有被注册，无法使用", activity_code));
        }

        // 组装执行上下文：策略执行时只依赖 ctx，避免策略到处查全局变量造成耦合
        ACTContext ctx = new ACTContext();
        ctx.activity_code = activity_code;
        ctx.scene_code = scene_code;
        ctx.uid = uid;
        ctx.biz_key = biz_key;
        ctx.config = cfg;
        ctx.params = params == null ? new JSONObject() : params;

        try {
            // 执行策略：由策略内部判断是否“命中/不命中”，并返回统一 ISyncResult
            ISyncResult r = strategy.execute(ctx);
            if (!r.isSuccess()) {
                log(r, cfg.debug_level, "[%s-%s][%s] - %s", activity_code, cfg.title, biz_key, r.getMsg());
                return r;
            }

            // 成功：记录一条业务日志，方便人工排查与运营对账
            log(r, cfg.debug_level, "用户：%s 在[%s]中参与了活动[%s-%s] - %s 策略：%s"
                    , ctx.uid
                    , ctx.scene_code
                    , cfg.activity_code
                    , cfg.title
                    , biz_key
                    , cfg.strategy_code
            );

            // 写活动执行日志表：用于幂等判断、运营审计、故障回溯
            ACTLogsEntity logsEntity = new ACTLogsEntity();
            logsEntity.activity_code = activity_code;
            logsEntity.scene_code = scene_code;
            logsEntity.uid = uid;
            logsEntity.biz_key = biz_key;
            logsEntity.result_code = r.getCode();
            logsEntity.result_msg = r.getMsg();
            logsEntity.extra_json = JSONObject.toJSONString(r.getData());
            ACTLogsService.getInstance().add(logsEntity);

            return r;
        } catch (Exception e) {
            // 单活动执行异常兜底：不应影响其他活动与主业务
            LogsUtil.error(e, TAG, "调用触发器过程中发生错误");
        }
        return new SyncResult(1, "");
    }

    // region 日志记录
    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * 记录流程日志
     * debug_level：0=关闭, 1=基础(成功/关键节点/错误), 2=详细(包含未命中/分支), 3=全量(附带更多上下文)
     * <p>
     * 约定：
     * - code==0      : 成功
     * - code==-1     : 未命中/跳过（正常分支）
     * - 其他         : 业务失败/异常码（建议 warn；真正异常在 catch 里 error）
     */
    private static void log(ISyncResult r, int debug_level, String fmt, Object... args) {
        if (r == null || debug_level <= 0) return;
        if (StringUtil.isEmpty(fmt)) return;

        final int code = r.getCode();
        final boolean ok = (code == 0);
        final boolean skip = (code == -1);
        final boolean err = (!ok && !skip);

        // 输出门槛：1 输出 ok + err；2 额外输出 skip；3 全量
        if (skip && debug_level < 2) return;
        if (!skip && debug_level < 1) return;

        Object[] baseArgs = (args == null || args.length == 0) ? EMPTY_ARGS : args;

        // 3=全量：把 code/msg/data 放到最后；data 有值才拼接
        String fmt2 = fmt;
        Object[] finalArgs = baseArgs;

        if (debug_level >= 3) {
            String msg = String.valueOf(r.getMsg());

            boolean hasData = hasMeaningfulData(r.getData());
            String dataStr = null;

            if (hasData) {
                Object dataObj = r.getData();
                if (dataObj instanceof String) dataStr = (String) dataObj;
                else {
                    try {
                        dataStr = JSONObject.toJSONString(dataObj);
                    } catch (Exception ignore) {
                        dataStr = "<data_to_json_failed>";
                    }
                }
            }

            // 尾部格式：code=%s,msg=%s,data=%s（data 有值才加）
            if (hasData) {
                fmt2 = fmt + " code=%s,msg=%s,data=%s";
                finalArgs = appendArgs(baseArgs, code, msg, dataStr);
            } else {
                fmt2 = fmt + " code=%s,msg=%s";
                finalArgs = appendArgs(baseArgs, code, msg);
            }
        }

        // ✅ 稳定关键：日志绝不能炸主流程
        safeEmit(err, fmt2, finalArgs, code, r.getMsg());
    }

    /**
     * data “存在值”判断：尽量不做重活，只做轻判断，避免性能开销
     */
    private static boolean hasMeaningfulData(Object data) {
        if (data == null) return false;

        if (data instanceof CharSequence) {
            return ((CharSequence) data).length() > 0;
        }
        if (data instanceof Map) {
            return !((Map<?, ?>) data).isEmpty();
        }
        if (data instanceof Collection) {
            return !((Collection<?>) data).isEmpty();
        }

        // JSONObject/JSONArray 也能走 Map/Collection 分支（fastjson2 的实现大多兼容）
        // 其他类型：视为有值（例如数字、实体对象）
        return true;
    }

    /**
     * 把追加参数放到最后（因为你要把 code/msg/data 放在末尾）
     */
    private static Object[] appendArgs(Object[] base, Object... tail) {
        int n1 = (base == null) ? 0 : base.length;
        int n2 = (tail == null) ? 0 : tail.length;

        Object[] out = new Object[n1 + n2];
        if (n1 > 0) System.arraycopy(base, 0, out, 0, n1);
        if (n2 > 0) System.arraycopy(tail, 0, out, n1, n2);
        return out;
    }

    /**
     * 安全输出：
     * - args 为空时，用 "%s" 包一层，彻底规避 fmt 中 '%'（比如“50%”）导致 String.format 崩溃
     * - 捕获 IllegalFormatException / Exception，保证业务不中断
     */
    private static void safeEmit(boolean err, String fmt, Object[] args, int code, String msg) {
        try {
            // args 为空：把 fmt 当纯文本输出，避免 fmt 里含 % 触发 format 解析
            if (args == null || args.length == 0) {
                if (err) LogsUtil.warn(TAG, "%s", fmt);
                else LogsUtil.info(TAG, "%s", fmt);
                return;
            }

            if (err) LogsUtil.warn(TAG, fmt, args);
            else LogsUtil.info(TAG, fmt, args);

        } catch (IllegalFormatException ife) {
            // 格式化失败兜底：避免再次 format 崩溃，一律用 "%s"
            try {
                LogsUtil.warn(TAG, "%s",
                        "log format error: fmt=" + fmt
                                + ", argsLen=" + (args == null ? 0 : args.length)
                                + ", code=" + code
                                + ", msg=" + msg
                );
            } catch (Exception ignore) {
                // 吞掉，保证业务不中断
            }
        } catch (Exception any) {
            // 任何日志异常都不能影响业务
            try {
                LogsUtil.warn(TAG, "%s", "log emit error: " + any.getClass().getSimpleName() + " - " + any.getMessage());
            } catch (Exception ignore) {
                // 吞掉
            }
        }
    }
    // endregion
}
