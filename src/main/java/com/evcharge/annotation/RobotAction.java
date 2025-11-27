package com.evcharge.annotation;

import java.lang.annotation.*;

/**
 * 学习自定义注解
 * https://blog.csdn.net/weixin_42467508/article/details/114039665?ops_request_misc=&request_id=&biz_id=102&utm_term=java%20%E6%96%B9%E6%B3%95%E6%B3%A8%E8%A7%A3&utm_medium=distribute.pc_search_result.none-task-blog-2~all~sobaiduweb~default-0-114039665.142^v63^control,201^v3^control_2,213^v2^t3_esquery_v2&spm=1018.2226.3001.4187
 */
@Retention(RetentionPolicy.RUNTIME)//使用元注解，表示此自定义注解 会在class字节码文件中存在，在运行时可以通过反射获取到
@Target(ElementType.METHOD)//表示此注解应用于方法中
@Documented
public @interface RobotAction {

}
