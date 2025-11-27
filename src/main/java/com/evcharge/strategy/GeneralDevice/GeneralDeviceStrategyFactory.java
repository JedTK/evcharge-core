package com.evcharge.strategy.GeneralDevice;

import com.evcharge.annotation.GeneralDeviceStrategyMapping;
import com.evcharge.entity.device.GeneralDeviceEntity;
import com.evcharge.service.GeneralDevice.GeneralDeviceService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用设备策略工厂类。
 * <p>
 * 该工厂类用于根据设备的类型编码（typeCode）、品牌编码（brandCode）和SPU编码（spuCode）来获取相应的策略实现类。
 * 工厂类会在启动时缓存所有带有 `@GeneralDeviceStrategyMapping` 注解的策略实现类，以便在需要时快速获取对应的策略。
 */
@Component
public class GeneralDeviceStrategyFactory {
    @Getter
    private static GeneralDeviceStrategyFactory instance;
    // 使用并发哈希映射存储策略实现类缓存，键为策略标识符，值为对应的策略实现类
    private final Map<String, IGeneralDeviceStrategy> strategyCache = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    /**
     * 使用构造函数注入 ApplicationContext。
     * <p>
     * 构造函数注入是推荐的依赖注入方式，因为它能够确保依赖在对象创建时就已准备好，
     * 并且更容易进行单元测试。
     *
     * @param applicationContext Spring 的 ApplicationContext，用于获取所有策略 Bean
     */
    @Autowired
    public GeneralDeviceStrategyFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        instance = this;
        init();
    }

    /**
     * 初始化策略缓存。
     * <p>
     * 该方法会扫描所有带有 `@GeneralDeviceStrategyMapping` 注解的类，并将它们缓存到策略缓存中。
     */
    private void init() {
        // 获取所有带有 GeneralDeviceStrategyMapping 注解的 Bean
        Map<String, Object> strategyBeans = applicationContext.getBeansWithAnnotation(GeneralDeviceStrategyMapping.class);
        // 遍历这些 Bean，并将其按类型编码、品牌编码和SPU编码生成的键存入缓存中
        for (Object bean : strategyBeans.values()) {
            GeneralDeviceStrategyMapping mapping = bean.getClass().getAnnotation(GeneralDeviceStrategyMapping.class);
            String key = generateKey(mapping.typeCode(), mapping.brandCode(), mapping.spuCode());
            strategyCache.put(key, (IGeneralDeviceStrategy) bean);
        }
    }

    /**
     * 根据序列号获取对应的策略实现类。
     * <p>
     * 该方法会首先根据设备的SPU编码查找最具体的策略。如果没有找到，则依次查找品牌编码策略和类型编码策略。
     * 如果仍未找到对应的策略，则返回默认策略。
     *
     * @param serialNumber 设备的序列号，用于唯一标识具体设备
     * @return 对应的策略实现类，如果找不到则返回默认策略
     */
    public IGeneralDeviceStrategy getStrategy(String serialNumber) {
        // 通过序列号获取设备实例
        GeneralDeviceEntity device = GeneralDeviceService.getInstance().getWithSerialNumber(serialNumber);

        // 优先查找 SPU 策略
        IGeneralDeviceStrategy strategy = strategyCache.get(generateKey(device.typeCode, device.brandCode, device.spuCode));
        if (strategy != null) return strategy;

        // 查找品牌策略
        strategy = strategyCache.get(generateKey(device.typeCode, device.brandCode, ""));
        if (strategy != null) return strategy;

        // 查找类型策略
        strategy = strategyCache.get(generateKey(device.typeCode, "", ""));
        if (strategy != null) return strategy;

        // 返回默认策略
        strategy = strategyCache.get(generateKey("", "", ""));
        return strategy;
    }

    /**
     * 生成策略缓存的键。
     * <p>
     * 该方法根据设备的类型编码、品牌编码和SPU编码生成唯一的缓存键。
     * 如果所有编码均为空，则返回默认键 "default"。
     *
     * @param typeCode  设备的类型编码
     * @param brandCode 设备的品牌编码
     * @param spuCode   设备的SPU编码
     * @return 生成的缓存键
     */
    private String generateKey(String typeCode, String brandCode, String spuCode) {
        // 如果所有编码都为空，则返回默认键 "default"
        if (!StringUtils.hasLength(typeCode)
                && !StringUtils.hasLength(brandCode)
                && !StringUtils.hasLength(spuCode)) {
            return "default";
        }
        // 以 "typeCode_brandCode_spuCode" 格式生成唯一键
        return String.format("%s_%s_%s", typeCode, brandCode, spuCode);
    }
}