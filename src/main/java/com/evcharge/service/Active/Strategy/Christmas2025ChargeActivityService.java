package com.evcharge.service.Active.Strategy;

import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.ACTStrategy;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.ISyncResult;

/**
 * 2025年圣诞节充电活动业务逻辑
 * <p>
 * 活动时间：2025-12-25 一整天
 * 描述：活动当天的用户进行充电结算时，当结算金额小数点后2位出现12和25的可以退还本次充电扣费金额（相当于免单）
 *
 */
@ACTStrategy(code = "ACT_2025_XMAS_CHARGE",desc = "")
public class Christmas2025ChargeActivityService implements IACTStrategy {


//    /**
//     * 活动代码
//     */
//    private final static String activity_code = "ACT_2025_XMAS_CHARGE";
//    /**
//     * 活动标题
//     */
//    private final static String title = "2025年圣诞充电活动";
//    /**
//     * 活动开始时间
//     */
//    private final static long start_time = TimeUtil.toTimestamp("2025-12-25 00:00:00");
//    /**
//     * 活动结束时间
//     */
//    private final static long end_time = TimeUtil.toTimestamp("2025-12-25 23:59:59");



    @Override
    public ISyncResult execute(ACTContext ctx) throws Exception {
        return null;
    }

}
