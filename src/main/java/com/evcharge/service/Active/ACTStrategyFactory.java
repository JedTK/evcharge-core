package com.evcharge.service.Active;

import com.evcharge.service.Active.Strategy.DefaultACTStrategy;
import com.evcharge.service.Active.base.ACTStrategy;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.StringUtil;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动策略工厂
 * - strategy_code -> strategy instance
 */
public class ACTStrategyFactory {

    private static final String TAG = "活动策略工厂";

    private static final Map<String, IACTStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    /**
     * 防止重复初始化（可按需移除）
     */
    private static volatile boolean INITIALIZED = false;

    /**
     * 初始化
     */
    public static void init() {
        register(DefaultACTStrategy.class.getPackageName());
    }

    /**
     * 注册活动策略（实例）
     */
    public static SyncResult register(String strategyCode, IACTStrategy strategy, boolean forced) {
        if (StringUtil.isEmpty(strategyCode)) {
            LogsUtil.warn(TAG, "注册失败：缺少策略编码");
            return new SyncResult(2, "缺少策略编码");
        }
        if (strategy == null) {
            LogsUtil.warn(TAG, "注册失败：策略实例为空, code=%s", strategyCode);
            return new SyncResult(2, "策略实例为空");
        }

        // 统一 trim，避免配置里多空格导致找不到
        strategyCode = strategyCode.trim();

        if (!forced) {
            IACTStrategy old = STRATEGY_MAP.putIfAbsent(strategyCode, strategy);
            if (old != null) {
                LogsUtil.error(TAG, "注册失败：策略编码重复 code=%s, old=%s, new=%s"
                        , strategyCode
                        , old.getClass().getName()
                        , strategy.getClass().getName());
                return new SyncResult(3, "策略编码重复:" + strategyCode);
            }
        }
        STRATEGY_MAP.put(strategyCode, strategy);

        LogsUtil.info(TAG, "注册活动策略成功：%s -> %s", strategyCode, strategy.getClass().getName());
        return new SyncResult(0, "");
    }

    /**
     * 注册活动策略（从策略类 @ACTStrategy 注解读取 code）
     */
    public static SyncResult register(IACTStrategy strategy) {
        return register(strategy, false);
    }

    /**
     * 注册活动策略（从策略类 @ACTStrategy 注解读取 code）
     *
     * @param strategy 策略实例
     * @param forced   是否强制覆盖
     */
    public static SyncResult register(IACTStrategy strategy, boolean forced) {
        if (strategy == null) {
            LogsUtil.warn(TAG, "注册失败：策略实例为空");
            return new SyncResult(2, "策略实例为空");
        }

        Class<?> clz = strategy.getClass();

        // 读取注解
        ACTStrategy meta = clz.getAnnotation(ACTStrategy.class);
        if (meta == null) {
            LogsUtil.warn(TAG, "注册失败：策略类缺少 @ACTStrategy 注解：%s", clz.getName());
            return new SyncResult(2, "策略类缺少@ACTStrategy注解:" + clz.getName());
        }

        String code = meta.code();
        if (StringUtil.isEmpty(code)) {
            LogsUtil.warn(TAG, "注册失败：@ACTStrategy.code 为空：%s", clz.getName());
            return new SyncResult(2, "@ACTStrategy.code不能为空:" + clz.getName());
        }

        code = code.trim();

        // 走你已有的注册逻辑（含重复检测/覆盖）
        return register(code, strategy, forced);
    }

    /**
     * 启动扫描注册（建议只调用一次）
     *
     * @param basePackage 要扫描的基础包名，如 com.evcharge.service.Active.strategies
     */
    public static synchronized void register(String basePackage) {
        if (INITIALIZED) {
            LogsUtil.warn(TAG, "已初始化，跳过重复扫描：%s", basePackage);
            return;
        }
        if (StringUtil.isEmpty(basePackage)) throw new IllegalArgumentException("basePackage 不能为空");

        // 更稳的 Reflections 配置
        Reflections reflections = new Reflections(basePackage
                , new TypeAnnotationsScanner()
                , new SubTypesScanner(false)
        );

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(ACTStrategy.class);

        int ok = 0;
        int skip = 0;

        for (Class<?> clz : classes) {
            ACTStrategy meta = clz.getAnnotation(ACTStrategy.class);
            if (meta == null) {
                skip++;
                continue;
            }

            String code = meta.code();
            if (StringUtil.isEmpty(code)) {
                LogsUtil.warn(TAG, "策略 code 为空，跳过：%s", clz.getName());
                skip++;
                continue;
            }
            code = code.trim();

            // 必须实现 IACTStrategy
            if (!IACTStrategy.class.isAssignableFrom(clz)) {
                LogsUtil.error(TAG, "类标注了 @ACTStrategy 但未实现 IACTStrategy：%s", clz.getName());
                // 建议 fail-fast
                throw new IllegalStateException("策略类未实现 IACTStrategy: " + clz.getName());
            }

            // 实例化
            IACTStrategy strategy = newInstance((Class<? extends IACTStrategy>) clz);

            // 注册（重复建议 fail-fast）
            SyncResult r = register(code, strategy, false);
            if (r.getCode() != 0) {
                // 这里我建议直接抛异常，避免“启动成功但运行缺策略”的隐蔽事故
                throw new IllegalStateException("策略注册失败：" + code + " / " + r.getMsg());
            }

            ok++;
        }

        INITIALIZED = true;
        LogsUtil.info(TAG, "扫描注册完成：basePackage=%s, ok=%d, skip=%d, total=%d"
                , basePackage
                , ok
                , skip
                , classes.size()
        );
    }

    /**
     * 按策略编码查找
     */
    public static IACTStrategy getStrategy(String strategyCode) {
        if (StringUtil.isEmpty(strategyCode)) return null;
        return STRATEGY_MAP.get(strategyCode.trim());
    }

    /**
     * （可选）清空（用于单测/热更新）
     */
    public static synchronized void clear() {
        STRATEGY_MAP.clear();
        INITIALIZED = false;
    }

    private static IACTStrategy newInstance(Class<? extends IACTStrategy> clz) {
        try {
            Constructor<? extends IACTStrategy> c = clz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("策略类必须提供无参构造：" + clz.getName(), e);
        } catch (Exception e) {
            throw new IllegalStateException("策略实例化失败：" + clz.getName(), e);
        }
    }
}
