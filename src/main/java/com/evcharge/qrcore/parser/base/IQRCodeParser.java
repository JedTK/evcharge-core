package com.evcharge.qrcore.parser.base;

/**
 * 二维码解析器接口
 */
public interface IQRCodeParser {
    /**
     * 当前解析器是否支持处理这段二维码内容
     */
    boolean supports(String content);

    /**
     * 真正的解析逻辑
     */
    QRContent parse(String content);
}
