package com.evcharge.service.Active;

import com.evcharge.entity.active.ACTSceneEntity;

import java.util.List;
import java.util.Map;

/**
 * 场景映射表：一个活动绑定多个场景;(ACTScene)表服务接口
 *
 * @author :
 * @date : 2025-12-19
 */
public class ACTSceneService {
    /**
     * 单例实例
     * - 使用双重检查锁（DCL）保证并发安全与初始化性能
     */
    private volatile static ACTSceneService instance;

    public static ACTSceneService getInstance() {
        if (instance == null) {
            synchronized (ACTSceneService.class) {
                if (instance == null) {
                    instance = new ACTSceneService();
                }
            }
        }
        return instance;
    }

    /**
     * 获取场景下的活动编码列表
     *
     * @param scene_code 场景代码
     * @return 活动编码列表
     */
    public List<Map<String, Object>> getList(String scene_code) {
        return getList(scene_code, true);
    }

    /**
     * 获取场景下的活动编码列表
     *
     * @param scene_code 场景代码
     * @param inCache    是否优先从缓存中获取
     * @return 活动编码列表
     */
    public List<Map<String, Object>> getList(String scene_code, boolean inCache) {
        ACTSceneEntity entity = new ACTSceneEntity();
        if (inCache) entity.cache(String.format("ACT:Scene:%s:List", scene_code));
        entity.field("activity_code")
                .where("scene_code", scene_code)
                .where("status", 1)
                .order("priority")
        ;
        return entity.select();
    }

    /**
     * 活动场景配置信息
     *
     * @param scene_code 场景代码
     * @return 活动配置实体类
     */
    public ACTSceneEntity getScene(String scene_code) {
        return getScene(scene_code, true);
    }

    /**
     * 活动场景配置信息
     *
     * @param scene_code 场景代码
     * @param inCache    是否优先从缓存中获取
     * @return 活动配置实体类
     */
    public ACTSceneEntity getScene(String scene_code, boolean inCache) {
        ACTSceneEntity entity = new ACTSceneEntity();
        if (inCache) entity.cache(String.format("BaseData:ACTScene:%s", scene_code));
        entity.where("scene_code", scene_code);
        return entity.findEntity();
    }
}
