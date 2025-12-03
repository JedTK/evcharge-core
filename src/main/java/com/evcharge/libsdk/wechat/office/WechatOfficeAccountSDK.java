package com.evcharge.libsdk.wechat.office;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.HttpClientForWechat;
import com.xyzs.entity.DataService;
import com.xyzs.utils.*;
import org.springframework.web.client.RestTemplate;

/**
 * 微信公众号相关接口封装 SDK。
 * <p>
 * 主要职责：
 * 1. 统一获取并缓存公众号 access_token（全局唯一、有限时）。
 * 2. 统一获取并缓存 jsapi_ticket（用于前端 JS-SDK 调用）。
 * 3. 对外提供 JsApi 签名生成能力（前端调用 wx.config 时所需参数）。
 * <p>
 * 线程安全说明：
 * - 单例模式 + synchronized 方法，确保 access_token / jsapi_ticket 在高并发下只会被初始化一次。
 * - 缓存层由 DataService.getMainCache() 提供，假设其内部已做好线程安全。
 * <p>
 * 使用方式（示例）：
 * - 运营后台页面需要调用微信 JS-SDK（如分享、扫码等），前端请求后端接口。
 * - 后端接口调用 generateJsSignature(url) 生成 appId、timestamp、nonceStr、signature 等参数。
 * - 前端拿到这些参数后，调用 wx.config 进行初始化。
 */
public class WechatOfficeAccountSDK {

    /**
     * 公众号的 appId（从配置文件加载）。
     */
    public String appId;

    /**
     * 公众号的 appSecret（从配置文件加载）。
     */
    public String appSecret;

    /**
     * 日志前缀，用于区分模块。
     */
    private final static String TAG = "公众号SDK(2.0)";

    /**
     * access_token 的缓存键。
     * 注意：access_token 为公众号级别全局唯一，多个接口共用同一个值。
     */
    private static final String CACHE_KEY_ACCESS_TOKEN = "Wechat:WxOfficeAccessToken";

    /**
     * jsapi_ticket 的缓存键。
     * jsapi_ticket 用于 JS-SDK 签名，和 access_token 一样有有效期限制。
     */
    private static final String CACHE_KEY_JSAPI_TICKET = "Wechat:OfficeAccount:JsApiTicket";

    /**
     * 懒汉式单例实例，使用 volatile 保证可见性和禁止指令重排。
     */
    private volatile static WechatOfficeAccountSDK instance;

    /**
     * 获取全局单例实例。
     * <p>
     * 采用双重检查锁定（DCL）的方式实现线程安全的懒加载单例。
     */
    public static WechatOfficeAccountSDK getInstance() {
        if (instance == null) {
            synchronized (WechatOfficeAccountSDK.class) {
                if (instance == null) {
                    instance = new WechatOfficeAccountSDK();
                }
            }
        }
        return instance;
    }

    /**
     * 构造函数，从配置中心加载 appId 与 appSecret。
     * 一般在应用启动后，第一次使用单例时才会真正实例化。
     */
    public WechatOfficeAccountSDK() {
        this.appId = ConfigManager.getString("wechat.officeAccountAppId");
        this.appSecret = ConfigManager.getString("wechat.officeAccountAppSecret");
    }

    /**
     * 从微信服务器获取 access_token（带缓存）。
     * <p>
     * 整体流程：
     * 1. 先从缓存读取 access_token，如果存在则直接返回。
     * 2. 若缓存中不存在，则检查配置是否齐全（appId / appSecret）。
     * 3. 调用微信官方接口，获取 access_token。
     * 4. 对返回结果进行错误码和数据完整性校验。
     * 5. 将 access_token 写入缓存，设置过期时间（略小于微信返回的 expires_in）。
     * 6. 返回最新的 access_token。
     * <p>
     * 注意：
     * - 该方法使用 synchronized 关键字，避免在并发情况下重复向微信服务器请求。
     * - 只要缓存层可用，绝大部分请求都会命中缓存，不会频繁访问微信接口。
     *
     * @return 公众号 access_token，若失败返回空字符串。
     */
    public synchronized String getAccessToken() {
        try {
            // 1. 优先从缓存中读取 access_token（避免频繁调用微信接口）
            String accessToken = DataService.getMainCache().getString(CACHE_KEY_ACCESS_TOKEN);
            if (StringUtil.isNotEmpty(accessToken)) {
                return accessToken;
            }

            // 2. 检查配置是否正确
            if (StringUtil.isEmpty(appId) || StringUtil.isEmpty(appSecret)) {
                LogsUtil.warn(TAG, "请求获取AccessToken失败，appId/appSecret未配置");
                return "";
            }

            // 3. 构造微信官方接口地址
            String url = String.format(
                    "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    appId, appSecret
            );

            // 4. 发起 HTTP GET 请求
            HttpClientForWechat client = new HttpClientForWechat(new RestTemplate());
            String text = client.sendGet(url);

            // 5. 判空校验
            if (StringUtil.isEmpty(text)) {
                LogsUtil.warn(TAG, "请求获取AccessToken发生错误，响应结果为空 - %s", url);
                return "";
            }

            // 6. 解析 JSON 结果
            JSONObject json = JSONObject.parseObject(text);
            if (json == null) {
                LogsUtil.warn(TAG, "请求获取AccessToken发生错误，响应结果无法解析为JSON - %s - %s", url, text);
                return "";
            }

            // 7. 判断是否返回错误码（微信失败时会返回 errcode 和 errmsg）
            if (json.containsKey("errcode")) {
                int errCode = JsonUtil.getInt(json, "errcode", -1);
                String errMsg = json.getString("errmsg");
                LogsUtil.warn(TAG, "请求获取AccessToken发生错误 - errcode=%s errmsg=%s - %s - %s", errCode, errMsg, url, text);
                return "";
            }

            // 8. 从返回结果中读取 access_token 和有效期
            accessToken = json.getString("access_token");
            long expiresIn = JsonUtil.getLong(json, "expires_in", 7200);

            if (StringUtil.isEmpty(accessToken) || expiresIn <= 0) {
                LogsUtil.warn(TAG, "请求获取AccessToken返回内容异常 - %s - %s", url, text);
                return "";
            }

            // 9. 计算缓存时间：比微信官方有效期少 5 分钟，防止“临界点”失效
            //    同时设置一个下限（例如 120 秒），避免计算出现负数等问题。
            long ttlMs = Math.max((expiresIn - 300) * 1000L, 120_000L);
            DataService.getMainCache().set(CACHE_KEY_ACCESS_TOKEN, accessToken, ttlMs);

            return accessToken;
        } catch (Exception e) {
            // 捕获所有异常，避免抛出到调用层导致页面错误
            LogsUtil.error(e, TAG, "通过API读取公众号AccessToken失败");
        }
        return "";
    }

    /**
     * 从微信服务器获取 jsapi_ticket（带缓存）。
     * <p>
     * jsapi_ticket 用途：
     * - 用于调用微信 JS-SDK 接口时的签名计算。
     * <p>
     * 整体流程：
     * 1. 先从缓存读取 jsapi_ticket，如果存在则直接返回。
     * 2. 若缓存中不存在，则先通过 getAccessToken() 获取 access_token。
     * 3. 使用 access_token 调用微信的 ticket/getticket 接口。
     * 4. 对返回结果进行错误码和数据完整性校验。
     * 5. 将 jsapi_ticket 写入缓存，设置过期时间（略小于微信返回的 expires_in）。
     * 6. 返回最新的 jsapi_ticket。
     *
     * @return jsapi_ticket 字符串，若失败返回空字符串。
     */
    public synchronized String getJsApiTicket() {
        try {
            // 1. 优先从缓存里读取 jsapi_ticket
            String ticket = DataService.getMainCache().getString(CACHE_KEY_JSAPI_TICKET);
            if (StringUtil.isNotEmpty(ticket)) {
                return ticket;
            }

            // 2. 如果缓存中没有，则需要依赖 access_token
            String accessToken = getAccessToken();
            if (StringUtil.isEmpty(accessToken)) {
                LogsUtil.warn(TAG, "获取 jsapi_ticket 失败，access_token 为空");
                return "";
            }

            // 3. 构造微信官方接口地址
            String url = String.format("https://api.weixin.qq.com/cgi-bin/ticket/getticket?type=jsapi&access_token=%s", accessToken);

            // 4. 发起 HTTP GET 请求
            HttpClientForWechat client = new HttpClientForWechat(new RestTemplate());
            String text = client.sendGet(url);

            // 5. 判空校验
            if (StringUtil.isEmpty(text)) {
                LogsUtil.warn(TAG, "请求获取 jsapi_ticket 发生错误，响应结果为空 - %s", url);
                return "";
            }

            // 6. 解析 JSON 结果
            JSONObject json = JSONObject.parseObject(text);
            if (json == null) {
                LogsUtil.warn(TAG, "请求获取 jsapi_ticket 发生错误，响应结果无法解析为JSON - %s - %s", url, text);
                return "";
            }

            // 7. 检查 errcode，微信接口约定 errcode=0 为成功
            int errcode = JsonUtil.getInt(json, "errcode", -1);
            if (errcode != 0) {
                String errmsg = json.getString("errmsg");
                LogsUtil.warn(TAG, "请求获取 jsapi_ticket 发生错误 - errcode=%s errmsg=%s - %s - %s", errcode, errmsg, url, text);
                return "";
            }

            // 8. 读取 ticket 和有效期
            ticket = json.getString("ticket");
            long expiresIn = JsonUtil.getLong(json, "expires_in", 300);

            if (StringUtil.isEmpty(ticket) || expiresIn <= 0) {
                LogsUtil.warn(TAG, "请求获取 jsapi_ticket 返回内容异常 - %s - %s", url, text);
                return "";
            }

            // 9. 设置缓存时间：提前一点过期，防止临界时间内签名失败
            long ttlMs = Math.max((expiresIn - 100) * 1000L, 120_000L);
            DataService.getMainCache().set(CACHE_KEY_JSAPI_TICKET, ticket, ttlMs);

            return ticket;
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "[获取微信JsApiTicket] 获取失败");
        }
        return "";
    }

    /**
     * 使用当前 HTTP 请求的完整 URL 生成 JsApi 签名。
     * <p>
     * 场景：
     * - 在 Controller 或 Filter 中调用时，通常可以直接通过 HttpRequestUtil.getUrl()
     * 获取当前请求的完整地址，然后生成对应的签名包返回给前端。
     * <p>
     * 注意：
     * - URL 必须与前端页面实际访问的地址完全一致（包含协议、域名、端口、路径以及查询参数），
     * 否则微信会校验失败。
     *
     * @return JsApi 签名相关参数的 JSON 对象，失败时返回 null。
     */
    public JSONObject generateJsSignature() {
        return generateJsSignature(HttpRequestUtil.getClientUrl());
    }

    /**
     * 根据指定 URL 生成 JsApi 签名。
     * <p>
     * 生成逻辑：
     * 1. 获取 jsapi_ticket。
     * 2. 生成随机字符串 nonceStr。
     * 3. 生成时间戳 timestamp（秒级）。
     * 4. 按照微信官方要求拼接待签名字符串：
     * jsapi_ticket=xxx&noncestr=xxx&timestamp=xxx&url=xxx
     * 5. 对该字符串做 SHA1 运算，得到 signature。
     * 6. 打包 appId、timestamp、nonceStr、signature 等字段返回给调用方。
     * <p>
     * 参数说明：
     * - url：需要与前端页面实际访问的地址保持一致。
     * 若传入空字符串，会尝试从 HttpRequestUtil.getUrl() 自动获取当前请求 URL。
     *
     * @param url 需要进行签名的完整 URL（包含协议、域名、路径和查询参数）。
     * @return 返回一个 JSON 对象，包含前端调用 wx.config 所需的全部参数，失败时返回 null。
     */
    public JSONObject generateJsSignature(String url) {
        try {
            // 当调用方未显式传入 URL 时，尝试使用当前请求的 URL
            if (StringUtil.isEmpty(url)) url = HttpRequestUtil.getUrl();

            // 1. 获取 jsapi_ticket
            String jsApiTicket = this.getJsApiTicket();
            if (StringUtil.isEmpty(jsApiTicket)) {
                LogsUtil.warn(TAG, "创建JsApi签名失败，jsapi_ticket为空，url=%s", url);
                return null;
            }

            // 2. 生成签名所需的随机串和时间戳（时间戳单位：秒）
            String nonceStr = common.randomStr(10);
            long timestamp = TimeUtil.getTimestamp() / 1000;

            // 3. 按照微信官方要求拼接待签名字符串
            // 注意：参数名全部小写，且必须按指定顺序连接。
            String rawString = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%s&url=%s"
                    , jsApiTicket
                    , nonceStr
                    , timestamp
                    , url
            );

            // 4. 使用 SHA1 对字符串进行签名
            String signature = SHAUtils.sha1(rawString);

//            LogsUtil.info(TAG, "请求JsApi签名处理 - 签名字符串：%s 签名后：%s", rawString, signature);

            // 5. 组装返回给前端的签名包
            JSONObject signPackage = new JSONObject();
            // 当前使用的 jsapi_ticket，便于排查问题时比对
            signPackage.put("jsapiTicket", jsApiTicket);
            // 微信公众号 appId
            signPackage.put("appId", this.appId);
            // 随机串
            signPackage.put("nonceStr", nonceStr);
            // 时间戳（秒级）
            signPackage.put("timestamp", timestamp);
            // 用于签名的 URL（应与前端实际访问一致）
            signPackage.put("url", url);
            // 签名结果
            signPackage.put("signature", signature);
            // 签名前的原始拼接字符串，便于调试
            signPackage.put("rawString", rawString);

//            LogsUtil.info(TAG, "请求JsApi签名：%s", signPackage.toJSONString());

            return signPackage;
        } catch (Exception e) {
            // 在日志里记录 URL 与错误信息，便于排查线上问题
            LogsUtil.error(e, TAG, "创建JsApi签名失败，url=%s msg=%s", url, e.getMessage());
        }
        return null;
    }
}
