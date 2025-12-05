package com.evcharge.qrcore.parser.base;

import com.alibaba.fastjson2.JSONObject;

/**
 * 二维码内容
 */
public class QRContent {
    public enum QRType {
        URL,                // 纯 URL
        TEXT,               // 纯文本
        EvDevice,           // 充电设备
        GeneraDevice,       // 通用设备
        UNKNOWN             // 无法识别
    }

    /**
     * 是否解析成功
     */
    public boolean success;
    /**
     * 二维码内容
     */
    public QRType type;
    /***
     * 设备编码
     */
    public String device_code;
    /**
     * 其他数据
     */
    public JSONObject data;
    /**
     * 原始二维码内容
     */
    public String content;
    /**
     * 解析失败时的错误说明
     */
    public String errorMessage;

    // --- 链式构造方便使用 ---
    public static QRContent ok(QRType type) {
        QRContent r = new QRContent();
        r.success = true;
        r.type = type;
        return r;
    }

    public static QRContent fail(String msg) {
        QRContent r = new QRContent();
        r.success = false;
        r.type = QRType.UNKNOWN;
        r.errorMessage = msg;
        return r;
    }
}
