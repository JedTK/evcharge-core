package com.evcharge.service.Active.Strategy;

import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.ACTStrategy;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.ISyncResult;

/**
 * 充电结算分位匹配退款策略
 * - 规则：结算金额小数点后两位命中 matchCents 列表则退款/免单
 */
@ACTStrategy(code = "STR_CHARGE_FINISH_CENTS_MATCH_REFUND", desc = "充电结算分位匹配退款策略")
public class ChargeFinishCentsMatchRefundStrategy implements IACTStrategy {

    @Override
    public ISyncResult execute(ACTContext ctx) throws Exception {

        return null;
    }
}
