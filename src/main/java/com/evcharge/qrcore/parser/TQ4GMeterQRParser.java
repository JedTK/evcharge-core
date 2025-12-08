package com.evcharge.qrcore.parser;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.xyzs.utils.StringUtil;

import java.net.URI;

/**
 * 拓强 4G 电表二维码解析器。
 * <p>
 * 支持两类二维码格式：
 * 1) 旧版纯 URL：
 * https://{任意子域名.tq-ele.com}/{电表编号}.5010060.a7cf8
 * 例：https://i.tq-ele.com/250110261441.5010060.a7cf8
 * <p>
 * 2) 新版小程序跳转 URL：
 * https://iot.tqdianbiao.com/Miniprogram/device/?code={电表编号}.5010060.42267
 * 例：https://iot.tqdianbiao.com/Miniprogram/device/?code=250814261183.5010060.42267
 * <p>
 * 核心行为：
 * - 识别为拓强电表（品牌 CHTQDQ）；
 * - 提取设备号 device_code（取 "电表编号.其他.其他" 的第一段）；
 * - 返回 QRContent.QRType.GeneraDevice，并写入 brand_code、spu_code。
 */
public class TQ4GMeterQRParser implements IQRCodeParser {

    /**
     * 品牌域名后缀列表。
     * - tq-ele.com：旧域名
     * - tqdianbiao.com：新域名（iot.tqdianbiao.com 等）
     * <p>
     * 判断规则：host 等于后缀，或以 ".后缀" 结尾。
     */
    private static final String[] BRAND_DOMAIN_SUFFIXES = {
            "tq-ele.com",
            "tqdianbiao.com"
    };

    /**
     * SPU编码，目前只和拓强合作 4G 电表，暂时固定。
     */
    private static final String spu_code = "CHTQDQ-4G-DDSY1886";

    /**
     * 品牌代码。
     */
    private static final String brand_code = "CHTQDQ";

    @Override
    public boolean supports(String content) {
        if (StringUtil.isEmpty(content)) return false;
        String text = content.trim();
        if (StringUtil.isEmpty(text)) return false;

        // 必须是 http 或 https 开头，避免和纯文本解析器冲突
        if (!text.startsWith("http://") && !text.startsWith("https://")) return false;

        try {
            URI uri = URI.create(text);

            // 1. 域名校验：必须是拓强相关域名
            String host = uri.getHost();
            if (StringUtil.isEmpty(host)) return false;
            if (!isTQHost(host)) return false;

            // 2. 尝试从 URL 中提取电表编号
            String meterCode = extractMeterCode(uri);
            return !StringUtil.isEmpty(meterCode);
        } catch (Exception e) {
            // URL 解析异常，视为不支持
            return false;
        }
    }

    @Override
    public QRContent parse(String content) {
        try {
            URI uri = URI.create(content.trim());

            String meterCode = extractMeterCode(uri);
            if (StringUtil.isEmpty(meterCode)) {
                QRContent fail = QRContent.fail("拓强电表二维码缺少电表编号");
                fail.content = content;
                return fail;
            }

            QRContent result = QRContent.ok(QRContent.QRType.GeneraDevice);
            result.content = content;
            result.device_code = meterCode;

            if (result.data == null) {
                result.data = new JSONObject();
            }

            // 基础设备信息
            result.data.put("spu_code", spu_code);
            result.data.put("brand_code", brand_code);

            // 可选：留存完整的 code 串，便于排查问题（从 query 或 path 中拿）
            String rawCode = extractRawCode(uri);
            if (!StringUtil.isEmpty(rawCode)) {
                result.data.put("raw_code", rawCode);
            }

            // 可选：记录识别到的是哪种格式（旧 URL / 小程序 URL）
            result.data.put("qr_format", detectFormat(uri));

            return result;
        } catch (Exception e) {
            QRContent fail = QRContent.fail("拓强电表二维码解析异常");
            fail.content = content;
            return fail;
        }
    }

    /**
     * 判断 host 是否为拓强相关域名。
     * 规则：等于后缀，或以 ".后缀" 结尾。
     */
    private boolean isTQHost(String host) {
        String h = host.toLowerCase();
        for (String suffix : BRAND_DOMAIN_SUFFIXES) {
            String s = suffix.toLowerCase();
            if (h.equals(s) || h.endsWith("." + s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 URI 中提取“电表编号”。
     * 优先级：
     * 1) 新版：query 中的 code 参数，如 ?code=250814261183.5010060.42267
     * 2) 旧版：path 最后一段，如 /250110261441.5010060.a7cf8
     * <p>
     * 返回值：
     * - 非空：电表编号（第一个 "." 之前的部分）
     * - 为空：未能提取
     */
    private String extractMeterCode(URI uri) {
        // 1. 先尝试从 query 的 code 参数解析
        String rawCodeFromQuery = extractCodeFromQuery(uri);
        if (!StringUtil.isEmpty(rawCodeFromQuery)) {
            String[] tokens = rawCodeFromQuery.split("\\.");
            if (tokens.length > 0 && !StringUtil.isEmpty(tokens[0])) {
                return tokens[0];
            }
        }

        // 2. 回退到旧版 path 规则
        String rawCodeFromPath = extractCodeFromPath(uri);
        if (!StringUtil.isEmpty(rawCodeFromPath)) {
            String[] tokens = rawCodeFromPath.split("\\.");
            if (tokens.length > 0 && !StringUtil.isEmpty(tokens[0])) {
                return tokens[0];
            }
        }

        return null;
    }

    /**
     * 提取“完整 code 串”，用于记录到 data.raw_code。
     * 优先使用 query 中的 code，其次是 path 最后一段。
     */
    private String extractRawCode(URI uri) {
        String rawCode = extractCodeFromQuery(uri);
        if (!StringUtil.isEmpty(rawCode)) return rawCode;
        return extractCodeFromPath(uri);
    }

    /**
     * 从 query 中提取 code 参数。
     * 例如：?code=250814261183.5010060.42267
     */
    private String extractCodeFromQuery(URI uri) {
        String query = uri.getQuery();
        if (StringUtil.isEmpty(query)) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (StringUtil.isEmpty(pair)) continue;
            int idx = pair.indexOf('=');
            if (idx <= 0 || idx == pair.length() - 1) continue;

            String key = pair.substring(0, idx);
            String value = pair.substring(idx + 1);
            if ("code".equalsIgnoreCase(key) && !StringUtil.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 从 path 最后一段提取“完整 code 串”。
     * 例如：/250110261441.5010060.a7cf8
     * 返回：250110261441.5010060.a7cf8
     */
    private String extractCodeFromPath(URI uri) {
        String path = uri.getPath();
        if (StringUtil.isEmpty(path)) return null;

        String[] pathParts = path.split("/");
        if (pathParts.length == 0) return null;

        String lastSegment = pathParts[pathParts.length - 1];
        if (StringUtil.isEmpty(lastSegment)) return null;

        return lastSegment;
    }

    /**
     * 检测二维码格式类型，主要用于调试和排障。
     * - new_miniprogram：query 中携带 code 参数
     * - legacy_path：老版路径携带设备号
     * - unknown：未命中上述两类
     */
    private String detectFormat(URI uri) {
        if (!StringUtil.isEmpty(extractCodeFromQuery(uri))) {
            return "new_miniprogram";
        }
        if (!StringUtil.isEmpty(extractCodeFromPath(uri))) {
            return "legacy_path";
        }
        return "unknown";
    }
}
