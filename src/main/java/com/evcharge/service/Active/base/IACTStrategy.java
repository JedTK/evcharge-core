package com.evcharge.service.Active.base;

import com.xyzs.entity.ISyncResult;

/**
 * 活动策略接口
 */
public interface IACTStrategy {
    /**
     * 活动执行策略
     *
     * @param ctx 活动上下文
     * @return 执行结果
     */
    ISyncResult execute(ACTContext ctx) throws Exception;
}
