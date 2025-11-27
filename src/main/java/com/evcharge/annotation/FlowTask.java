package com.evcharge.annotation;

import java.lang.annotation.*;

/**
 * 自定义工作流
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface FlowTask {
    /**
     * 任务id
     *
     * @return
     */
    String id() default "";

    /**
     * 说明
     *
     * @return
     */
    String desc() default "";

    boolean autoFinish() default true;
}
