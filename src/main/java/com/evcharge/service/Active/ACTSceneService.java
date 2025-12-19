package com.evcharge.service.Active;

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
}
