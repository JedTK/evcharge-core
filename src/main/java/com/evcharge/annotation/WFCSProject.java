package com.evcharge.annotation;

import java.lang.annotation.*;

/**
 * 自定义工作流
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface WFCSProject {
    /**
     * 说明
     */
    String desc() default "";
}
