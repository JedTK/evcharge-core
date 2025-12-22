package com.evcharge.service.Active.base;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.ACTConfigEntity;

/**
 * 活动上下文
 */
public class ACTContext {
    /**
     * 活动编码(唯一)
     */
    public String activity_code;
    /**
     * 触发场景：CHARGE_FINISH/RECHARGE_CALLBACK/HOME_ENTER等
     */
    public String scene_code;
    /**
     * 用户Id
     */
    public long uid;
    /**
     * 幂等业务键(订单号/充值单号/自定义)
     */
    public String biz_key;
    /**
     * 活动配置
     */
    public ACTConfigEntity config;
    /**
     * 业务侧传入参数
     */
    public JSONObject params;
}
