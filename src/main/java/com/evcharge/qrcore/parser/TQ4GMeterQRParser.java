package com.evcharge.qrcore.parser;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.qrcore.parser.base.IQRCodeParser;
import com.evcharge.qrcore.parser.base.QRContent;
import com.xyzs.utils.StringUtil;

import java.net.URI;

/**
 * 拓强 4G 电表二维码解析器。
 * <p>
 * 约定二维码内容格式示例:
 * https://{任意子域名.tq-ele.com}/{电表编号}.5010060.a7cf8
 * 示例:
 * https://i.tq-ele.com/250110261441.5010060.a7cf8
 * <p>
 * 约定含义:
 * 1) 域名以 tq-ele.com 结尾, 作为拓强电表品牌标识。
 * 例如:
 * i.tq-ele.com
 * www.tq-ele.com
 * 2) 路径最后一段为 "电表编号.其他.其他" 格式:
 * 250110261441.5010060.a7cf8
 * 其中:
 * 250110261441 为设备号 deviceCode
 * 后面的 5010060、a7cf8 等字段含义暂不明确, 当前只作为附加信息保留。
 * <p>
 * 设计目标:
 * 1) 上层只关心是否成功解析以及得到的 deviceCode、品牌信息等。
 * 2) 解析失败时返回 fail, 让上层有机会继续走 UrlQRParser 等兜底逻辑。
 * 3) 不对电表编号做过多业务校验, 只要非空即视为有效。
 * <p>
 * 线程安全性:
 * 本解析器实现无成员变量状态, 所有逻辑都基于入参, 因此是线程安全的。
 */
public class TQ4GMeterQRParser implements IQRCodeParser {

    /**
     * 品牌域名后缀。
     * 支持任意子域名, 例如:
     * i.tq-ele.com
     * www.tq-ele.com
     * <p>
     * 判断规则为 host 以该后缀结尾。
     */
    private static final String BRAND_DOMAIN_SUFFIX = "tq-ele.com";

    /**
     * SPU编码，目前只和拓强合作4G电表，暂时固定
     */
    private static final String spu_code = "CHTQDQ-4G-DDSY6607-2P";
    /**
     * 品牌代码
     */
    private static final String brand_code = "CHTQDQ";

    @Override
    public boolean supports(String content) {
        if (StringUtil.isEmpty(content)) return false;
        String text = content.trim();
        if (StringUtil.isEmpty(text)) return false;

        // 必须是 http 或 https 开头的 URL
        // 这样可以避免和纯文本解析器发生冲突
        if (!text.startsWith("http://") && !text.startsWith("https://")) return false;

        try {
            URI uri = URI.create(text);

            // 1. 域名校验: 要求 host 以 tq-ele.com 结尾
            String host = uri.getHost();
            if (host == null || host.isEmpty()) return false;

            // 不区分大小写, 支持任意子域名
            if (!host.toLowerCase().endsWith(BRAND_DOMAIN_SUFFIX)) return false;

            // 2. 路径校验: 只关注最后一段
            //    示例路径: /250110261441.5010060.a7cf8
            String path = uri.getPath();
            if (path == null || path.isEmpty()) return false;

            // 按 "/" 拆分路径, 取最后一段
            String[] pathParts = path.split("/");
            if (pathParts.length == 0) return false;

            String lastSegment = pathParts[pathParts.length - 1];
            if (lastSegment == null || lastSegment.isEmpty()) return false;

            // 3. 最后一段按 "." 拆分, 取第一个部分作为设备号
            //    示例: "250110261441.5010060.a7cf8"
            //    拆分后: ["250110261441", "5010060", "a7cf8"]
            String[] tokens = lastSegment.split("\\.");
            if (tokens.length == 0) return false;

            String meterCode = tokens[0];
            return meterCode != null && !meterCode.isEmpty();
        } catch (Exception e) {
            // URL 解析异常, 直接视为不支持
            return false;
        }
    }

    @Override
    public QRContent parse(String content) {
        try {
            URI uri = URI.create(content.trim());
            String path = uri.getPath();
            String[] pathParts = path.split("/");

            String lastSegment = pathParts[pathParts.length - 1];
            String[] tokens = lastSegment.split("\\.");

            // 按约定, 第一个部分为电表编号
            String meterCode = tokens.length > 0 ? tokens[0] : null;
            if (meterCode == null || meterCode.isEmpty()) {
                QRContent fail = QRContent.fail("拓强电表二维码缺少电表编号");
                fail.content = content;
                return fail;
            }

            // 这里假设 QRType 中存在 DEVICE 枚举
            // 如果你的枚举里有更细分的类型, 可以替换为更具体的类型, 比如 METER_DEVICE 等
            QRContent result = QRContent.ok(QRContent.QRType.GeneraDevice);
            result.content = content;

            // 设备号写入统一字段 device_code
            result.device_code = meterCode;

            // data 中保存更细节的信息, 方便后续业务使用或排查问题
            if (result.data == null) result.data = new JSONObject();

            // 基础信息
            result.data.put("spu_code", spu_code);
            result.data.put("brand_code", brand_code);
            return result;
        } catch (Exception e) {
            // 单个解析器内部发生异常时, 不应该影响整体解析流程
            // 这里返回一个 fail, 让上层 QRCoreParser 有机会继续使用其他解析器处理
            QRContent fail = QRContent.fail("拓强电表二维码解析异常");
            fail.content = content;
            return fail;
        }
    }
}
