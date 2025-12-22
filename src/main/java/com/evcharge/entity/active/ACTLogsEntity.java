package com.evcharge.entity.active;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 执行日志表：幂等+排错+统计 - 实体类 ;
 *
 * @date : 2025-12-19
 */
@TargetDB("evcharge_activity")
public class ACTLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 活动编码(唯一),;
     */
    public String activity_code;
    /**
     * 触发场景：CHARGE_FINISH/RECHARGE_CALLBACK/HOME_ENTER等,;
     */
    public String scene_code;
    /**
     * 用户ID,;
     */
    public long uid;
    /**
     * 幂等业务键(订单号/充值单号/自定义),;
     */
    public String biz_key;
    /**
     * 结果码：0=成功，非0失败,;
     */
    public int result_code;
    /**
     * 结果说明/失败原因,;
     */
    public String result_msg;
    /**
     * 扩展信息：奖励、弹窗、策略输出等,;
     */
    public String extra_json;
    /**
     * 创建时间戳,;
     */
    public long create_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static ACTLogsEntity getInstance() {
        return new ACTLogsEntity();
    }
}
