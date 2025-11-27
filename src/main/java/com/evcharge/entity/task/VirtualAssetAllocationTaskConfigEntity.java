package com.evcharge.entity.task;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 虚拟金额分配任务配置;
 *
 * @author : JED
 * @date : 2022-12-8
 */
public class VirtualAssetAllocationTaskConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 充电站ID
     */
    public long CSId;
    /**
     * 状态：0=不启动，1=启动，2=完成，3=模拟充电中，4=已撤回
     */
    public int status;
    /**
     * 总金额
     */
    public double totalAmount;
    /**
     * 总余额
     */
    public double totalBalance;
    /**
     * 每日消耗金额(大概值)，超过这个值会停止
     */
    public double dailyConsumeAmount;
    /**
     * 订单类型：1=充值，2=充电卡
     */
    public int orderTypeId;
    /**
     * 充电习惯：开始时间，如:15:00:00
     */
    public String dailyStartChargeTime;
    /**
     * 充电习惯：结束时间，如：2:00:00
     */
    public String dailyEndChargeTime;
    /**
     * 开始时间戳
     */
    public long startTime;
    /**
     * 结束时间戳
     */
    public long endTime;
    /**
     * 下次开始时间戳
     */
    public long next_start_time;
    /**
     * 测试ID，由时间戳生成
     */
    public long testId;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static VirtualAssetAllocationTaskConfigEntity getInstance() {
        return new VirtualAssetAllocationTaskConfigEntity();
    }
}
