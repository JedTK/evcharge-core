package com.evcharge.service.Active.Strategy;

import com.evcharge.service.Active.base.ACTContext;
import com.evcharge.service.Active.base.ACTStrategy;
import com.evcharge.service.Active.base.IACTStrategy;
import com.xyzs.entity.ISyncResult;
import com.xyzs.entity.SyncResult;

/**
 * 默认活动策略，没实际意义，只是建立用于工厂策略自动查找包路径
 */
@ACTStrategy(code = "DEFAULT", desc = "默认活动策略")
public class DefaultACTStrategy implements IACTStrategy {

    @Override
    public ISyncResult execute(ACTContext ctx) throws Exception {
        return new SyncResult(-1, "");
    }
}
