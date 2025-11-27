package com.evcharge.libsdk.wechat.corpbot;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业微信机器人类
 * <p>
 * 该类封装了与企业微信机器人进行通信的各种功能，如发送文本消息、Markdown消息、图片消息、文件消息、模板卡片等。
 * <p>
 * 使用者可以通过提供 webhookUrl 来实例化该类，并调用相应的方法发送消息。
 * <p>
 * 文档参考：<a href="https://developer.work.weixin.qq.com/document/path/91770">企业微信机器人 API 文档</a>
 */
public class WechatCorpBot {

    // Webhook URL，用于与企业微信机器人通信
    @Getter
    @Setter
    private String webhookUrl;

    // Log 标签，用于标识日志信息
    private final static String TAG = "企业微信机器人";

    // OkHttpClient 实例，用于发送 HTTP 请求
    private final OkHttpClient okHttpClient = new OkHttpClient();

    // Webhook URL 的正则表达式，用于验证传入的 URL 是否为有效的企业微信机器人 Webhook
    private static final String WEBHOOK_URL_REGEX = "^https://qyapi\\.weixin\\.qq\\.com/cgi-bin/webhook/send\\?key=[\\w-]+$";
    private static final Pattern pattern = Pattern.compile(WEBHOOK_URL_REGEX);

    // 无参构造函数
    public WechatCorpBot() {
    }

    // 带有 Webhook URL 的构造函数
    public WechatCorpBot(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * 发送消息的核心方法
     *
     * @param messageBody 消息体
     * @return SyncResult 发送结果
     */
    public SyncResult send(JSONObject messageBody) {
        return send(messageBody.toJSONString());
    }

    /**
     * 发送消息的核心方法
     *
     * @param messageJSONBody 消息体，JSON格式
     * @return SyncResult 发送结果
     */
    public SyncResult send(String messageJSONBody) {
        // 创建请求体
        RequestBody body = RequestBody.create(messageJSONBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseText = "";
            if (response.body() != null)
                responseText = response.body().string();

            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.error(TAG, "企业微信机器人 - 发送消息失败：无返回任何数据");
                return new SyncResult(1, "");
            }

            // 解析响应体
            JSONObject result = JSONObject.parseObject(responseText);
            int errcode = JsonUtil.getInt(result, "errcode", -1);

            // 检查消息限制，如果达到限制可以做队列推迟发送或者换其他机器人发送
            if (errcode == 45009) {
                return new SyncResult(errcode, "消息限制");
            }

            if (errcode != 0) {
                String errmsg = JsonUtil.getString(result, "errmsg");
                LogsUtil.error(TAG, "企业微信机器人 - 发送消息失败 - %s - 原始消息：%s", responseText, messageJSONBody);
                return new SyncResult(errcode, errmsg);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "企业微信机器人 - 发送消息发生错误 - %s", messageJSONBody);
        }
        return new SyncResult(0, "");
    }

    /**
     * 发送普通文本消息
     *
     * @param content 文本内容，最长不超过2048个字节，必须是UTF-8编码
     * @return SyncResult 发送结果
     */
    public SyncResult sendText(String content) {
        return sendText(content, null, null);
    }

    /**
     * 发送带有 @ 功能的文本消息
     *
     * @param content        文本内容，最长不超过2048个字节，必须是UTF-8编码
     * @param mentioned_list 用户ID列表，提醒群中的指定成员(@某个成员)，@all表示提醒所有人
     * @return SyncResult 发送结果
     */
    public SyncResult sendText(String content, String[] mentioned_list) {
        return sendText(content, mentioned_list, null);
    }

    /**
     * 发送带有 @ 功能的文本消息
     *
     * @param content               文本内容，最长不超过2048个字节，必须是UTF-8编码
     * @param mentioned_list        用户ID列表，提醒群中的指定成员(@某个成员)，@all表示提醒所有人
     * @param mentioned_mobile_list 手机号列表，提醒手机号对应的群成员(@某个成员)，@all表示提醒所有人
     * @return SyncResult 发送结果
     */
    public SyncResult sendText(String content, String[] mentioned_list, String[] mentioned_mobile_list) {
        JSONObject messageBody = WechatCorpBotMessageBuilder.text(content, mentioned_list, mentioned_mobile_list);
        return send(messageBody);
    }

    /**
     * 发送 Markdown 类型消息
     *
     * @param content Markdown内容，最长不超过4096个字节，必须是UTF-8编码
     * @return SyncResult 发送结果
     */
    public SyncResult sendMarkdown(String content) {
        JSONObject messageBody = WechatCorpBotMessageBuilder.markdown(content);
        return send(messageBody);
    }

    /**
     * 发送图片类型消息
     *
     * @param imageBase64 图片的Base64编码
     * @return SyncResult 发送结果
     */
    public SyncResult sendImage(String imageBase64) {
        JSONObject messageBody = WechatCorpBotMessageBuilder.image(imageBase64);
        return send(messageBody);
    }

    /**
     * 发送图文类型消息
     *
     * @param articles 图文消息内容列表
     * @return SyncResult 发送结果
     */
    public SyncResult sendNews(List<ArticleEntity> articles) {
        JSONObject messageBody = WechatCorpBotMessageBuilder.news(articles);
        return send(messageBody);
    }

    /**
     * 发送文件类型消息
     *
     * @param mfile 文件对象
     * @return SyncResult 发送结果
     * @throws Exception 如果文件上传失败
     */
    public SyncResult sendFileMessage(File mfile) throws Exception {
        // 上传文件并获取 mediaId
        String mediaId = uploadFile(mfile, "file");
        if (!StringUtils.hasLength(mediaId)) {
            return new SyncResult(2, "缺少[mediaId]参数，可能上传文件失败");
        }
        JSONObject messageBody = WechatCorpBotMessageBuilder.file(mediaId);
        return send(messageBody);
    }

    /**
     * 发送语音类型消息
     *
     * @param mfile 文件对象
     * @return SyncResult 发送结果
     * @throws Exception 如果文件上传失败
     */
    public SyncResult sendVoiceMessage(File mfile) throws Exception {
        // 上传文件并获取 mediaId
        String mediaId = uploadFile(mfile, "voice");
        if (!StringUtils.hasLength(mediaId)) {
            return new SyncResult(2, "缺少[mediaId]参数，可能上传文件失败");
        }
        JSONObject messageBody = WechatCorpBotMessageBuilder.voice(mediaId);
        return send(messageBody);
    }

    /**
     * 发送文本通知模板卡片到企业微信
     *
     * @param source                  消息来源相关信息
     * @param main_title              主标题对象，包含 title 和 desc
     * @param sub_title_text          二级普通文本
     * @param emphasis_content        突出显示的重要内容
     * @param quote_area              引用文献样式
     * @param horizontal_content_list 二级标题+文本列表
     * @param jump_list               跳转指引样式的列表
     * @param card_action             卡片跳转类型
     * @return SyncResult 发送结果
     */
    public SyncResult sendTextTemplateCard(JSONObject source
            , JSONObject main_title
            , String sub_title_text
            , JSONObject emphasis_content
            , JSONObject quote_area
            , JSONArray horizontal_content_list
            , JSONArray jump_list
            , JSONObject card_action) {
        // 构建消息体
        JSONObject messageBody = WechatCorpBotMessageBuilder.textTemplateCard(source
                , main_title
                , sub_title_text
                , emphasis_content
                , quote_area
                , horizontal_content_list
                , jump_list
                , card_action);
        return send(messageBody);
    }

    /**
     * 发送图文展示模板卡片到企业微信
     *
     * @param source                  消息来源相关信息
     * @param main_title              主标题对象
     * @param card_image              卡片的大图片对象
     * @param image_text_area         图文区域对象
     * @param quote_area              引用区域对象
     * @param vertical_content_list   竖直内容列表
     * @param horizontal_content_list 水平内容列表
     * @param jump_list               跳转列表
     * @param card_action             卡片跳转类型
     * @return SyncResult 发送结果
     */
    public SyncResult sendImageTemplateCard(JSONObject source
            , JSONObject main_title
            , JSONObject card_image
            , JSONObject image_text_area
            , JSONObject quote_area
            , JSONArray vertical_content_list
            , JSONArray horizontal_content_list
            , JSONArray jump_list
            , JSONObject card_action) {
        JSONObject messageBody = WechatCorpBotMessageBuilder.imageTemplateCard(source
                , main_title
                , card_image
                , image_text_area
                , quote_area
                , vertical_content_list
                , horizontal_content_list
                , jump_list
                , card_action);
        return send(messageBody);
    }

    /**
     * 上传文件到企业微信服务器
     *
     * @param inputStream 文件输入流
     * @param type        文件类型，如 "file" 或 "voice"
     * @return 文件的 media_id 或者错误信息
     * @throws Exception 如果文件上传失败
     */
    public String uploadFile(InputStream inputStream, String type) throws Exception {
        // 创建临时文件
        File tempFile = File.createTempFile("upload", null);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        // 调用重载方法上传文件
        String mediaId = uploadFile(tempFile, type);
        // 确保临时文件在上传后被删除
        tempFile.deleteOnExit();
        return mediaId;
    }

    /**
     * 上传文件到企业微信服务器
     *
     * @param file 文件对象
     * @param type 文件类型，如 "file" 或 "voice"
     * @return 文件的 media_id 或者错误信息
     * @throws Exception 如果文件上传失败
     */
    public String uploadFile(File file, String type) throws Exception {
        // 如果 webhookUrl 为空，则返回空字符串
        if (!StringUtils.hasLength(webhookUrl)) return "";

        // 解析 Webhook URL
        URL url = new URL(webhookUrl);
        Map<String, String> params = splitQuery(url);
        if (params.isEmpty()) return "";

        String key = params.get("key");
        String api = String.format("https://qyapi.weixin.qq.com/cgi-bin/webhook/upload_media?key=%s&type=%s", key, type);

        // 构建文件请求体
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media", file.getName(), fileBody)
                .addFormDataPart("filename", file.getName())
                .addFormDataPart("filelength", String.valueOf(file.length()))
                .addFormDataPart("content-type", "application/octet-stream")
                .build();

        Request request = new Request.Builder().url(api).post(requestBody).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = null;
            if (response.body() != null) {
                responseBody = response.body().string();
            }
            JSONObject json = JSONObject.parseObject(responseBody);
            return JsonUtil.getString(json, "media_id");
        }
    }

    /**
     * 解析 URL 中的查询参数
     *
     * @param url 需要解析的 URL
     * @return 包含查询参数的 Map 对象
     */
    public static Map<String, String> splitQuery(URL url) {
        Map<String, String> queryPairs = new HashMap<>();
        String query = url.getQuery();

        if (query == null || query.isEmpty()) {
            return queryPairs;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return queryPairs;
    }

    /**
     * 判断URL是否是企业微信机器人链接
     *
     * @param url 需要验证的URL
     * @return true 如果是有效的Webhook URL，否则为false
     */
    public static boolean isValidWebhookUrl(String url) {
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    //region 实体类

    /**
     * 图文消息的内容实体类
     * <p>
     * 该类用于封装图文消息中的文章信息，包括标题、描述、链接和图片链接等。
     */
    public class ArticleEntity {
        public String title;       // 标题
        public String description; // 描述
        public String url;         // 点击后跳转的链接
        public String picurl;      // 图片链接
    }

    /**
     * 文本通知模板卡片的内容实体类
     * <p>
     * 该类用于封装文本通知模板卡片的内容信息，包括标题、描述、链接和按钮文字等。
     */
    public class TextNoticeTemplateCard {
        public String title;       // 标题
        public String description; // 描述
        public String url;         // 点击后跳转的链接
        public String buttonText;  // 按钮文字
    }

    //endregion
}