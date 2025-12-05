package com.evcharge.qrcore.parser;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * URL 二维码解析器
 * 作用：
 * 1. 识别 http / https 开头的内容；
 * 2. 尝试用 URI 解析出 scheme/host/path/query 等结构；
 * 3. 将 URL 各部分和 query 参数写入 QRContent.data；
 * 4. 约定从 query 中提取 spu_code / device_code，方便上层使用。
 */
public class UrlQRParser implements IQRCodeParser {

    @Override
    public boolean supports(String content) {
        if (content == null) return false;
        String text = content.trim();
        return text.startsWith("http://") || text.startsWith("https://");
    }

    @Override
    public QRContent parse(String content) {
        // 理论上 supports=true 时 content 不会为 null，这里再兜一层
        if (content == null) return QRContent.fail("URL 内容为空");

        String text = content.trim();

        QRContent r = QRContent.ok(QRContent.QRType.URL);
        // 保留原始二维码内容
        r.content = content;

        try {
            URI uri = URI.create(text);

            // 解析 URL 结构
            JSONObject data = new JSONObject();
            data.put("scheme", uri.getScheme());
            data.put("host", uri.getHost());
            if (uri.getPort() != -1) {
                data.put("port", uri.getPort());
            }
            data.put("path", uri.getPath());
            data.put("fragment", uri.getFragment());

            // 解析 query 参数
            Map<String, String> queryParams = parseQuery(uri.getRawQuery());
            JSONObject queryJson = new JSONObject();
            queryJson.putAll(queryParams);
            data.put("query", queryJson);

            r.data = data;
            r.device_code = firstNonEmpty(
                    queryParams.get("device_code"),
                    queryParams.get("deviceCode"),
                    queryParams.get("device"),
                    queryParams.get("dev"),
                    queryParams.get("sn")
            );
        } catch (Exception ignored) {
            // 如果 URI.create 失败，就当成普通 URL 字符串返回：
            // type=URL, raw=原文，不再强制 fail，避免影响兜底 TextQRParser 的使用习惯。
        }
        return r;
    }

    /**
     * 解析 URL 的 query 部分：
     * a=1&b=2&c=3  =>  {a=1, b=2, c=3}
     */
    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;

            int idx = pair.indexOf('=');
            String key;
            String value;
            if (idx > -1) {
                key = pair.substring(0, idx);
                value = pair.substring(idx + 1);
            } else {
                // 没有等号的情况：?flag  =>  flag=""
                key = pair;
                value = "";
            }

            try {
                // URL 解码，防止中文 / 特殊字符
                key = URLDecoder.decode(key, StandardCharsets.UTF_8);
                value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }

            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * 从一组候选值中返回第一个非空非空串的值
     */
    private String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
