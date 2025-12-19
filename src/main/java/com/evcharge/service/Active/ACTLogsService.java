package com.evcharge.service.Active;

/**
 * 执行日志表：幂等+排错+统计;(ACTLogs)表服务接口
 *
 * @author :
 * @date : 2025-12-19
 */
public class ACTLogsService {
    /**
     * 单例实例
     * - 使用双重检查锁（DCL）保证并发安全与初始化性能
     */
    private volatile static ACTLogsService instance;

    public static ACTLogsService getInstance() {
        if (instance == null) {
            synchronized (ACTLogsService.class) {
                if (instance == null) {
                    instance = new ACTLogsService();
                }
            }
        }
        return instance;
    }
}
