package com.evcharge.libsdk.wechat.corp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.libsdk.wechat.corpbot.WechatCorpBot;
import com.xyzs.utils.common;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * WechatCorpBotMessageBuilder 类用于构建企业微信机器人发送的不同类型的消息。
 * 此类提供了多种方法，用于组装文本消息、Markdown消息、图片消息、图文消息、文件消息、语音消息以及模板卡片消息等。
 */
public class WechatCorpMessageBuilder {

    /**
     * 文本消息
     *
     * @param agentId 企业应用的id，整型。企业内部开发，可在应用的设置页面查看；第三方服务商，可通过接口 获取企业授权信息 获取该参数值
     * @param toUser  指定接收消息的成员，成员ID列表（多个接收者用‘|’分隔，最多支持1000个）。特殊情况：指定为"@all"，则向该企业应用的全部成员发送
     * @param toParty 指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。当touser为"@all"时忽略本参数
     * @param toTag   指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。当touser为"@all"时忽略本参数
     * @param content 消息内容
     * @return 消息结构
     */
    public static JSONObject text(String agentId,
                                  String toUser,
                                  String toParty,
                                  String toTag,
                                  String content) {
        // 构建消息体
        JSONObject body = new JSONObject();
        if (StringUtils.hasLength(toUser)) body.put("touser", toUser);
        if (StringUtils.hasLength(toParty)) body.put("toparty", toParty);
        if (StringUtils.hasLength(toTag)) body.put("totag", toTag);
        
        body.put("msgtype", "text");
        body.put("agentid", agentId);
        body.put("text", new JSONObject() {{
            put("content", content);
        }});
        body.put("safe", 0);
        body.put("enable_id_trans", 0);
        body.put("enable_duplicate_check", 0);
        body.put("duplicate_check_interval", 1800);
        return body;
    }

    /**
     * 组装 Markdown 类型消息。
     *
     * @param content Markdown内容，最长不超过4096个字节，必须是UTF-8编码。
     * @return 返回一个包含Markdown消息内容的JSONObject，用于发送至企业微信。
     */
    public static JSONObject markdown(String content) {
        // 构建消息体
        JSONObject body = new JSONObject();
        body.put("msgtype", "markdown");

        // 构建 Markdown 对象
        JSONObject markdown = new JSONObject();
        markdown.put("content", content);
        body.put("markdown", markdown);
        return body;
    }

    /**
     * 组装图片类型消息。
     *
     * @param imageBase64 图片的Base64编码。
     * @return 返回一个包含图片消息内容的JSONObject，用于发送至企业微信。
     */
    public static JSONObject image(String imageBase64) {
        // 构建消息体
        JSONObject body = new JSONObject();
        body.put("msgtype", "image");

        // 构建图片对象
        JSONObject image = new JSONObject();
        image.put("base64", imageBase64);
        image.put("md5", common.md5(imageBase64));
        body.put("image", image);
        return body;
    }

    /**
     * 组装图文类型消息。
     *
     * @param articles 图文消息内容列表，包含多个ArticleEntity对象。
     * @return 返回一个包含图文消息内容的JSONObject，用于发送至企业微信。
     */
    public static JSONObject news(List<WechatCorpBot.ArticleEntity> articles) {
        // 构建消息体
        JSONObject body = new JSONObject();
        body.put("msgtype", "news");

        // 构建图文消息内容
        JSONArray jsonArticles = new JSONArray();
        for (WechatCorpBot.ArticleEntity article : articles) {
            JSONObject jsonArticle = new JSONObject();
            jsonArticle.put("title", article.title);
            jsonArticle.put("description", article.description);
            jsonArticle.put("url", article.url);
            jsonArticle.put("picurl", article.picurl);
            jsonArticles.add(jsonArticle);
        }

        JSONObject news = new JSONObject();
        news.put("articles", jsonArticles);
        body.put("news", news);
        return body;
    }

    /**
     * 组装文件类型消息。
     *
     * @param mediaId 文件在企业微信服务器上的媒体ID。
     * @return 返回一个包含文件消息内容的JSONObject，用于发送至企业微信。
     * @throws Exception 如果文件ID无效或消息构建失败，抛出异常。
     */
    public static JSONObject file(String mediaId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("msgtype", "file");
        body.put("file", new JSONObject() {{
            put("media_id", mediaId);
        }});
        return body;
    }

    /**
     * 组装语音类型消息。
     *
     * @param mediaId 语音在企业微信服务器上的媒体ID。
     * @return 返回一个包含语音消息内容的JSONObject，用于发送至企业微信。
     * @throws Exception 如果语音ID无效或消息构建失败，抛出异常。
     */
    public static JSONObject voice(String mediaId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("msgtype", "voice");
        body.put("voice", new JSONObject() {{
            put("media_id", mediaId);
        }});
        return body;
    }

    /**
     * 发送文本通知模板卡片到企业微信。
     *
     * @param source                  消息来源相关信息。
     * @param main_title              主标题对象，包含 title 和 desc。
     * @param sub_title_text          二级普通文本。
     * @param emphasis_content        突出显示的重要内容。
     * @param quote_area              引用文献样式。
     * @param horizontal_content_list 二级标题+文本列表。
     * @param jump_list               跳转指引样式的列表。
     * @param card_action             卡片跳转类型。
     * @return 返回一个包含模板卡片消息内容的JSONObject，用于发送至企业微信。
     */
    public static JSONObject textTemplateCard(JSONObject source
            , JSONObject main_title
            , String sub_title_text
            , JSONObject emphasis_content
            , JSONObject quote_area
            , JSONArray horizontal_content_list
            , JSONArray jump_list
            , JSONObject card_action) {
        // 构建消息体
        JSONObject body = new JSONObject();
        body.put("msgtype", "template_card");

        // 构建模板卡片对象
        JSONObject template_card = new JSONObject();
        template_card.put("card_type", "text_notice");

        // 添加 source 字段
        if (source != null) template_card.put("source", source);

        // 添加主标题
        if (main_title != null) template_card.put("main_title", main_title);

        // 添加突出显示的内容
        if (emphasis_content != null) template_card.put("emphasis_content", emphasis_content);

        // 添加二级普通文本
        if (StringUtils.hasLength(sub_title_text)) template_card.put("sub_title_text", sub_title_text);

        // 添加引用区域
        if (quote_area != null) template_card.put("quote_area", quote_area);

        // 添加水平内容列表
        if (horizontal_content_list != null) template_card.put("horizontal_content_list", horizontal_content_list);

        // 添加跳转列表
        if (jump_list != null) template_card.put("jump_list", jump_list);

        // 添加卡片跳转类型
        if (card_action != null) template_card.put("card_action", card_action);

        body.put("template_card", template_card);
        return body;
    }

    /**
     * 发送图文展示模板卡片到企业微信。
     *
     * @param source                  消息来源相关信息。
     * @param main_title              主标题对象，包含 title 和 desc。
     * @param card_image              卡片的大图片对象。
     * @param image_text_area         图文区域对象，包含图片和文本。
     * @param quote_area              引用区域对象。
     * @param vertical_content_list   竖直内容列表，包含多个内容对象。
     * @param horizontal_content_list 水平内容列表，包含多个内容对象。
     * @param jump_list               跳转列表，包含多个跳转指引对象。
     * @param card_action             卡片跳转类型。
     * @return 返回一个包含图文展示模板卡片消息内容的JSONObject，用于发送至企业微信。
     */
    public static JSONObject imageTemplateCard(JSONObject source
            , JSONObject main_title
            , JSONObject card_image
            , JSONObject image_text_area
            , JSONObject quote_area
            , JSONArray vertical_content_list
            , JSONArray horizontal_content_list
            , JSONArray jump_list
            , JSONObject card_action) {
        // 构建消息体
        JSONObject body = new JSONObject();
        body.put("msgtype", "template_card");

        // 构建模板卡片对象
        JSONObject template_card = new JSONObject();
        template_card.put("card_type", "news_notice");

        // 添加 source 字段
        if (source != null) template_card.put("source", source);

        // 添加主标题
        if (main_title != null) template_card.put("main_title", main_title);

        // 添加卡片图片
        if (card_image != null) template_card.put("card_image", card_image);

        // 添加图文区域
        if (image_text_area != null) template_card.put("image_text_area", image_text_area);

        // 添加引用区域
        if (quote_area != null) template_card.put("quote_area", quote_area);

        // 添加竖直内容列表
        if (vertical_content_list != null) template_card.put("vertical_content_list", vertical_content_list);

        // 添加水平内容列表
        if (horizontal_content_list != null) template_card.put("horizontal_content_list", horizontal_content_list);

        // 添加跳转列表
        if (jump_list != null) template_card.put("jump_list", jump_list);

        // 添加卡片跳转类型，默认为跳转URL
        if (card_action == null) {
            card_action = new JSONObject();
            card_action.put("type", 1); // 1代表跳转URL
            card_action.put("url", "");
        }
        template_card.put("card_action", card_action);

        body.put("template_card", template_card);
        return body;
    }
}