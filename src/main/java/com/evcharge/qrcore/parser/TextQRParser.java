package com.evcharge.qrcore.parser;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.xyzs.utils.StringUtil;

/**
 * 纯文本二维码解析器（兜底）
 * <p>
 * 作用：
 * - 前面的专用解析器（设备码、URL 等）都不匹配时，由它兜底处理；
 * - 不做任何业务含义判断，只是简单标记为 TEXT，并把原文放入 data 中。
 */
public class TextQRParser implements IQRCodeParser {

    @Override
    public boolean supports(String content) {
        // 兜底解析器：只要还有内容就认为“我可以处理”
        // 空内容在 QRCoreParser 里已经拦截掉了
        return StringUtil.isNotEmpty(content);
    }

    @Override
    public QRContent parse(String content) {
        // 这里理论上 supports 为 true 时 content 一定非空，为安全再兜一层
        if (content == null || content.trim().isEmpty()) {
            return QRContent.fail("文本内容为空");
        }

        QRContent r = QRContent.ok(QRContent.QRType.TEXT);
        r.content = content;

        // 可选：把文本原文也塞进 data，方便上层统一处理
        JSONObject data = new JSONObject();
        data.put("text", content);
        r.data = data;

        // 兜底解析器不去猜 spu_code/device_code，避免和专用解析器职责混淆
        // r.spu_code = null;
        // r.device_code = null;
        r.device_code = content;
        return r;
    }
}
