package com.evcharge.libsdk.genkigo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

/**
 * 元气充图片SDK
 */
public class GenkigoImageSDK {

    //region 属性
    private String uploadUrl = "";
    private String trustStorePath = "";
    private String trustStorePassword = "";

    public GenkigoImageSDK setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        return this;
    }

    public GenkigoImageSDK setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
        return this;
    }

    public GenkigoImageSDK setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return this;
    }
    //endregion

    public GenkigoImageSDK(String uploadUrl) {
        this.uploadUrl = uploadUrl;
        this.trustStorePath = System.getProperty("user.dir") + ConfigManager.getString("upload.key");
        this.trustStorePassword = "asdf12345";
    }

    /**
     * 上传
     *
     * @param file                 图片文件
     * @param targetServerSavePath 目标服务器参数，保存的路径
     * @return 同步结果
     */
    public SyncResult upload(File file, String targetServerSavePath) {
        return upload(file, "files", targetServerSavePath);
    }

    /**
     * 上传
     *
     * @param file                 图片文件
     * @param filekey              上传的文件参数名
     * @param targetServerSavePath 目标服务器参数，保存的路径
     * @return 同步结果
     */
    public SyncResult upload(File file, String filekey, String targetServerSavePath) {
//        System.setProperty("javax.net.ssl.trustStore", this.trustStorePath);
//        System.setProperty("javax.net.ssl.trustStorePassword", this.trustStorePassword);

        String ApiUrl = String.format("%s?action=uploads&save_path=%s", this.uploadUrl, targetServerSavePath);
        try {
            if (file == null || !file.exists()) {
                return new SyncResult(2, "文件不存在");
            }

            String responseText = Http2Util.uploadFile(ApiUrl, filekey, file);
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn(this.getClass().getSimpleName(), "请求服务器但是无回调任何数据 %s", ApiUrl);
                return new SyncResult(3, "请求服务器但是无回调任何数据");
            }

            JSONObject json = JSONObject.parseObject(responseText);
            int code = JsonUtil.getInt(json, "code", -1);
            String message = JsonUtil.getString(json, "msg");
            if (code != 0) {
                LogsUtil.warn(this.getClass().getSimpleName(), "上传图片发生错误 %s - %s", ApiUrl, responseText);
                return new SyncResult(code, message);
            }

            String url = JsonUtil.getString(json, "data");
            return new SyncResult(code, message, url);
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getSimpleName(), "上传图片到目标服务器发生错误，%s", ApiUrl);
            return new SyncResult(1, String.format("上传图片到目标服务器发生错误 - %s", e.getMessage()));
        }
    }

    /**
     * 上传Base64编码的图片内容
     *
     * @param base64Image          Base64编码的图片内容
     * @param targetServerSavePath 目标服务器保存路径的参数
     * @param imageSuffix          图片后缀
     * @return 同步结果
     */
    public SyncResult uploadBase64Image(String base64Image, String targetServerSavePath, String imageSuffix) {
        String ApiUrl = String.format("%s?action=uploads&save_path=%s", this.uploadUrl, targetServerSavePath);
        try {
            // 解码Base64编码的图片内容
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // 调用uploadFile方法上传文件
            String responseText = Http2Util.uploadFile(ApiUrl
                    , "files"
                    , String.format("%s%s", TimeUtil.getTimestamp(), imageSuffix)
                    , new ByteArrayInputStream(imageBytes));
            if (!StringUtils.hasLength(responseText)) {
                LogsUtil.warn(this.getClass().getSimpleName(), "请求服务器但是无回调任何数据 %s", ApiUrl);
                return new SyncResult(3, "请求服务器但是无回调任何数据");
            }
            // 解析上传结果并返回同步结果
            JSONObject json = JSONObject.parseObject(responseText);
            int code = json.getIntValue("code");
            String message = json.getString("msg");
            String url = json.getString("data");
            return new SyncResult(code, message, url);
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getSimpleName(), "上传Base64编码图片到目标服务器发生错误 %s", ApiUrl);
            return new SyncResult(1, "上传Base64编码图片到目标服务器发生错误");
        }
    }
}
