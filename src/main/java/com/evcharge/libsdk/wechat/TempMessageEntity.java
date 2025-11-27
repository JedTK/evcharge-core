package com.evcharge.libsdk.wechat;


import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.Map;

@Data
public class TempMessageEntity {

    //region  -- 实体类属性 --
    /**
     * 接收者（用户）的 openid
     */
    private String touser;

    /**
     * 所需下发的订阅模板id
     */
    private String template_id;

    /**
     * 点击模板卡片后的跳转页面，仅限本小程序内的页面。支持带参数,（示例index?foo=bar）。该字段不填则模板无跳转。
     */
    private String page;

    /**
     * 跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
     */
    private String miniprogram_state;

    private String lang;

    /**
     * 模板内容，格式形如 { "key1": { "value": any }, "key2": { "value": any } }
     */
    private JSONObject data;
    //endregion









}
