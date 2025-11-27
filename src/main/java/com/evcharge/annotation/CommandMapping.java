package com.evcharge.annotation;

import java.lang.annotation.*;

/**
 * 命令映射注解，用于标记带命令参数的数据处理方法，如HTTP、TCP、WebSocket、UDP等。
 * <p>
 * 使用指南：
 * 1. 编写命令处理类，并在需要处理的方法上添加此注解。
 * 2. 在程序启动时，编写注解注册处理器，注册所有标注了此注解的方法。
 * <p>
 * 主要作用：
 * 此注解将注解值（命令名称）映射到对应的处理方法，并加载到内存中以优化和加速访问。
 * 相较于传统的映射方式，此方法性能更高；相较于使用switch和if-else结构，此方法更为优雅；
 * 相较于策略模式，此方法在后续添加新命令时无需编辑命令列表，扩展性更强。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CommandMapping {
    /**
     * 命令的名称，用于映射到具体的处理方法。
     *
     * @return 命令名称
     */
    String value() default "";
}
