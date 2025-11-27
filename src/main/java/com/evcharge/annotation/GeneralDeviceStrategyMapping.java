package com.evcharge.annotation;

import java.lang.annotation.*;

/**
 * 通用设备策略类型注解。
 * <p>
 * 该注解用于标记通用设备的策略映射。通过此注解，可以为设备类型、品牌、或具体的SPU（Stock Keeping Unit）编码指定对应的策略执行逻辑。
 * <p>
 * 策略的执行顺序是：
 * 1. 检查是否存在对应的 SPU 编码策略。
 * 2. 如果没有找到对应的 SPU 编码策略，则检查是否存在对应的品牌编码策略。
 * 3. 如果品牌编码策略也不存在，则检查是否存在对应的设备类型编码策略。
 * 4. 如果以上三者都没有找到对应的策略，则表明该设备尚未定义策略。
 * <p>
 * 该注解可用于实现通用设备的默认策略，当需要执行设备的固有能力时，通过注解指定的编码来确定使用哪一策略。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GeneralDeviceStrategyMapping {

    /**
     * 类型编码(必要)。
     * <p>
     * 该字段用于指定设备的类型编码。设备的类型编码代表设备的通用类别，用于在没有具体品牌或SPU策略时，
     * 作为默认的策略执行依据。
     *
     * @return 设备的类型编码
     */
    String typeCode() default "";

    /**
     * 品牌编码(可选)。
     * <p>
     * 该字段用于指定设备的品牌编码。品牌编码在一个品牌内的不同产品之间共享相同的功能或能力，
     * 这意味着可以为同一品牌的多个设备集中处理相同的策略。当SPU策略未定义时，
     * 会根据品牌编码来查找相应的策略。
     *
     * @return 设备的品牌编码，默认为空字符串
     */
    String brandCode() default "";

    /**
     * SPU编码(可选)。
     * <p>
     * 该字段用于指定设备的SPU编码，SPU是设备的最小区分单位，用于标识特定型号或规格的设备。
     * 该字段控制的策略粒度最细，可以为每一个具体的产品指定其固有的能力策略。
     * 当品牌策略未定义时，会根据SPU编码来查找相应的策略。
     *
     * @return 设备的SPU编码，默认为空字符串
     */
    String spuCode() default "";
}