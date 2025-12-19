package com.evcharge.service.Active;

import com.evcharge.entity.active.ACTLogsEntity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;

import java.util.Map;

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

    /**
     * 新增活动操作日志
     *
     * @param entity 日志实体类
     */
    public void add(ACTLogsEntity entity) {
        entity.create_time = TimeUtil.getTimestamp();
        entity.insert();
    }

    /**
     * 检查幂等值
     *
     * @param activity_code 活动编码
     * @param scene_code    场景编码
     * @param uid           用户id
     * @param biz_key       幂等业务键(订单号/充值单号/自定义),;
     * @return 是否存在数据
     */
    public boolean exist(String activity_code, String scene_code, long uid, String biz_key) {
        Map<String, Object> data = ACTLogsEntity.getInstance()
                .field("id")
                .cache(String.format("ACT:Logs:%s_%s_%s_%s", activity_code, scene_code, uid, biz_key))
                .where("activity_code", activity_code)
                .where("scene_code", scene_code)
                .where("uid", uid)
                .where("biz_key", biz_key)
                .find();
        if (data == null || data.isEmpty()) return false;
        long id = MapUtil.getLong(data, "id");
        return id != 0;
    }
}
