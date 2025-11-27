package com.evcharge.task.monitor.check;

import com.xyzs.entity.SyncResult;

/**
 * 设备检测器接口
 * 定义了所有检测器必须实现的方法。
 * 这是策略模式的核心。
 */
public interface DeviceChecker {
    /**
     * 执行检测的核心方法
     * @param device 待检测的设备
     * @return true 如果设备正常，false 如果异常
     */
    SyncResult check(Device device);

    /**
     * 声明此检测器支持哪种设备类型
     * @return 支持的设备类型枚举
     */
    DeviceType supports(); // <--- 确保这一行存在！

}
