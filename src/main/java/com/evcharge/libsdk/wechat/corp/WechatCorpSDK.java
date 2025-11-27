package com.evcharge.libsdk.wechat.corp;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBot;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Http2Util;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import okhttp3.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 企业微信SDK (垃圾)
 * 开发文档：<a href="https://developer.work.weixin.qq.com/document/path/90600">...</a>
 */
@Deprecated
public class WechatCorpSDK {
    /**
     * 企业ID
     */
    private String corpId;

    /**
     * 应用的凭证密钥，注意应用需要是启用状态
     */
    private String corpSecret;

    private final static String TAG = "企业微信SDK";
    private final static String BASE_URL = "https://qyapi.weixin.qq.com";
    // OkHttpClient 实例，用于发送 HTTP 请求
    private final OkHttpClient okHttpClient = new OkHttpClient();

    public WechatCorpSDK setCorpId(String corpId) {
        this.corpId = corpId;
        return this;
    }

    public WechatCorpSDK setCorpSecret(String corpSecret) {
        this.corpSecret = corpSecret;
        return this;
    }

    public WechatCorpSDK() {
    }

    public WechatCorpSDK(String corpId, String corpSecret) {
        this.corpId = corpId;
        this.corpSecret = corpSecret;
    }

    /**
     * 获取授权码
     * 文档链接：<a href="https://developer.work.weixin.qq.com/document/path/91039">...</a>
     *
     * @return 授权码
     */
    public String getAccessToken() {
        try {
            String access_token = DataService.getMainCache().getString(String.format("WechatCorpSDK:%s:%s:AccessToken", this.corpId, this.corpSecret));
            if (StringUtils.hasLength(access_token)) return access_token;

            String requestUrl = String.format("%s/cgi-bin/gettoken?corpid=%s&corpsecret=%s&debug=1", BASE_URL, this.corpId, this.corpSecret);
            String response = Http2Util.get(requestUrl,"application/x-www-form-urlencoded");
            //返回结果：{"errcode":0,"errmsg":"ok","access_token":"accesstoken000001","expires_in":7200}
            if (!StringUtils.hasLength(response)) {
                LogsUtil.warn(TAG, "获取授权码无任何结果返回 - %s", requestUrl);
                return "";
            }
            int errcode = JsonUtil.getInt(response, "errcode", -1);
            String errmsg = JsonUtil.getString(response, "errmsg", "");
            if (errcode != 0) {
                LogsUtil.warn(TAG, "获取授权码发生错误 - %s - %s", requestUrl, errmsg);
                return "";
            }
            access_token = JsonUtil.getString(response, "access_token", "");
            int expires_in = JsonUtil.getInt(response, "expires_in", 0);
            if (expires_in > 0) {
                //转化成毫秒级
                expires_in = (expires_in - 10) * 1000;
                DataService.getMainCache().set(String.format("WechatCorpSDK:%s:%s:AccessToken", this.corpId, this.corpSecret), access_token, expires_in);
            }
            return access_token;
        } catch (Exception e) {
            LogsUtil.error(TAG, "获取授权码发生错误");
        }
        return "";
    }

    public SyncResult getUserList(String cursor) {
        // 获取授权码
        String accessToken = getAccessToken();
        // 目标请求地址
        String requestUrl = String.format("%s/cgi-bin/user/list_id?access_token=%s", BASE_URL, accessToken);
        // 组建参数
        JSONObject params = new JSONObject();
        if (StringUtils.hasLength(cursor)) params.put("cursor", cursor);
        params.put("limit", 10000);

        // 创建请求体
        RequestBody body = RequestBody.create(params.toJSONString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(requestUrl)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseText = "";
            if (response.body() != null) responseText = response.body().string();
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.error(TAG, " 发送消息失败：无返回任何数据");
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
                LogsUtil.error(TAG, "发送消息失败 - %s - 原始消息：%s", responseText, params);
                return new SyncResult(errcode, errmsg);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "发送消息发生错误 - %s", params);
        }
        return new SyncResult(0, "");
    }

    /**
     * 发送消息的核心方法
     * 文档链接：<a href="https://developer.work.weixin.qq.com/document/path/90236">...</a>
     *
     * @param messageBody 消息体
     * @return SyncResult 发送结果
     */
    public SyncResult send(JSONObject messageBody) {
        // 获取授权码
        String accessToken = getAccessToken();
        // 目标请求地址
        String requestUrl = String.format("%s/cgi-bin/message/send?access_token=%s", BASE_URL, accessToken);
        // 将消息体转换为字符串
        String params = messageBody.toJSONString();
        // 创建请求体
        RequestBody body = RequestBody.create(params, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(requestUrl)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseText = "";
            if (response.body() != null) responseText = response.body().string();
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.error(TAG, " 发送消息失败：无返回任何数据");
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
                LogsUtil.error(TAG, "发送消息失败 - %s - 原始消息：%s", responseText, params);
                return new SyncResult(errcode, errmsg);
            }
        } catch (Exception e) {
            LogsUtil.error(e, TAG, "发送消息发生错误 - %s", params);
        }
        return new SyncResult(0, "");
    }

    /**
     * 文本消息
     *
     * @param agentId 企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
     * @param toUser  指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。特殊情况：指定为"@all"，则向该企业应用的全部成员发送
     * @param toParty 指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。当touser为"@all"时忽略本参数
     * @param toTag   指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。当touser为"@all"时忽略本参数
     * @param content 消息内容
     * @return 发送结果
     */
    public SyncResult sendText(String agentId,
                               String toUser,
                               String toParty,
                               String toTag,
                               String content) {
        JSONObject messageBody = WechatCorpMessageBuilder.text(agentId, toUser, toParty, toTag, content);
        return send(messageBody);
    }

    /**
     * 发送 Markdown 类型消息
     *
     * @param content Markdown内容，最长不超过4096个字节，必须是UTF-8编码
     * @return SyncResult 发送结果
     */
    public SyncResult sendMarkdown(String content) {
        JSONObject messageBody = WechatCorpMessageBuilder.markdown(content);
        return send(messageBody);
    }

    /**
     * 发送图片类型消息
     *
     * @param imageBase64 图片的Base64编码
     * @return SyncResult 发送结果
     */
    public SyncResult sendImage(String imageBase64) {
        JSONObject messageBody = WechatCorpMessageBuilder.image(imageBase64);
        return send(messageBody);
    }

    /**
     * 发送图文类型消息
     *
     * @param articles 图文消息内容列表
     * @return SyncResult 发送结果
     */
    public SyncResult sendNews(List<WechatCorpBot.ArticleEntity> articles) {
        JSONObject messageBody = WechatCorpMessageBuilder.news(articles);
        return send(messageBody);
    }

    /**
     * 发送文件类型消息
     *
     * @param file 文件对象
     * @return SyncResult 发送结果
     * @throws Exception 如果文件上传失败
     */
    public SyncResult sendFileMessage(File file) throws Exception {
        // 上传文件并获取 mediaId
        String mediaId = uploadFile(file, "file");
        if (!StringUtils.hasLength(mediaId)) {
            return new SyncResult(2, "缺少[mediaId]参数，可能上传文件失败");
        }
        JSONObject messageBody = WechatCorpMessageBuilder.file(mediaId);
        return send(messageBody);
    }

    /**
     * 发送语音类型消息
     *
     * @param file 文件对象
     * @return SyncResult 发送结果
     * @throws Exception 如果文件上传失败
     */
    public SyncResult sendVoiceMessage(File file) throws Exception {
        // 上传文件并获取 mediaId
        String mediaId = uploadFile(file, "voice");
        if (!StringUtils.hasLength(mediaId)) {
            return new SyncResult(2, "缺少[mediaId]参数，可能上传文件失败");
        }
        JSONObject messageBody = WechatCorpMessageBuilder.voice(mediaId);
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
        JSONObject messageBody = WechatCorpMessageBuilder.textTemplateCard(source
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
        JSONObject messageBody = WechatCorpMessageBuilder.imageTemplateCard(source
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
        // 获取授权码
        String accessToken = getAccessToken();
        // 目标请求地址
        String requestUrl = String.format("%s/cgi-bin/media/upload?access_token=%s&type=%s", BASE_URL, accessToken, type);
        // 构建文件请求体
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media", file.getName(), fileBody)
                .addFormDataPart("filename", file.getName())
                .addFormDataPart("filelength", String.valueOf(file.length()))
                .addFormDataPart("content-type", "application/octet-stream")
                .build();
        Request request = new Request.Builder().url(requestUrl).post(requestBody).build();
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
}
