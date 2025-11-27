package com.evcharge.mqtt;

import java.lang.annotation.*;

/**
 * MQTT订阅注解
 */
@Retention(RetentionPolicy.RUNTIME)//使用元注解，表示此自定义注解 会在class字节码文件中存在，在运行时可以通过反射获取到
@Target(ElementType.METHOD)//表示此注解应用于方法中
@Documented
public @interface XMQTTSub {
    /**
     * 主题
     */
    String Topic() default "";

    /**
     * 消息质量：0=最多一次，1=至少一次，2=仅一次
     */
    int Qos() default 0;

    /**
     * 打开日志
     */
    boolean openLog() default true;
}
