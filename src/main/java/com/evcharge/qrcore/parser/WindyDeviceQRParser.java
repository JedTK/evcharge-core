package com.evcharge.qrcore.parser;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.xyzs.utils.Convert;
import com.xyzs.utils.StringUtil;

import java.net.URI;

/**
 * 帝能 / windy 品牌电动自行车充电设备二维码解析。
 * <p>
 * 约定二维码内容格式示例：
 * https://任意域名/windy/4780f40c/ff
 * <p>
 * 约定含义：
 * 1) 路径第一段 windy 作为品牌代码（品牌标识）。
 * 2) 路径第二段 4780f40c 为设备号 deviceCode。
 * 3) 路径第三段 ff 为端口号：
 * - 一般情况是 "00" 或 "01" 代表具体端口。
 * - "ff" 表示空白端口（未指定端口），在解析时可以视为 null。
 * <p>
 * 设计要点：
 * - 不依赖域名（host），只要路径结构符合 /windy/{deviceCode}/{port} 即认为支持。
 * - supports 只负责“识别格式”，parse 负责真正解析和组装 QRContent。
 */
public class WindyDeviceQRParser implements IQRCodeParser {

    /**
     * 品牌代码常量，对应路径中的第一段 "windy"。
     */
    private static final String BRAND_CODE = "Windy";

    @Override
    public boolean supports(String content) {
        if (StringUtil.isEmpty(content)) return false;
        String text = content.trim();
        if (StringUtil.isEmpty(text)) return false;

        // 必须是 http 或 https 的 URL，避免和纯文本解析器冲突
        if (!text.startsWith("http://") && !text.startsWith("https://")) return false;

        try {
            URI uri = URI.create(text);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return false;
            }

            // 按 "/" 拆分路径，示例：
            // "/windy/4780f40c/ff" -> ["", "windy", "4780f40c", "ff"]
            String[] parts = path.split("/");
            // 至少需要 /windy/{deviceCode}
            if (parts.length < 3) {
                return false;
            }

            // 第一段必须是 windy（品牌代码）
            if (!BRAND_CODE.equalsIgnoreCase(parts[1])) {
                return false;
            }

            // 第二段必须有设备号
            String deviceCode = parts[2];
            return deviceCode != null && !deviceCode.isEmpty();
        } catch (Exception e) {
            // URL 非法则不支持
            return false;
        }
    }

    @Override
    public QRContent parse(String content) {
        try {
            URI uri = URI.create(content.trim());
            String path = uri.getPath();
            String[] parts = path.split("/");

            // 这里在 supports 已经过滤过一次，理论上不会为空，为安全再做一层校验
            String deviceCode = parts.length > 2 ? parts[2] : "";
            String portRaw = parts.length > 3 ? parts[3] : "";

            if (StringUtil.isEmpty(deviceCode)) {
                QRContent fail = QRContent.fail("windy 设备二维码缺少设备号");
                fail.content = content;
                return fail;
            }

            // 端口号规范化
            // 约定：
            //   - "00" / "01" 等保留原样
            //   - "ff" 或 "FF" 视为未指定端口，解析为 null
            String port = null;
            if (StringUtil.isEmpty(portRaw)) {
                if (!"ff".equalsIgnoreCase(portRaw)) port = portRaw;
                else port = "99";
            }

            // 这里假设 QRType 里有 DEVICE 枚举，如果你目前只有 URL / TEXT，可以先改成 URL，
            // 或者在 QRType 中新增一个 DEVICE 类型。
            QRContent result = QRContent.ok(QRContent.QRType.EvDevice);
            result.content = content;
            // 设备号写入统一字段
            result.device_code = deviceCode;

            // 附加数据写入 data
            if (result.data == null) {
                result.data = new JSONObject();
            }
            result.data.put("brand_code", BRAND_CODE);
            result.data.put("device_code", deviceCode);
            result.data.put("port", Convert.toInt(port));
            return result;
        } catch (Exception e) {
            // 出现异常时返回 fail，让上层有机会继续走 UrlQRParser 或 TextQRParser 等兜底
            QRContent fail = QRContent.fail("windy 设备二维码解析异常");
            fail.content = content;
            return fail;
        }
    }
}
