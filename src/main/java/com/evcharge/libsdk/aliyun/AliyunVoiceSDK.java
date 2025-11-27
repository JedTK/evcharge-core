package com.evcharge.libsdk.aliyun;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dyvmsapi20170525.models.*;
import com.aliyun.sdk.service.dyvmsapi20170525.*;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import darabonba.core.client.ClientOverrideConfiguration;

import java.util.concurrent.CompletableFuture;

/**
 * 阿里云语音通知服务 SDK 类，封装了与阿里云 Dyvms API 进行交互的接口，用于发起语音通知请求。
 * 该类通过阿里云提供的 SDK，调用单次语音呼叫 API（SingleCallByTts），实现向指定的电话号码发送 TTS（文字转语音）呼叫。
 */
public class AliyunVoiceSDK {

    // 阿里云访问密钥ID
    private String accessKeyId;
    // 阿里云访问密钥密钥
    private String accessKeySecret;
    /**
     * 区域ID，用于指定调用的阿里云服务器的区域。
     * 默认值为 "cn-shenzhen"（深圳地区），可根据实际需求修改为其他区域。
     */
    private String region = "cn-shenzhen";
    /**
     * 覆盖默认的阿里云 API 端点地址。
     * 默认值为 "dyvmsapi.aliyuncs.com"，可以根据实际情况设置为不同的API端点。
     */
    private String endpointOverride = "dyvmsapi.aliyuncs.com";
    // 日志标签，用于标识日志中的模块信息
    private final static String TAG = "阿里云语音通知";

    public static AliyunVoiceSDK getInstance() {
        return new AliyunVoiceSDK();
    }

    public AliyunVoiceSDK setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }

    public AliyunVoiceSDK setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    public AliyunVoiceSDK setRegion(String region) {
        this.region = region;
        return this;
    }

    public AliyunVoiceSDK setEndpointOverride(String endpointOverride) {
        this.endpointOverride = endpointOverride;
        return this;
    }

    /**
     * 初始化阿里云凭证提供者，使用静态凭证来访问阿里云API。
     * 通过阿里云的访问密钥 ID 和访问密钥来创建一个 StaticCredentialProvider。
     *
     * @return StaticCredentialProvider 用于认证阿里云服务的凭证提供者
     */
    private StaticCredentialProvider init() {
        return StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(this.accessKeyId) // 设置访问密钥ID
                .accessKeySecret(this.accessKeySecret) // 设置访问密钥Secret
                .build());
    }

    /**
     * 发起单次 TTS 语音呼叫请求。
     * 该方法通过调用阿里云 Dyvms API，将 TTS 模板内容通过电话播放给指定的被叫号码。
     *
     * @param single_phone_number 被叫号码，仅支持单个号码呼叫
     * @param ttsCode             语音模板代码（TTS模板ID），用于指定语音播放的内容
     * @param tranData            模板中的透传参数，用于动态替换模板中的变量
     * @return SyncResult  返回调用结果的对象，包含调用状态码和消息
     * - 0 表示成功
     * - 1 表示请求失败，并返回异常信息
     * - 2 表示请求成功但接口返回错误信息
     */
    public SyncResult singleCall(String single_phone_number, String ttsCode, JSONObject tranData) {
        // 初始化阿里云凭证提供者
        StaticCredentialProvider provider = this.init();

        try (AsyncClient client = AsyncClient.builder()
                .region(this.region) // 设置调用区域
                .credentialsProvider(provider) // 设置认证信息
                .overrideConfiguration(ClientOverrideConfiguration.create().setEndpointOverride(this.endpointOverride)) // 覆盖默认API端点
                .build()) {
            // 构建 TTS 呼叫请求对象
            SingleCallByTtsRequest request = SingleCallByTtsRequest.builder()
                    .calledNumber(single_phone_number) // 设置被叫号码
                    .ttsCode(ttsCode) // 设置 TTS 模板代码
                    .ttsParam(tranData.toJSONString()) // 设置 TTS 模板中的透传参数
                    .build();

            // 发送异步请求并获取响应结果
            CompletableFuture<SingleCallByTtsResponse> response = client.singleCallByTts(request);
            SingleCallByTtsResponse resp = response.get();

            // 将返回结果转换为 JSON 对象
//            JSONObject json = JSONObject.from(resp);
            String jsonString = JSON.toJSONString(resp);

            // 记录响应结果日志
            LogsUtil.info(TAG, jsonString);

            // 关闭客户端连接
            client.close();

            if ("OK".equalsIgnoreCase(JsonUtil.getString(jsonString, "$.body.code"))) {
                return new SyncResult(0, "发送成功");
            }
            String errMsg = JsonUtil.getString(jsonString, "$.body.message");
            LogsUtil.warn(TAG, "[%s] %s", single_phone_number, errMsg);
            return new SyncResult(3, errMsg);
        } catch (Exception e) {
            // 记录错误日志
            LogsUtil.error(e, TAG, "单次语音呼叫请求失败！");
            return new SyncResult(1, e.getMessage()); // 返回失败结果
        }
    }
}