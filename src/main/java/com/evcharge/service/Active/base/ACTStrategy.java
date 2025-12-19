package com.evcharge.service.Active.base;

import java.lang.annotation.*;

/**
 * 活动配置注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ACTStrategy {
    /**
     * 策略编码
     */
    String code();

    /**
     * 策略简短说明
     */
    String desc() default "";
}
