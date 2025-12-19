package com.evcharge.service.Active;


import com.evcharge.entity.active.ACTConfigEntity;

/**
 * 活动配置表;(ACTConfig)表服务接口
 *
 * @author :
 * @date : 2025-12-19
 */
public class ACTConfigService {

    /**
     * 单例实例
     * - 使用双重检查锁（DCL）保证并发安全与初始化性能
     */
    private volatile static ACTConfigService instance;

    public static ACTConfigService getInstance() {
        if (instance == null) {
            synchronized (ACTConfigService.class) {
                if (instance == null) {
                    instance = new ACTConfigService();
                }
            }
        }
        return instance;
    }

    /**
     * 活动活动配置信息
     *
     * @param activity_code 活动代码
     * @return 活动配置实体类
     */
    public ACTConfigEntity getConfig(String activity_code) {
        return getConfig(activity_code, true);
    }

    /**
     * 活动活动配置信息
     *
     * @param activity_code 活动代码
     * @param inCache       是否优先从缓存中获取
     * @return 活动配置实体类
     */
    public ACTConfigEntity getConfig(String activity_code, boolean inCache) {
        ACTConfigEntity entity = new ACTConfigEntity();
        if (inCache) entity.cache(String.format("BaseData:ACTConfig:%s", activity_code));
        entity.where("activity_code", activity_code);
        return entity.findEntity();
    }
}
